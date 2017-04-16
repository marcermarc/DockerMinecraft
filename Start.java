import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public class Start {
    private static Process process;
    private static OutputStream output;

    public static void main(String args[]) {
        if (args.length != 2) {
            System.out.println("Two Arguments needed, first command, second path");
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    output.write("stop".getBytes());
                } catch (IOException ex) {
                    System.out.println("Stop Failed: " + ex.getMessage());
                }
            }
        });

        try {
            process = Runtime.getRuntime().exec(args[0], null, new File(args[1]));
            output = process.getOutputStream();
        } catch (Exception ex) {
            System.out.println("Start Failed: " + ex.getMessage());
        }

        try {
            process.waitFor();
        } catch (InterruptedException ex) {
            System.out.println("Wait Failed: " + ex.getMessage());
        }
    }
}
