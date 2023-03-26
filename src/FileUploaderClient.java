import java.io.*;
import java.net.Socket;

public class FileUploaderClient {
    static final int PORT = 5656;

    public static void main(String[] args) {
        MultiThreadedClient thread = new MultiThreadedClient();
        thread.run();
    }

    public static void progressBar(int percentage) {
        int length = 26;
        int filledLength = (int) (percentage / 100.0 * length);
        int remainingLength = length - filledLength;

        String sb = '\r' +
                String.format("%3d%% [", percentage) +
                "=".repeat(Math.max(0, filledLength)) +
                " ".repeat(Math.max(0, remainingLength)) +
                ']';

        System.out.print(sb);
    }

    public static class MultiThreadedClient implements Runnable {
        private Socket socket;
//        private PrintWriter out;
//        private BufferedReader in;
        private ObjectOutputStream objectOutputStream;
        private ObjectInputStream objectInputStream;

        public static void main(String[] args) {
            Thread clientThread = new Thread(new MultiThreadedClient());
            clientThread.start();
        }

        public void run() {
            try {
                socket = new Socket("localhost", PORT);
                objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                objectInputStream = new ObjectInputStream(socket.getInputStream());

                System.out.println("Vul hier het bestandspad in: ");
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
                String filePath = bufferedReader.readLine();

                File file = new File(filePath);
                if (!file.exists() || !file.isFile()) {
                    throw new Exception("Bestand kon niet gevonden worden.");
                }

                objectOutputStream.writeObject(file.getName());

                String fileValidationOutput = (String)objectInputStream.readObject();
                if (fileValidationOutput.equals("FileExists")) {
                    throw new Exception("Het bestand bestaat al op de server.");
                }

                objectOutputStream.writeObject(file.length());

                byte[] buffer = new byte[4096];
                float filesize = file.length();
                float chunks = filesize / (float) buffer.length;
                if (chunks < 1.0F) {
                    chunks = 1.0F;
                }

                int bytesRead;
                int chunkIndex = 0;

                FileInputStream fileInputStream = new FileInputStream(file);
                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    objectOutputStream.write(buffer, 0, bytesRead);
                    chunkIndex++;

                    float percentage = chunkIndex / chunks * 100;
                    progressBar((int) percentage);
                }

                System.out.println('\n');

                objectOutputStream.close();
                objectInputStream.close();
                fileInputStream.close();
                bufferedReader.close();

                System.out.println("Bestand succesvol verzonden!");
            } catch (IOException e) {
                System.err.println("Kon geen verbinding maken met " + socket.getInetAddress().getHostName() + ":" + PORT);
                System.exit(-1);
            } catch (Exception e) {
                System.err.println(e.getMessage());
                System.exit(-1);
            }
        }
    }
}
