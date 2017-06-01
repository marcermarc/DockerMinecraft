import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class Start {
    private static Process process;
    private static OutputStream output;
    private static boolean restart = true;

    public static void main(String args[]) {
        if (args.length != 2) {
            System.out.println("Two Arguments needed, first command, second path");
            return;
        }

        // get the signal to stop the process
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                restart = false; // stop the endless loop for restarting the server

                if (output != null) {
                    output.write(new byte[]{(byte) 0x73, (byte) 0x74, (byte) 0x6f, (byte) 0x70, (byte) 0x0d, (byte) 0x0a}); // The byte-array represents "stop" and a return char
                    output.flush(); // write the buffer to the process
                }

                process.waitFor(); // wait for the server-stop
                Runtime.getRuntime().halt(0); // return 0 so the process end is success
            } catch (Exception ex) {
                System.out.println("Stop Failed: " + ex.getMessage());
            }
        }));

        // this thread handle the input and hands the input off to the process
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

        while (restart) {
            try {
                String command = args[0]
                        .replaceAll("&HEAP&", System.getenv().get("HEAP")) // replace the HEAP
                        .replaceAll("&PARAMS&", System.getenv().get("PARAMS")); //replace the PARAMS

                ProcessBuilder pb = new ProcessBuilder(command.split(" ")) // split the command because every parameter has to be an own string
                        .directory(new File(args[1])) // set the startup directory (external of the container)
                        .inheritIO() // share the console with this process
                        .redirectInput(ProcessBuilder.Redirect.PIPE); // return the input stream back to this program so the stop command can be send

                process = pb.start(); // init the process
                output = process.getOutputStream(); // get the output-stream to write commands to the process
                process.waitFor(); // wait for server-stop
                output = null;
            } catch (Exception ex) {
                System.out.println("Execution Failed: " + ex.getMessage());
            }
        }
    }
}