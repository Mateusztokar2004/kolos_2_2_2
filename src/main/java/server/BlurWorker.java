package server;

import java.awt.image.BufferedImage;

class BlurWorker implements Runnable {
    private final BufferedImage src, dst;
    private final int size, r, y0, y1;

    BlurWorker(BufferedImage src, BufferedImage dst, int size,
               int y0, int y1) {
        this.src = src;
        this.dst = dst;
        this.size = size;
        this.r = size / 2;   // promień = size/2
        this.y0 = y0;        // pierwszy wiersz dla tego wątku
        this.y1 = y1;        // wiersz końcowy (exclusive)
    }

    @Override public void run() {
        int w = src.getWidth(), h = src.getHeight();
        for (int y = y0; y < y1; y++)
            for (int x = 0; x < w; x++)
                dst.setRGB(x, y, blurPixel(x, y, w, h));
    }

    private int blurPixel(int x, int y, int w, int h) {
        long a = 0, rAcc = 0, g = 0, b = 0;
        for (int dy = -r; dy <= r; dy++) {
            int yy = clamp(y + dy, 0, h - 1);
            for (int dx = -r; dx <= r; dx++) {
                int xx = clamp(x + dx, 0, w - 1);
                int rgb = src.getRGB(xx, yy);
                a    += (rgb >>> 24) & 0xFF;
                rAcc += (rgb >>> 16) & 0xFF;
                g    += (rgb >>> 8)  & 0xFF;
                b    +=  rgb         & 0xFF;
            }
        }
        int px = size * size;
        return ((int)(a / px) << 24) | ((int)(rAcc / px) << 16)
                | ((int)(g / px) << 8) |  (int)(b / px);
    }

    private int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
