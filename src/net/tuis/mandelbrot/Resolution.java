package net.tuis.mandelbrot;

enum Resolution {
    SVGA(800,600, "SVGA"),
    HD720(1280, 720, "HD 720"),
    HD900(1600, 900, "HD+"),
    HD1080(1920, 1080, "HD 1080"),
    UHD4K(4096, 2304, "UHD 4K"),
    CHD(8192, 4608, "Crazy 8K");
    
    private final int width, height;
    private final String text;
    
    private Resolution(int width, int height, String text) {
        this.width = width;
        this.height = height;
        this.text = String.format("%s (%d x %d)", text, width, height);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public String toString() {
        return text;
    }

}
