import java.awt.Canvas;
import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;

//Based on code from ftp://ftp.lal.in2p3.fr/pub/EcoleJava99/Java14/991126/analysis/Histogram2D.java

public class Histogram extends Canvas{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Frame frame = new Frame("2D Histogram");

    private double[][] values;
    private long entries = 0;
    private long errors = 0;
    private int xbin;
    private int ybin;
    private double xmin;
    private double xmax;
    private double ymin;
    private double ymax;
    private double vmax;
    private int top, bottom, left, right;

    private Color[] color = new Color[256];

    private Label entriesLabel, errorsLabel, xbinLabel, ybinLabel;
    private Label xminLabel, xmaxLabel, yminLabel, ymaxLabel, vmaxLabel;
    private Panel info;

	private double vmax2;

    double[][] getValues() {
        return values;
    }
    
    double getXmin() {
        return xmin;
    }
    
    double getXmax() {
        return xmax;
    }
    
    double getYmin() {
        return ymin;
    }
    
    double getYmax() {
        return ymax;
    }

    /**
     * constructs a (hidden) histogram.
     *
     * @param xbin  number of x bins
     * @param ybin  number of y bins
     * @param xmin  min value for x
     * @param xmax  max value for x
     * @param ymin  min value for y
     * @param ymax  max value for y
     */
    public Histogram(int xbin, int ybin, double xmin, double xmax, double ymin, double ymax) {
        super();
        this.xbin = xbin;
        this.ybin = ybin;
        this.xmin = xmin;
        this.xmax = xmax;
        this.ymin = ymin;
        this.ymax = ymax;
        
        values = new double[xbin][ybin];

        for (int i=0; i<color.length; i++) {
            color[i] = new Color(Color.HSBtoRGB((color.length - i) * 0.66f / color.length, 1.0f, 1.0f));
        }
        
        top = 10;
        bottom = 20;
        left = 20;
        right = 10;

        // build info panel
        info = new Panel();
        info.setLayout(new GridLayout(0,2));
        info.add(new Label("Entries:"));
        info.add(entriesLabel = new Label());
        info.add(new Label("O/U-flow:"));
        info.add(errorsLabel = new Label());
        info.add(new Label("XBins:"));
        info.add(xbinLabel = new Label());
        info.add(new Label("YBins:"));
        info.add(ybinLabel = new Label());
        info.add(new Label("Xmin:"));
        info.add(xminLabel = new Label());
        info.add(new Label("Xmax:"));
        info.add(xmaxLabel = new Label());
        info.add(new Label("Ymin:"));
        info.add(yminLabel = new Label());
        info.add(new Label("Ymax:"));
        info.add(ymaxLabel = new Label());
        info.add(new Label("Vmax:"));
        info.add(vmaxLabel = new Label());
        
        clear();
    }

    /**
     * resets the histogram values
     */
    public void clear() {
    	values = new double[xbin][ybin];
    	entries = 0;
        errors = 0;
        repaint();
    }
    
    public void histclear() {
    	values = new double[xbin][ybin];
    	entries = 0;
        errors = 0;
        //repaint();
    }
    
    /**
     * (internal use) draws actual histogram.
     *
     * @param g graphics context
     */
    public synchronized void paint(Graphics g) {
    
        Dimension d = getSize();

        double xwidth = ((double)d.width - left - right) / xbin;
        double ywidth = ((double)d.height - top - bottom) / ybin;
        vmax = 0;
        vmax2=vmax;
        
        
        for (int i=0; i<values.length; i++) {
            for (int j=0; j<values[i].length; j++) {
            	
            	if(values[i][j]>vmax)
            	{
            		vmax2=vmax;
            		vmax=values[i][j];
            	}
            	else if(values[i][j]>vmax2)
            	{
            		vmax2=values[i][j];
            	}
            	
                //vmax = Math.max(vmax, values[i][j]);
                
            }
        }
        // ignore value for the moment

        for (int i=0; i<values.length; i++) {
            for (int j=0; j<values[i].length; j++) {
                if (values[i][j] > 0) {
                    int x = (int)(i * xwidth + 0.5) + left;
                    int y = d.height - bottom - (int)(j * ywidth + 0.5);
                    int index=0;
                    if(values[i][j] > vmax2){
                    	 index = (int)(((double)values[i][j]) * (color.length-1) / vmax);
                    }else{
                    	 index = (int)(((double)values[i][j]) * (color.length-1) / vmax2);
                    }
                    g.setColor(color[index]);
                    g.drawRect(x,y,2,2);
                }
            }
        }

        // update labels
        entriesLabel.setText(""+entries);
        errorsLabel.setText(""+errors);
        xbinLabel.setText(""+xbin);
        ybinLabel.setText(""+ybin);
        xminLabel.setText(""+xmin);
        xmaxLabel.setText(""+xmax);
        yminLabel.setText(""+ymin);
        ymaxLabel.setText(""+ymax);
        vmaxLabel.setText(""+vmax);
    }

