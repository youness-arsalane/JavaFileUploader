import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class FileUploaderServer {
    static final int PORT = 5656;

    public static void main(String[] args) {
        ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(PORT);
        } catch (IOException e) {
            System.err.println("Kon geen server starten op poort: " + PORT);
            System.exit(-1);
        }

        System.out.println("--- Server draait en luister naar poort " + PORT + " ---");

        Socket clientSocket;

        while (true) {
            try {
                clientSocket = serverSocket.accept();
                System.out.println("Nieuwe client verbonden: " + clientSocket.getInetAddress().getHostAddress());

                ClientHandler clientHandler = new ClientHandler(clientSocket);
                Thread thread = new Thread(clientHandler);
                thread.start();

            } catch (IOException e) {
                System.err.println("Fout bij verbinding met client: " + e.getMessage());
                System.exit(-1);
            }
        }
    }

    public static class ClientHandler implements Runnable {
        private final Socket socket;
        private final ObjectOutputStream objectOutputStream;
        private final ObjectInputStream objectInputStream;

        ClientHandler (Socket socket) throws IOException {
            this.socket = socket;
            this.objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            this.objectInputStream = new ObjectInputStream(socket.getInputStream());
        }

        @Override
        public void run() {
            String filePath = System.getProperty("user.dir") + "\\uploaded-files\\";

            File folderFile = new File(filePath);
            if (!folderFile.exists() && !folderFile.mkdir()) {
                System.err.println("Kon geen map aanmaken voor de bestanden.");
                System.exit(-1);
            }

            String filename = null;
            FileOutputStream fileOutputStream = null;

            try {
                filename = (String) objectInputStream.readObject();

                File file = new File(filePath + filename);
                if (file.exists() && file.isFile()) {
                    objectOutputStream.writeObject("FileExists");
                    System.out.println("Kon bestand '" + filename + "' niet uploaden omdat deze al bestaat.");
                    Thread.sleep(100);
                    throw new IOException();
                }

                objectOutputStream.writeObject("FileNotExists");

                long filesize = (long) objectInputStream.readObject();
                System.out.println("Nieuw bestand wordt geüpload: '" + filename + "' (Bestandsgrootte: " + filesize + " bytes).");

                byte[] buffer = new byte[4096];
                int bytesRead;
                long bytesReceived = 0;

                fileOutputStream = new FileOutputStream(filePath + filename);
                while (bytesReceived < filesize && (bytesRead = objectInputStream.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, bytesRead);
                    bytesReceived += bytesRead;
                }

                fileOutputStream.close();

                System.out.println("Bestand '" + filename + "' succesvol ontvangen!");

            } catch (IOException | ClassNotFoundException | InterruptedException e) {
                System.out.println(e.getMessage());
                System.err.println("Verbinding verbroken met client: " + this.socket.getInetAddress().getHostAddress());
                if (filename != null && fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }

                    File file = new File(filePath + filename);
                    if (file.exists()) {
                        if (!file.delete()) {
                            System.err.println("Corrupt bestand kon niet worden verwijderd.");
                        }
                    }
                }
            }
        }
    }
}
