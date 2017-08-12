import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class Start {

    //<editor-fold desc="Parameter-Strings">
    private static final String COMMAND = "command";
    private static final String WORKDIR = "workdir";
    private static final String WORLD_PATH = "worldpath";
    private static final String USE_RAMDISK = "useramdisk";
    private static final String RAMDISK_BACKUP_INTERVAL = "ramdiskbackupinterval"; // In minutes
    private static final String MAX_COPY_TIME = "maxcopytime"; // In seconds
    private static final String RESTART_INTERVAL = "restartinterval"; // In minutes
    private static final String MAX_SHUTDOWN_TIME = "maxshutdowntime"; // In seconds
    //</editor-fold>

    private static final String RAMDISK_WORLD_PATH = "/dev/shm/world";
    private static final Map<String, String> parameters = new HashMap<>();
    private static File workdirFile;
    private static Timer timer;
    private static Process process;
    private static OutputStream output;
    private static boolean restart = true; // while this is true minecraft is restarted

    static {
        // Default values:
        parameters.put(WORLD_PATH, "world"); // the relativ path from workdir is used
        parameters.put(RAMDISK_BACKUP_INTERVAL, "60");
        parameters.put(MAX_COPY_TIME, "30");
        parameters.put(MAX_SHUTDOWN_TIME, "30");
    }

    public static void main(String args[]) {

        // load all parameters, environment overwrites default, args overwrites environment
        getEnvironment();
        parseArgs(args);
        if (!testParameters()) {
            System.out.println("Minimum parameters not filled");
            Runtime.getRuntime().halt(-1);
        }
        workdirFile = new File(parameters.get(WORKDIR));

        initRamdisk();
        initRestart();
        initShutdownHook();
        initInputHandleThread();

        runMinecraft();
    }

    private static void runMinecraft() {
        int errorCounter = 0;

        while (restart && errorCounter < 10) { // if there are to many errors the container should stop
            try {
                ProcessBuilder pb = new ProcessBuilder(parameters.get(COMMAND).split(" ")) // split the command because every parameter has to be an own string
                        .directory(workdirFile) // set the startup directory (external of the container)
                        .inheritIO() // share the console with this process
                        .redirectInput(ProcessBuilder.Redirect.PIPE); // return the input stream back to this program so the stop command can be send

                process = pb.start(); // init the process
                output = process.getOutputStream(); // get the output-stream to write commands to the process
                process.waitFor(); // wait for server-stop
                output = null;

                if (restart) {
                    backupRamdisk(); // because minecraft is stopped a backup is now usefull
                }

            } catch (Exception ex) {
                System.out.println("Execution Failed: " + ex.getMessage());
                errorCounter++;
            }
        }
    }

    //<editor-fold desc="Init">
    private static void parseArgs(String args[]) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i] != null && args[i].startsWith("-")) {
                parameters.put(args[i].substring(1).toLowerCase(), args[i + 1] == null || args[i + 1].startsWith("-") ? null : args[i + 1]);
                i++;
            }
        }
    }

    private static void getEnvironment() {
        for (Map.Entry<String, String> ent : System.getenv().entrySet()) {
            parameters.put(ent.getKey().toLowerCase(), ent.getValue());
        }
    }

    private static boolean testParameters() {
        return parameters.containsKey(COMMAND) && parameters.get(COMMAND) != null && !parameters.get(COMMAND).isEmpty() &&
                parameters.containsKey(WORKDIR) && parameters.get(WORKDIR) != null && !parameters.get(WORKDIR).isEmpty() &&

                // test if parameters are numbers
                isNuberic(parameters.get(RAMDISK_BACKUP_INTERVAL)) &&
                isNuberic(parameters.get(MAX_COPY_TIME)) &&
                isNuberic(parameters.get(MAX_SHUTDOWN_TIME)) &&
                (!parameters.containsKey(RESTART_INTERVAL) || isNuberic(parameters.get(RESTART_INTERVAL))) // this parameter is not needed
                ;
    }
    //</editor-fold>

    //<editor-fold desc="Stop">
    private static void stopCommand() throws IOException {
        if (output != null) {
            output.write(new byte[]{(byte) 0x73, (byte) 0x74, (byte) 0x6f, (byte) 0x70, (byte) 0x0d, (byte) 0x0a}); // The byte-array represents "stop" and a return char
            output.flush(); // write the buffer to the process
        }
    }

    private static void stop(boolean haltThisIfFailed) {
        try {
            stopCommand();
            process.waitFor(Long.parseLong(parameters.get(MAX_SHUTDOWN_TIME)), TimeUnit.SECONDS);
        } catch (Exception ex) {
            System.out.println("Minecraft Shutdown Failed. (Maybe max shutdown time is to short)");

            if (haltThisIfFailed) {
                System.out.println("To stop minecraft the docker-container is shutting down.");
                Runtime.getRuntime().exit(-1);
            }
        }
    }
    //</editor-fold>

    //<editor-fold desc="Ram Disk">
    private static void initRamdisk() {
        if (parameters.containsKey(USE_RAMDISK)) {
            try {
                copy(parameters.get(WORLD_PATH), RAMDISK_WORLD_PATH, workdirFile);
            } catch (Exception ex) {
                System.out.println("Ramdisk-Creation: Copy Failed: " + ex.getMessage());
                Runtime.getRuntime().halt(-1);
            }

            if (Long.parseLong(parameters.get(RAMDISK_BACKUP_INTERVAL)) > 0) { // if the backup is not manually switched off
                if (timer == null) {
                    timer = new Timer();
                }

                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        backupRamdisk();
                    }
                }, Long.parseLong(parameters.get(RAMDISK_BACKUP_INTERVAL)) * 60 * 1000); // delay in milliseconds: RAMDISK_BACKUP_INTERVAL * 60 (seconds per minute) * 1000 (milliseconds per second)
            }
        }
    }

    private static void backupRamdisk() {
        if (parameters.containsKey(USE_RAMDISK)) {
            try {
                copy(RAMDISK_WORLD_PATH, parameters.get(WORLD_PATH), workdirFile);
            } catch (Exception ex) {
                System.out.println("Ramdisk-Backup: Copy Failed: " + ex.getMessage());
            }
        }
    }

    private static void finilizeRamdisk() {
        if (parameters.containsKey(USE_RAMDISK)) {
            try {
                copy(RAMDISK_WORLD_PATH, parameters.get(WORLD_PATH), workdirFile);
            } catch (Exception ex) {
                System.out.println("Ramdisk-Finalize: Copy Failed (DATA IS LOST): " + ex.getMessage());
            }
        }
    }

    private static synchronized void copy(String from, String to, File workdir) throws IOException, InterruptedException { // synchronized to avoid doing two copies at one time
        new ProcessBuilder("mkdir", "-p", to) // create the path
                .directory(workdir)
                .start()
                .waitFor(1, TimeUnit.SECONDS); // should not last longer then one second

        new ProcessBuilder("cp", "-r", "-u", from + "/.", to) // copy the files
                .directory(workdir)
                .start()
                .waitFor(Long.parseLong(parameters.get(MAX_COPY_TIME)), TimeUnit.MINUTES); // should not last longer then the given minutes
    }
    //</editor-fold>

    //<editor-fold desc="Restart">
    private static void initRestart() {
        if (parameters.containsKey(RESTART_INTERVAL)) {
            if (timer == null) {
                timer = new Timer();
            }

            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    stop(true);
                }
            }, Long.parseLong(parameters.get(RESTART_INTERVAL)) * 60 * 1000); // delay in milliseconds: RAMDISK_BACKUP_INTERVAL * 60 (seconds per minute) * 1000 (milliseconds per second)
        }
    }
    //</editor-fold>

    //<editor-fold desc="Shutdown hook">
    private static void initShutdownHook() {
        // get the signal to stop the process
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            restart = false; // stop the endless loop for restarting the server

            stop(false); // stop minecraft

            finilizeRamdisk(); // to copy the data back from the Ramdisk

            Runtime.getRuntime().halt(0); // return 0 so the process end is success
        }));
    }
    //</editor-fold>

    //<editor-fold desc="Input-handle thread">
    private static void initInputHandleThread() {
        new Thread(() -> {
            BufferedReader buf = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                try {
                    String in = buf.readLine() + "\n"; // get the input and add the enter char

                    if (output != null) {
                        output.write(in.getBytes());
                        output.flush(); // write the buffer to the process
                    }
                } catch (Exception ex) {
                    System.out.println("Exception: " + ex.getMessage());
                }
            }
        }).start();
    }
    //</editor-fold>

    //<editor-fold desc="Util">
    private static boolean isNuberic(String str) {
        if (str == null) {
            return false;
        }
        int length = str.length();
        if (length == 0) {
            return false;
        }

        for (int i = 0; i < length; i++) {
            char c = str.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }
    //</editor-fold>
}