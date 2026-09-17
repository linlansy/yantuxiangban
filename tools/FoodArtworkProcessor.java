import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Converts generated square food art into a compact 512px transparent Android asset. */
public final class FoodArtworkProcessor {
    private static final int OUTPUT_SIZE = 512;

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Usage: input.png output.png");
        BufferedImage source = ImageIO.read(new File(args[0]));
        BufferedImage transparent = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D copy = transparent.createGraphics();
        copy.setComposite(AlphaComposite.Src);
        copy.drawImage(source, 0, 0, null);
        copy.dispose();

        if (!hasTransparency(transparent)) removeGeneratedCheckerboard(transparent);

        BufferedImage output = new BufferedImage(OUTPUT_SIZE, OUTPUT_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = output.createGraphics();
        graphics.setComposite(AlphaComposite.Src);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(transparent, 0, 0, OUTPUT_SIZE, OUTPUT_SIZE, null);
        graphics.dispose();
        ImageIO.write(output, "png", new File(args[1]));
    }

    private static boolean hasTransparency(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y += 8) {
            for (int x = 0; x < image.getWidth(); x += 8) {
                if ((image.getRGB(x, y) >>> 24) < 255) return true;
            }
        }
        return false;
    }

    private static void removeGeneratedCheckerboard(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);
        boolean[] visited = new boolean[pixels.length];
        int[] queue = new int[pixels.length];
        int head = 0;
        int tail = 0;
        for (int x = 0; x < width; x++) {
            tail = offer(x, 0, width, height, pixels, visited, queue, tail);
            tail = offer(x, height - 1, width, height, pixels, visited, queue, tail);
        }
        for (int y = 0; y < height; y++) {
            tail = offer(0, y, width, height, pixels, visited, queue, tail);
            tail = offer(width - 1, y, width, height, pixels, visited, queue, tail);
        }
        while (head < tail) {
            int index = queue[head++];
            int x = index % width;
            int y = index / width;
            pixels[index] &= 0x00FFFFFF;
            tail = offer(x - 1, y, width, height, pixels, visited, queue, tail);
            tail = offer(x + 1, y, width, height, pixels, visited, queue, tail);
            tail = offer(x, y - 1, width, height, pixels, visited, queue, tail);
            tail = offer(x, y + 1, width, height, pixels, visited, queue, tail);
        }
        image.setRGB(0, 0, width, height, pixels, 0, width);
    }

    private static int offer(int x, int y, int width, int height, int[] pixels, boolean[] visited, int[] queue, int tail) {
        if (x < 0 || y < 0 || x >= width || y >= height) return tail;
        int index = y * width + x;
        if (visited[index] || !isCheckerboard(pixels[index])) return tail;
        visited[index] = true;
        queue[tail] = index;
        return tail + 1;
    }

    private static boolean isCheckerboard(int color) {
        int red = color >>> 16 & 0xFF;
        int green = color >>> 8 & 0xFF;
        int blue = color & 0xFF;
        int maximum = Math.max(red, Math.max(green, blue));
        int minimum = Math.min(red, Math.min(green, blue));
        return (red + green + blue) / 3 >= 232 && maximum - minimum <= 7;
    }
}
