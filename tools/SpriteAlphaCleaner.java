import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import javax.imageio.ImageIO;

/** Removes the generated light neutral background from 4x2 pet sprite sheets. */
public final class SpriteAlphaCleaner {
    private static final int COLUMNS = 4;
    private static final int ROWS = 2;

    public static void main(String[] args) throws Exception {
        if (args.length == 0) throw new IllegalArgumentException("Pass one or more PNG paths");
        for (String value : args) clean(new File(value));
    }

    private static void clean(File file) throws Exception {
        BufferedImage source = ImageIO.read(file);
        BufferedImage image = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        image.getGraphics().drawImage(source, 0, 0, null);
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);
        boolean[] visited = new boolean[pixels.length];
        int[] queue = new int[pixels.length];
        int head = 0;
        int tail = 0;

        int cellWidth = width / COLUMNS;
        int cellHeight = height / ROWS;
        for (int row = 0; row < ROWS; row++) {
            int top = row * cellHeight;
            int bottom = (row + 1) * cellHeight - 1;
            for (int column = 0; column < COLUMNS; column++) {
                int left = column * cellWidth;
                int right = (column + 1) * cellWidth - 1;
                for (int x = left; x <= right; x++) {
                    tail = offer(x, top, width, height, pixels, visited, queue, tail);
                    tail = offer(x, bottom, width, height, pixels, visited, queue, tail);
                }
                for (int y = top; y <= bottom; y++) {
                    tail = offer(left, y, width, height, pixels, visited, queue, tail);
                    tail = offer(right, y, width, height, pixels, visited, queue, tail);
                }
            }
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

        removeCrossFrameBleed(pixels, width, height);

        image.setRGB(0, 0, width, height, pixels, 0, width);
        ImageIO.write(image, "png", file);
        long transparent = Arrays.stream(pixels).filter(pixel -> (pixel >>> 24) == 0).count();
        System.out.printf("%s: transparent=%d/%d%n", file.getName(), transparent, pixels.length);
    }

    private static int offer(
        int x,
        int y,
        int width,
        int height,
        int[] pixels,
        boolean[] visited,
        int[] queue,
        int tail
    ) {
        if (x < 0 || y < 0 || x >= width || y >= height) return tail;
        int index = y * width + x;
        if (visited[index] || !isBackground(pixels[index])) return tail;
        visited[index] = true;
        queue[tail] = index;
        return tail + 1;
    }

    private static boolean isBackground(int color) {
        int red = color >>> 16 & 0xFF;
        int green = color >>> 8 & 0xFF;
        int blue = color & 0xFF;
        int maximum = Math.max(red, Math.max(green, blue));
        int minimum = Math.min(red, Math.min(green, blue));
        // The generator's checkerboard uses slightly warm neutral values around
        // #E4E4E4. Keep the chroma tolerance deliberately narrow so the blue-white
        // fur cannot become connected to the background flood.
        return (red + green + blue) / 3 >= 220 && maximum - minimum <= 4;
    }

    /**
     * Image generators occasionally let a paw or ear from the neighbouring frame
     * cross a 4x2 cell boundary. Such fragments are disconnected from the subject
     * in the current cell, touch that cell's outer edge, and never reach its centre.
     * Remove only those components; detached motion marks inside a frame are kept.
     */
    private static void removeCrossFrameBleed(int[] pixels, int width, int height) {
        int cellWidth = width / COLUMNS;
        int cellHeight = height / ROWS;
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                removeCrossFrameBleed(pixels, width, column * cellWidth, row * cellHeight, cellWidth, cellHeight);
            }
        }
    }

    private static void removeCrossFrameBleed(int[] pixels, int width, int left, int top, int cellWidth, int cellHeight) {
        boolean[] visited = new boolean[cellWidth * cellHeight];
        int[] queue = new int[cellWidth * cellHeight];
        int[] component = new int[cellWidth * cellHeight];
        int centreLeft = left + cellWidth / 4;
        int centreRight = left + cellWidth * 3 / 4;
        int centreTop = top + cellHeight / 8;
        int centreBottom = top + cellHeight * 7 / 8;

        for (int localY = 0; localY < cellHeight; localY++) {
            for (int localX = 0; localX < cellWidth; localX++) {
                int localIndex = localY * cellWidth + localX;
                int globalIndex = (top + localY) * width + left + localX;
                if (visited[localIndex] || (pixels[globalIndex] >>> 24) <= 8) continue;

                int head = 0;
                int tail = 0;
                int count = 0;
                boolean touchesEdge = false;
                boolean reachesCentre = false;
                visited[localIndex] = true;
                queue[tail++] = localIndex;
                while (head < tail) {
                    int current = queue[head++];
                    component[count++] = current;
                    int x = current % cellWidth;
                    int y = current / cellWidth;
                    int absoluteX = left + x;
                    int absoluteY = top + y;
                    touchesEdge |= x == 0 || x == cellWidth - 1 || y == 0 || y == cellHeight - 1;
                    reachesCentre |= absoluteX >= centreLeft && absoluteX <= centreRight
                        && absoluteY >= centreTop && absoluteY <= centreBottom;
                    if (x > 0) tail = offerOpaque(x - 1, y, left, top, cellWidth, width, pixels, visited, queue, tail);
                    if (x + 1 < cellWidth) tail = offerOpaque(x + 1, y, left, top, cellWidth, width, pixels, visited, queue, tail);
                    if (y > 0) tail = offerOpaque(x, y - 1, left, top, cellWidth, width, pixels, visited, queue, tail);
                    if (y + 1 < cellHeight) tail = offerOpaque(x, y + 1, left, top, cellWidth, width, pixels, visited, queue, tail);
                }

                if (touchesEdge && !reachesCentre) {
                    for (int index = 0; index < count; index++) {
                        int current = component[index];
                        int x = current % cellWidth;
                        int y = current / cellWidth;
                        pixels[(top + y) * width + left + x] &= 0x00FFFFFF;
                    }
                }
            }
        }
    }

    private static int offerOpaque(
        int x,
        int y,
        int left,
        int top,
        int cellWidth,
        int width,
        int[] pixels,
        boolean[] visited,
        int[] queue,
        int tail
    ) {
        int localIndex = y * cellWidth + x;
        if (visited[localIndex]) return tail;
        int globalIndex = (top + y) * width + left + x;
        if ((pixels[globalIndex] >>> 24) <= 8) return tail;
        visited[localIndex] = true;
        queue[tail] = localIndex;
        return tail + 1;
    }
}
