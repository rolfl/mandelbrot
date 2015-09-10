package net.tuis.mandelbrot;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Utility class to compute Mandelbrot sets within a given window
 * @author rolf
 */
public class Mandelbrot {

    private static final double MINSTEP = Double.MIN_NORMAL * 4;

    /**
     * A description of the center of focus for the window, and the zoom level.
     */
    public static final class Window {
        private final double centerX, centerY, zoom;

        /**
         * Create a window centered at the given logical location and zoom
         * level.
         * 
         * @param centerX
         *            The X location
         * @param centerY
         *            The Y location
         * @param zoom
         *            The zoom degree (1.0 to 1.79e308 or so)
         */
        public Window(double centerX, double centerY, double zoom) {
            if (zoom <= 0.0) {
                throw new IllegalArgumentException("Illegal zoom " + zoom);
            }
            this.centerX = centerX;
            this.centerY = centerY;
            this.zoom = zoom;
        }

        /**
         * Get the focus X coordinate
         * @return the X coordinate of the focal point.
         */
        public double getCenterX() {
            return centerX;
        }

        /**
         * Get the focus Y coordinate
         * @return the Y coordinate of the focal point.
         */
        public double getCenterY() {
            return centerY;
        }

        /**
         * Get the zoom level
         * @return the zoom level.
         */
        public double getZoom() {
            return zoom;
        }

    }

    /**
     * Compute a matrix of iterations representing a window in to the Mandelbrot set.
     * 
     * @param pixWidth The width of the matrix to compute
     * @param pixHeight The height of the matrix to compute
     * @param limit The limit at which computations assume the coordinate is included in the set.
     * @param window The definition of the location and zoom degree in to the set.
     * @return A matrix containing the computational iterations
     */
    public static final int[][] mandelbrot(final int pixWidth, final int pixHeight,
            final int limit, final Window window) {

        final double mandWidth = 3.5 / window.getZoom();
        final double xStep = mandWidth / pixWidth;

        if (xStep < MINSTEP) {
            return overZoom(pixWidth, pixHeight, limit);
        }

        final double left = window.getCenterX() - mandWidth / 2.0;
        final double bottom = window.getCenterY() - (pixHeight / 2) * xStep;

        final double[] scaleX = IntStream.range(0, pixWidth)
                .mapToDouble(x -> left + x * xStep).toArray();
        final double[] scaleY = IntStream.range(0, pixHeight)
                .mapToDouble(y -> bottom + y * xStep).toArray();

        return IntStream
                .range(0, pixHeight)
                .parallel()
                .mapToObj(
                        y -> IntStream.range(0, pixWidth)
                                .map(x -> countIterations(limit, scaleX[x], scaleY[y]))
                                .toArray()).toArray(s -> new int[s][]);

    }

    private static int countIterations(final int limit, final double x0, final double y0) {
        double x = 0.0;
        double y = 0.0;
        int iterations = 0;
        while (x * x + y * y < 4.0 && iterations < limit) {
            double xt = x * x - y * y + x0;
            y = 2 * x * y + y0;
            x = xt;
            iterations++;
        }
        return iterations;
    }

    /**
     * Create a buffered image mapping the iterations of the mandelbrot to the color palette
     * @param mand The matrics to map.
     * @param color The color to map for the matrix.
     * @return A BufferedImage containing the mapped mandelbrot.
     */
    public static BufferedImage mapMandelbrot(int[][] mand, final int[] color) {
        final int width = mand[0].length;
        final int height = mand.length;
        final BufferedImage image = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_ARGB);
        int[] pixels = IntStream.range(0, width * height)
                .map(p -> color[mand[p / width][p % width] % color.length]).toArray();

        image.setRGB(0, 0, width, height, pixels, 0, width);
        return image;
    }

    /**
     * Produce an increasingly bright sequence of colors that spiral out through the colour wheel.
     * 
     * @param maxIndex the number of colors to produce (inclusive)
     * @return an array of aRGB values that represent the unique colors.
     */
    public static final int[] buildColors(final int maxIndex) {
        final float root = (float) Math.sqrt(maxIndex);
        return IntStream
                .rangeClosed(0, maxIndex)
                .map(c -> maxIndex - c)
                .mapToObj(
                        c -> Color
                                .getHSBColor((c % root) / root, 1.0f, (c / root) / root))
                .mapToInt(c -> c.getRGB()).toArray();
    }

    /**
     * For a given width, return a height that matches the ratio of a Mandelbrot
     * image. A complete Mandelbrot is contained within -2.5 to 1.0 and -1.0 to
     * 1.0 which gives a useful ratio of height to width of 3.5/2.0.
     * <p>
     * The returned height will always be odd which allows the center of the
     * image to be on an exact row.
     * 
     * @param width
     *            the width to compute a height for.
     * @return the corresponding height.
     */
    public static final int getAppropriateHeight(final int width) {
        int height = (int) ((width / 3.5) * 2.0);
        // an odd-numbered height is useful for presentation - especially at
        // zoom 1.0
        return height % 2 == 0 ? height + 1 : height;
    }

    private static int[][] overZoom(int pixWidth, int pixHeight, int limit) {
        int[][] result = new int[pixHeight][];
        int[] row = new int[pixWidth];
        Arrays.fill(row, pixWidth / 2, pixWidth, limit);
        Arrays.fill(result, 0, pixHeight / 2, row);
        row = new int[pixWidth];
        Arrays.fill(row, 0, pixWidth / 2, limit);
        Arrays.fill(result, pixHeight / 2, pixHeight, row);
        return result;
    }

}
