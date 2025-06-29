package client;

import java.io.*;
import java.net.Socket;

public class Client {

    /* ------------- połączenie ------------- */
    private static Socket connect(String host, int port) throws IOException {
        return new Socket(host, port);
    }

    /* ------------- wysyłanie -------------- */
    private static void send(String pngPath, Socket socket) throws IOException {
        File file = new File(pngPath);

        try (FileInputStream  in  = new FileInputStream(file);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

            /* 4-bajtowa długość – tak czyta serwer */
            out.writeInt((int) file.length());

            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1)
                out.write(buf, 0, n);

            out.flush();
            System.out.println("File sent (" + file.length() + " B).");
        }
    }

    /* ------------- odbiór ----------------- */
    private static void receive(Socket socket, String outPath) throws IOException {
        try (DataInputStream in  = new DataInputStream(socket.getInputStream());
             FileOutputStream out = new FileOutputStream(outPath)) {

            int fileSize   = in.readInt();          // 4 bajty długości
            byte[] buffer  = new byte[8192];
            int received   = 0;

            while (received < fileSize) {
                int n = in.read(buffer, 0,
                        Math.min(buffer.length, fileSize - received));
                if (n == -1) throw new EOFException("Stream closed too early");
                out.write(buffer, 0, n);
                received += n;
            }
            System.out.println("File received (" + received + " B).");
        }
    }

    /* ------------- main -------------------- */
    public static void main(String[] args) {
        try (Socket socket = connect("localhost", 5000)) {
            send("input.png", socket);          // podaj ścieżkę do pliku źródłowego
            receive(socket, "output.png");      // wynik z serwera
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
