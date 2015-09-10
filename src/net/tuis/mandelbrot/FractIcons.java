package net.tuis.mandelbrot;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.Icon;

enum FractIcons implements Icon {
    S16(16),
    S32(32),
    S63(64),
    S128(128),
    S256(256),
    S512(512);
    
    private static final int limit = 969;
    
    private final BufferedImage image;
    private final int size;

    private FractIcons(final int size) {
        this.size = size;
        int[][] matrix = Mandelbrot.mandelbrot(size, size, limit, new Mandelbrot.Window(0.0, 0.0, 0.9));
        this.image = Mandelbrot.mapMandelbrot(matrix, Mandelbrot.buildColors(limit));
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        g.drawImage(image, x, y, null);
        
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }
    
    public Image getImage() {
        return image;
    }

    public static final List<Image> getIcons() {
        return new ArrayList<>(Stream.of(values()).map(FractIcons::getImage).collect(Collectors.toList()));
    }
    

}
