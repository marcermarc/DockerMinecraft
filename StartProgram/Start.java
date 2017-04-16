import java.io.File;
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

        Runtime.getRuntime().addShutdownHook(new Thread() { // get the signal to stop the process
            @Override
            public void run() {
                try {
					restart = false; // stop the endless loop for restarting the server
                    output.write(new byte[] {(byte)0x73,(byte)0x74,(byte)0x6f,(byte)0x70,(byte)0x0d,(byte)0x0a}); // The byte-array represents "stop" and a return char
					output.flush(); // write the buffer to the process
					process.waitFor(); // wait for the server-stop
					Runtime.getRuntime().halt(0); // retrun 0 so the process end is succsessfull
                } catch (Exception ex) {
                    System.out.println("Stop Failed: " + ex.getMessage());
                }
            }
        });

		while (restart) {
			try {
				process = Runtime.getRuntime().exec(args[0], null, new File(args[1])); // init the process
				output = process.getOutputStream(); // get the output-stream to write commands to the process
				process.waitFor(); // wait for server-stop
			} catch (Exception ex) {
				System.out.println("Execution Failed: " + ex.getMessage());
			}
		}
    }
}