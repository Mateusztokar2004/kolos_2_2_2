package server;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;

public class Server {

    /* -------- GUI -------- */
    private static volatile int kernelSize = 3;   // 1‒15, tylko nieparzyste

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Server::createGui);
        server();
    }

    /* -------- gniazdo + obsługa klienta -------- */
    private static void server() {
        try (ServerSocket serverSocket = new ServerSocket(5000)) {
            System.out.println("Serwer nasłuchuje na 5000...");

            while (true) {
                try (Socket client = serverSocket.accept();
                     DataInputStream  in  = new DataInputStream(client.getInputStream());
                     DataOutputStream out = new DataOutputStream(client.getOutputStream())) {

                    /* --- PUNKT 1: odbiór PNG --- */
                    int length = in.readInt();
                    byte[] raw = in.readNBytes(length);
                    System.out.println("PUNKT 1: odebrano " + raw.length + " B");

                    /* --- PUNKT 2: zapis oryginału --- */
                    Path dir = Paths.get("images");
                    Files.createDirectories(dir);
                    String stamp = LocalDateTime.now()
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
                    Path original = dir.resolve(stamp + ".png");
                    Files.write(original, raw);
                    System.out.println("PUNKT 2: zapisano " + original);

                    /* --- PUNKT 4: box-blur --- */
                    BufferedImage src = ImageIO.read(original.toFile());
                    long t0 = System.currentTimeMillis();
                    BufferedImage dst = boxBlur(src, kernelSize);
                    long delay = System.currentTimeMillis() - t0;

                    Path processed = dir.resolve(stamp + "_blur.png");
                    ImageIO.write(dst, "png", processed.toFile());
                    System.out.printf("PUNKT 4: rozmyto w %d ms (kernel=%d)%n",
                            delay, kernelSize);

                    /* --- PUNKT 6: odesłanie pliku klientowi --- */
                    byte[] ans = Files.readAllBytes(processed);
                    out.writeInt(ans.length);   // 4 bajty długości
                    out.write(ans);             // sam obraz
                    out.flush();
                    System.out.println("PUNKT 6: odesłano " + ans.length + " B");

                    /*  punkt 5 (baza) dopiszemy później  */

                } catch (IOException | InterruptedException ex) {
                    ex.printStackTrace();   // problem dotyczył tylko tego klienta
                }
            }

        } catch (IOException ex) {
            System.out.println("Problem z serwerem:");
            ex.printStackTrace();
        }
    }

    /* -------- równoległy box-blur -------- */
    private static BufferedImage boxBlur(BufferedImage src, int size)
            throws InterruptedException {

        int cores = Runtime.getRuntime().availableProcessors();
        BufferedImage dst = new BufferedImage(
                src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);

        ExecutorService pool = Executors.newFixedThreadPool(cores);
        int slice = (int) Math.ceil(src.getHeight() / (double) cores);

        for (int i = 0; i < cores; i++) {
            int y0 = i * slice;
            int y1 = Math.min(src.getHeight(), y0 + slice);
            pool.submit(new BlurWorker(src, dst, size, y0, y1));
        }
        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.HOURS);
        return dst;
    }

    /* -------- GUI: suwak promienia -------- */
    private static void createGui() {
        JFrame f = new JFrame("Promień filtra");
        JSlider slider = new JSlider(1, 15, kernelSize);
        slider.setMajorTickSpacing(2);
        slider.setPaintTicks(true);
        JLabel label = new JLabel("Promień: " + kernelSize, SwingConstants.CENTER);

        slider.addChangeListener(e -> {
            int v = slider.getValue() | 1;   // wymuszamy nieparzystość
            kernelSize = v;
            label.setText("Promień: " + v);
            if (slider.getValue() != v) slider.setValue(v);
        });

        f.setLayout(new BorderLayout(5, 5));
        f.add(slider, BorderLayout.CENTER);
        f.add(label,  BorderLayout.SOUTH);
        f.pack();
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setVisible(true);
    }
}
