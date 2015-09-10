package net.tuis.mandelbrot;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;

/**
 * Main class for running and controlling the Mandelbrot GUI.
 * @author rolf
 */
public class Mandy extends JFrame {
    
    private static final long serialVersionUID = 1L;
    
    // default width for image.
    private static final int DEFAULT_WIDTH = 1024;
    
    /**
     * Run the main GUI app.
     * @param args all arguments ignored.
     */
    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(() -> new Mandy().setVisible(true));        
    }

    private final BlockingQueue<WindowState> stateq = new LinkedBlockingQueue<>();
    private final JLabel canvas = new JLabel();
    private final SpinnerNumberModel zoomModel = new SpinnerNumberModel(0.0, -1.0, 150, 0.1);
    private final SpinnerNumberModel realModel = new SpinnerNumberModel(0.0, -2.5, 1.0, 0.1);
    private final SpinnerNumberModel imaginaryModel = new SpinnerNumberModel(0.0, -1.5, 1.5, 0.1);
    private final JSpinner limit = new JSpinner(new SpinnerNumberModel(100, 10, 100000, 10));
    private final JSpinner zoom = new JSpinner(zoomModel);
    private final JSpinner real = new JSpinner(realModel);
    private final JSpinner imaginary = new JSpinner(imaginaryModel);
    private final JLabel actualSpan = new JLabel();
    private final JLabel actualZoom = new JLabel();
    
    // only ever changed on the EDT
    private final AtomicReference<WindowState> currentState = new AtomicReference<>(new WindowState(0, 0, 0, 0, 0, 0, 0));
    
    Mandy() {
        super("Mandelbrot Navigator");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        
        JPanel root = new JPanel(new BorderLayout());
        
        canvas.setPreferredSize(new Dimension(DEFAULT_WIDTH, Mandelbrot.getAppropriateHeight(DEFAULT_WIDTH)));
        
        JPanel filler = new JPanel(new BorderLayout());
        JPanel controls = new JPanel();
        controls.setBorder(new BevelBorder(BevelBorder.LOWERED));
        controls.setLayout(new GridLayout(0, 2, 3, 3));
        controls.add(new JLabel("Zoom (exponential):"));
        controls.add(zoom);
        controls.add(new JLabel("Limit:"));
        controls.add(limit);
        controls.add(new JLabel("Real:"));
        controls.add(real);
        controls.add(new JLabel("Imaginary:"));
        controls.add(imaginary);
        controls.add(new JLabel("Actual Zoom:"));
        controls.add(actualZoom);
        controls.add(new JLabel("Actual Span:"));
        controls.add(actualSpan);
        
        actualZoom.setHorizontalAlignment(SwingConstants.RIGHT);
        actualSpan.setHorizontalAlignment(SwingConstants.RIGHT);
        zoom.setEditor(new JSpinner.NumberEditor(zoom, "0.0"));
        real.setEditor(new JSpinner.NumberEditor(real, "0.0000000000000000"));
        imaginary.setEditor(new JSpinner.NumberEditor(imaginary, "0.0000000000000000"));

        Stream.of(zoom.getModel(), real.getModel(), imaginary.getModel(), limit.getModel())
            .forEach(m -> m.addChangeListener(e -> checkState()));
        
        canvas.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                checkState();
            }
        });
        
        MouseAdapter mouse = new MouseAdapter() {
            private int sx, sy;
            private boolean active = false;
            @Override
            public void mouseDragged(MouseEvent e) {
                if (!active) {
                    return;
                }
                int x = e.getX();
                int y = e.getY();
                int dx = x - sx;
                int dy = y - sy;
                WindowState state = currentState.get();
                if (state == null) {
                    return;
                }
                realModel.setValue(state.getFocusX() - dx * state.getStep());
                imaginaryModel.setValue(state.getFocusY() - dy * state.getStep());
                sx = x;
                sy = y;
                checkState();
            }
            @Override
            public void mousePressed(MouseEvent e) {
                active = e.getModifiersEx() == MouseEvent.BUTTON1_DOWN_MASK;
                sx = e.getX();
                sy = e.getY();
            }
            
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && e.getButton() == 1) {
                    // center on click point.
                    WindowState state = currentState.get();
                    if (state == null) {
                        return;
                    }
                    int dx = state.getPixWidth() / 2 - e.getX();
                    int dy = state.getPixHeight() / 2 - e.getY();
                    double mx = state.getFocusX() - state.getStep() * dx;
                    double my = state.getFocusY() - state.getStep() * dy;
                    realModel.setValue(mx);
                    imaginaryModel.setValue(my);
                    checkState();
                }
            }
            
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                WindowState state = currentState.get();
                if (state == null) {
                    return;
                }
                double val = (double)zoomModel.getValue();
                double nv = val - e.getWheelRotation() * zoomModel.getStepSize().doubleValue();
                zoomModel.setValue(Math.min((double)zoomModel.getMaximum(), Math.max((double)zoomModel.getMinimum(), nv)));
                checkState();
            }
        };
        
        canvas.addMouseMotionListener(mouse);
        canvas.addMouseListener(mouse);
        canvas.addMouseWheelListener(mouse);
        
        root.add(canvas, BorderLayout.CENTER);
        filler.add(controls, BorderLayout.NORTH);
        root.add(filler, BorderLayout.EAST);
        getContentPane().add(root);
        pack();
        
        checkState();
        
        Thread t = new Thread(() -> manageQueue(), "Mandelbrot Control");
        t.setDaemon(true);
        t.start();
        
    }
    
    /**
     * Monitor the update queue, and process new images as needed.
     */
    private void manageQueue() {
        Deque<WindowState> pending = new ArrayDeque<>();
        while (true) {
            try {
                pending.add(stateq.take());
                stateq.drainTo(pending);
                WindowState recent = pending.getLast();
                pending.clear();
                buildBrot(recent);
            } catch (InterruptedException e) {
                // ignore interruptions entirely.
                e.printStackTrace();
            }
        }
    }

    /**
     * Identify whether the image needs a redraw - schedule if needed.
     */
    private void checkState() {
        final int lim = ((Number)limit.getValue()).intValue();
        final double x = ((Number)real.getValue()).doubleValue();
        final double y = ((Number)imaginary.getValue()).doubleValue();
        final double z = Math.pow(10.0, ((Number)zoom.getValue()).doubleValue());
        final int w = canvas.getWidth();
        final int h = canvas.getHeight();
        final double span = 3.5 / z;
        final double step = span / w;
        
        final WindowState now = new WindowState(w, h, lim, x, y, z, step);
        
        if (currentState.getAndSet(now).equals(now)) {
            // previous value is same as current.
            return;
        }
        
        // add the state to the queue.... the control thread should pick that up.
        if (!stateq.offer(now)) {
            throw new IllegalStateException("Unable to process current state.");
        }
        realModel.setStepSize(step * 5);
        imaginaryModel.setStepSize(step * 5);
        actualZoom.setText(String.format("%8g", z));
        actualSpan.setText(String.format("%8g", span));
        
    }

    private final ThreadLocal<int[]> colormap = new ThreadLocal<>();
    
    private void buildBrot(WindowState state) {
        int[] cmap = colormap.get();
        if (cmap == null || cmap.length != state.getLimit() + 1) {
            cmap = Mandelbrot.buildColors(state.getLimit());
            colormap.set(cmap);
            System.out.println("Built color map of " + cmap.length + " colors");
        }
        Mandelbrot.Window window = new Mandelbrot.Window(state.getFocusX(), state.getFocusY(), state.getZoom());
        int[][] brot = Mandelbrot.mandelbrot(state.getPixWidth(), state.getPixHeight(), state.getLimit(), window);
        BufferedImage image = Mandelbrot.mapMandelbrot(brot, cmap);
        final Icon icon = new ImageIcon(image);
        SwingUtilities.invokeLater(() -> canvas.setIcon(icon));
    }

}
