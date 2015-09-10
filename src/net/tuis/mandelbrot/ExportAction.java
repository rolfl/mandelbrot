package net.tuis.mandelbrot;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;
import javax.swing.filechooser.FileFilter;

class ExportAction extends AbstractAction implements ThreadFactory{
    
    private static final long serialVersionUID = 1L;

    private static FileFilter pngs = new FileFilter() {

        @Override
        public boolean accept(File f) {
            return f.isDirectory() || f.getName().toLowerCase().endsWith(".png");
        }

        @Override
        public String getDescription() {
            return "*.png";
        }
        
    };
    
    private final AtomicReference<WindowState> state;
    private final JComboBox<Resolution> resCB;
    private final JFileChooser fChooser = new JFileChooser();

    public ExportAction(AtomicReference<WindowState> state, JComboBox<Resolution> res) {
        super("Export");
        this.state = state;
        this.resCB = res;
        fChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fChooser.setAcceptAllFileFilterUsed(false);
        fChooser.setFileFilter(pngs);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        setEnabled(false);

        ProgressMonitor monitor = new ProgressMonitor((Component)e.getSource(), "Exporting Mandelbrot", "Initializing", 0, 7);
        
        final WindowState current = state.get();
        final Resolution res = Resolution.values()[resCB.getSelectedIndex()];

        monitor.setNote("Submitting background build");
        
        String name = String.format("Mandelbrot_real%f_imag%f_zoom%f_limit%d.png", current.getFocusX(), current.getFocusY(), current.getZoom(), current.getLimit());
        ExecutorService service = Executors.newCachedThreadPool(this);
        Future<int[][]> future = service.submit(() -> buildImage(res, current));
        
        monitor.setProgress(1);
        monitor.setNote("Selecting destination");
        
        fChooser.setSelectedFile(new File(fChooser.getCurrentDirectory(), name));
        if (JFileChooser.APPROVE_OPTION == fChooser.showSaveDialog((Component)e.getSource())) {
            monitor.setProgress(2);
            
            monitor.setNote("Background processing");
            service.execute(() -> saveFile(future, fChooser.getSelectedFile(), current, monitor));
        } else {
            monitor.close();
        }
        service.shutdown();
        
    }

    private void saveFile(Future<int[][]> future, File selectedFile, WindowState currentState, ProgressMonitor monitor) {
        monitor.setProgress(3);
        
        monitor.setNote("Building Colours");

        try {
            int[] colors = Mandelbrot.buildColors(currentState.getLimit());
            monitor.setProgress(4);
            monitor.setNote("Awaiting matrix completion");

            int[][] matrix = future.get();
            monitor.setProgress(5);
            monitor.setNote("Building image");

            BufferedImage image = Mandelbrot.mapMandelbrot(matrix, colors);
            monitor.setProgress(6);
            monitor.setNote("Saving image");

            ImageIO.write(image, "png", selectedFile);
            monitor.setProgress(7);
        } catch (InterruptedException | ExecutionException | IOException e) {
            try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw);) {
                e.printStackTrace(pw);
                JOptionPane.showMessageDialog(null, sw.toString(), "Unable to save file " + selectedFile, JOptionPane.ERROR_MESSAGE);
            } catch (IOException e1) {
                // ignore
            }
        } finally {
            monitor.close();
            setEnabled(true);
        }
        
    }

    private int[][] buildImage(Resolution res, WindowState currentState) {
        return Mandelbrot.mandelbrot(res.getWidth(), res.getHeight(), currentState.getLimit(), 
                new Mandelbrot.Window(currentState.getFocusX(), currentState.getFocusY(), currentState.getZoom()));
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r, "Mandelbrot Export");
        t.setDaemon(true);
        return t;
    }

}
