package net.tuis.mandelbrot;


final class WindowState {
    private final int pixWidth, pixHeight, limit;
    private final double focusX, focusY, zoom;

    public WindowState(int pixWidth, int pixHeight, int limit, double focusX, double focusY, double zoom) {
        super();
        this.pixWidth = pixWidth;
        this.pixHeight = pixHeight;
        this.limit = limit;
        this.focusX = focusX;
        this.focusY = focusY;
        this.zoom = zoom;
    }

    public int getPixWidth() {
        return pixWidth;
    }

    public int getPixHeight() {
        return pixHeight;
    }

    public int getLimit() {
        return limit;
    }

    public double getFocusX() {
        return focusX;
    }

    public double getFocusY() {
        return focusY;
    }

    public double getZoom() {
        return zoom;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(focusX);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(focusY);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + limit;
        result = prime * result + pixHeight;
        result = prime * result + pixWidth;
        temp = Double.doubleToLongBits(zoom);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        WindowState other = (WindowState) obj;
        if (Double.doubleToLongBits(focusX) != Double.doubleToLongBits(other.focusX)) {
            return false;
        }
        if (Double.doubleToLongBits(focusY) != Double.doubleToLongBits(other.focusY)) {
            return false;
        }
        if (limit != other.limit) {
            return false;
        }
        if (pixHeight != other.pixHeight) {
            return false;
        }
        if (pixWidth != other.pixWidth) {
            return false;
        }
        if (Double.doubleToLongBits(zoom) != Double.doubleToLongBits(other.zoom)) {
            return false;
        }
        return true;
    }

}