    /**
     * add value to the histogram. Values outside interval xmin-xmax, ymin-ymax are counted as errors, others
     * as entries.
     *
     * @param x x-value value to be added to histogram
     * @param y y-value value to be added to histogram
     */
    public synchronized void add(double x, double y) {

        int xindex = 0;
        int yindex = 0;
        try {
            if ((x >= xmin) && (x <= xmax) &&
                (y >= ymin) && (y <= ymax)) {
                double xwidth = (xmax - xmin) / xbin;
                double ywidth = (ymax - ymin) / ybin;
                xindex = (int)((x - xmin) / xwidth);
                yindex = (int)((y - ymin) / ywidth);
                values[xindex][yindex]++;
            } else {
//                System.out.println("value ignored ("+x+","+y+"), not in interval.");
                errors++;
                values[0][0]++;
            }
            entries++;
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println(e);
        }
    }

    /**
     * sets the histogram to the one provided as an argument.
     * The number of bins, min and max have to be equal.
     *
     * @param h histogram to be copied
     */
    public synchronized void set(Histogram h) {
        if ((h.xbin == xbin) && (h.ybin == ybin) &&
            (h.xmin == xmin) && (h.xmax == xmax) &&
            (h.ymin == ymin) && (h.ymax == ymax)) {
            entries = h.entries;
            errors = h.errors;
            for (int i=0; i<h.values.length; i++) {
                for (int j=0; j<h.values[i].length; j++) {
                    values[i][j] = h.values[i][j];
                }
            }
            repaint();
        } else {
            System.out.println("ERROR: cannot set, needs equal bin, min and max");
        }
    }

    /**
     * merges this histogram with the one provided as an argument.
     * The number of bins, xmin and xmax have to be equal.
     *
     * @param h histogram to be used when merging
     */
    public synchronized void merge(Histogram h) {
        if ((h.xbin == xbin) && (h.ybin == ybin) &&
            (h.xmin == xmin) && (h.xmax == xmax) &&
            (h.ymin == ymin) && (h.ymax == ymax)) {
            entries += h.entries;
            errors += h.errors;
            for (int i=0; i<h.values.length; i++) {
                for (int j=0; j<h.values[i].length; j++) {
                    values[i][j] += h.values[i][j];
                }
            }
            repaint();
        } else {
            System.out.println("ERROR: cannot set, needs equal bin, min and max");
        }
    }

    /**
     * (internal use) provides default size
     *
     * @return preferred size
     */
    public Dimension getPreferredSize() {
        return new Dimension(400,400);
    }

    /**
     * creates a disposable frame and shows the histogram in it.
     */
    public void show() {
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
               
                frame.dispose();
            }
        });
        frame.setLayout(new BorderLayout());
        frame.add("Center", this);
        frame.add("East", info);
        frame.pack();
        frame.setVisible(true);
    }

    /**
     * disposes of the histogram and its frame
     */
    public void dispose() {
        frame.dispose();
    }

    /**
     * (internal use) issues a repaint of the histogram, but sleeps in the current thread for 5 ms, to let
     * the update thread paint the update into the histogram.
     */
    public void repaint() {
        super.repaint();
        try {
            Thread.sleep(500);
        } catch (Exception e) {
        }
    }

	
}
