package hypernova.gui;

import java.io.File;

import java.util.Calendar;
import java.util.Collection;
import java.util.Observer;
import java.util.Observable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import java.text.DecimalFormat;

import java.awt.Font;
import java.awt.Shape;
import java.awt.Color;
import java.awt.Polygon;
import java.awt.Graphics;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.FontMetrics;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.imageio.ImageIO;

import org.apache.log4j.Logger;

import hypernova.Ship;
import hypernova.Mass;
import hypernova.Universe;
import hypernova.Hypernova;
import hypernova.pilots.KeyboardPilot;
import hypernova.audio.MinimWrapper;
import hypernova.gui.backgrounds.*;

public class Viewer extends JComponent implements Observer {
    public static final long serialVersionUID = 850159523722721935l;
    public static final double MESSAGE_TIME = 2.0; // seconds
    public static final double MESSAGE_CORNER_TIME = 1.0; // seconds
    public static final double DEFAULT_SCALE = 2.0;
    public static final double SCALE_MAX = 4.0;
    public static final double SCALE_MIN = 0.75;
    public static final int QUALITY_DEFAULT = 2;
    public static final int QUALITY_MAX = 2;

    /* Minimap */
    public static final int MM_PAD = 10;
    public static final int MM_SIZE = 150;
    public static final double MM_SCALE = 0.025;
    public static final double MM_MSIZE = 75;
    public static final int MM_TSIZE = 100;
    public static final Shape MM_MASS = new Rectangle2D.Double(-MM_MSIZE, -MM_MSIZE, MM_MSIZE * 2, MM_MSIZE * 2);
    public static final Shape MINIMAP = new Ellipse2D.Double(1, 1, MM_SIZE - 2, MM_SIZE - 2);
    public static final int[] MM_XPOINTS = {-MM_TSIZE,-MM_TSIZE,MM_TSIZE*2};
    public static final int[] MM_YPOINTS = { MM_TSIZE,-MM_TSIZE,0};
    public static final Shape MM_SHIP = new Polygon(MM_XPOINTS,MM_YPOINTS,3);

    public static final double ZOOM_RATE = 1.2;

    public static final float[] STAR_COLORS = {1.0f, 0.66f, 0.33f};
    public static final int STAR_SEED = 0x9d2c5680;
 
    /* Starting size. */
    public static int WIDTH = 800;
    public static int HEIGHT = 600;

    private final Universe universe;
    private Mass focus;
    private double focusX, focusY;
    private double scale = DEFAULT_SCALE;
    private double targetScale = DEFAULT_SCALE;
    private int quality = QUALITY_DEFAULT; /* 0 - 2 quality setting. */

    private boolean record;
    private int recordCount;
    private static final String RECORD_FORMAT = "record-%08d.png";
    public BlockingQueue<File> saves = new LinkedBlockingQueue<File>(1);

    private double msgTime;
    private double msgCornerTime;
    private String message;
    private String cornerMessage;
    private boolean skipCornerProgress;

    private boolean isFaded = false;
    private static Background background = new MusicStarfield();

    private static Logger log = Logger.getLogger("gui.Viewer");

    public Viewer() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setOpaque(true);
        universe = Universe.get();
        universe.addObserver(this);
        addKeyListener(KeyboardPilot.get());
        addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {
                log.trace("keyPressed() " + e.getKeyCode());
                switch (e.getKeyCode()) {
                case KeyEvent.VK_PAGE_UP:
                    setScale(getScale() * ZOOM_RATE);
                    break;
                case KeyEvent.VK_PAGE_DOWN:
                    setScale(getScale() * (1 / ZOOM_RATE));
                    break;
                case KeyEvent.VK_P:
                    skipCornerProgress = true;
                    cornerMessage = "Paused";
                    repaint();
                    universe.togglePause();
                    break;
                case KeyEvent.VK_ESCAPE:
                    universe.togglePause(true);
                    System.exit(0);
                    break;
                case KeyEvent.VK_R:
                    record ^= true;
                    log.info("Recording set to " + record);
                    break;
                default:
                    log.trace("Unkown key " + e.getKeyCode());
                }
            }
            @Override
            public void keyReleased(KeyEvent e) {}
            @Override
            public void keyTyped(KeyEvent e) {}
        });

        new Thread(new Saver()).start();
    }

    public void setQuality(int q) {
        log.info("quality adjusted to " + q);
        quality = Math.max(q, 0);
        quality = Math.min(quality, QUALITY_MAX);
    }

    public int getQuality() {
        return quality;
    }

    public static void setBackground (Background bg) {
        background = bg;
    }

    public void setScale(double scale) {
        targetScale = Math.min(scale, SCALE_MAX);
        targetScale = Math.max(targetScale, SCALE_MIN);
        background.setScale(targetScale);
    }

    public double getScale() {
        return targetScale;
    }

    public void setFocus(Mass target) {
        focus = target;
    }

    @Override
    public void update(Observable o, Object msg) {
        updateFocus();
        if (record) {
            screenshot(new File(String.format(RECORD_FORMAT, recordCount++)));
        }
        repaint();
        MinimWrapper.loop();
    }
 
    private void updateFocus() {
        scale = (0.8 * scale + 0.2 * targetScale);
        background.setScale(scale);
        if (focus == null || !focus.isActive())
            focus = universe.getPlayer();
        double px, py;
        if (focus == null) {
            px = 0;
            py = 0;
        } else {
            px = focus.getX(0);
            py = focus.getY(0);
        }
        focusX = 0.6 * focusX + 0.4 * px;
        focusY = 0.6 * focusY + 0.4 * py;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        AffineTransform at = g2d.getTransform();
        if(background != null) background.drawBackground(g, g2d, focusX, focusY);
        g2d.setTransform(at);


        /* Set up graphics */
        if (quality > 0) {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                 RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                                 RenderingHints.VALUE_STROKE_PURE);
        }
        if (quality > 1) {
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                                 RenderingHints.VALUE_RENDER_QUALITY);
        }

        /* Wormhole */
        g2d.translate(-(focusX * scale - getWidth() / 2),
                      -(focusY * scale - getHeight() / 2));
        if(scale != 2) g2d.scale(scale/2, scale/2);
        Wormhole.drawAll(g2d);
        g2d.setTransform(at);

        /* Models */
        g2d.translate(-(focusX * scale - getWidth() / 2),
                      -(focusY * scale - getHeight() / 2));
        g2d.scale(scale, scale);
        Collection<Mass> objects = universe.getObjects();
        for (Mass m : objects) {
            drawMass(g2d, m);
        }
        g2d.setTransform(at);

        /* Minimap */
        minimap(objects, (Graphics2D) g2d.create(getWidth() - MM_PAD - MM_SIZE,
                MM_PAD, MM_SIZE, MM_SIZE));

        Info.paintInfo(g2d, getHeight(), focusX, focusY);
        g2d.setTransform(at);
        
        Transition.doTransition(g2d);
        paintOverlay(g2d);
    }

    private void minimap(Collection<Mass> objects, Graphics2D g) {
        g.setColor(Info.INFO_COLOR);
        g.fill(MINIMAP);
        g.setColor(Info.INFO_BORDER);
        g.draw(MINIMAP);
        MapMarker.drawAll(focusX, focusY, g);       
        g.setClip(MINIMAP);
        g.translate(-(focusX * MM_SCALE - MM_SIZE / 2),
                    -(focusY * MM_SCALE - MM_SIZE / 2));
        g.scale(MM_SCALE, MM_SCALE);

        AffineTransform at = new AffineTransform();
        for (Mass m : objects) {
            if (m.isShortlived() && !(m instanceof hypernova.Loot)) continue;
            at.setToTranslation(m.getX(0), m.getY(0));
            g.setColor(m.getFaction().getColor());
            if(m instanceof hypernova.Ship && ((Ship) m).getCanMove())
            {
              at.rotate(m.getA(0));
              g.fill(at.createTransformedShape(MM_SHIP));
            } else {
              g.fill(at.createTransformedShape(MM_MASS));
            }
            g.fill(at.createTransformedShape(MM_MASS));
        }
    }

    private void paintOverlay(Graphics2D g) {
        /* Display messages to the screen. */
        if (message == null || now() - msgTime > MESSAGE_TIME) {
            message = universe.nextMessage();
            msgTime = now();
            if (message != null)
                log.debug("Displaying: " + message);
        }
        if (cornerMessage == null || 
            now() - msgCornerTime > MESSAGE_CORNER_TIME) {
            cornerMessage = universe.nextCornerMessage();
            msgCornerTime = now();
            if (cornerMessage != null)
                log.debug("Displaying: " + cornerMessage);
        }
        if (message != null) {
            Font oldfont = g.getFont();
            g.setFont(oldfont.deriveFont(30f));
            FontMetrics fm = g.getFontMetrics();
            int width = fm.stringWidth(message);
            int x = getWidth() / 2;
            int y = getHeight() / 2;
            double progress = (1 - (now() - msgTime) / MESSAGE_TIME);
            int alpha = (int) (Math.sqrt(progress) * 255);
            g.setColor(new Color(0xff, 0xff, 0xff, alpha));
            g.drawString(message, x - width / 2, y - fm.getAscent() * 2);
        }

        if(cornerMessage != null)
        {
            Font oldfont = g.getFont();
            g.setFont(oldfont.deriveFont(15f));
            FontMetrics fm = g.getFontMetrics();
            int width = fm.stringWidth(cornerMessage);
            int x = getWidth();
            int y = getHeight();
            if(skipCornerProgress) {
              msgCornerTime = now();
              skipCornerProgress = false;
            }
            double progress = (1 - (now() - msgCornerTime) 
                            / MESSAGE_CORNER_TIME);
            int alpha = (int) (Math.sqrt(progress) * 255);
            g.setColor(new Color(0xff, 0xff, 0xB0, alpha));
            g.drawString(cornerMessage, x - width - 5, y - fm.getAscent());
        }
    }

    public void drawMass(Graphics2D g, Mass m) {
        g.setColor(m.getFaction().getColor());
        Model model = m.getModel();
        g.draw(model.getShape());
        g.fill(model.getFilled());
    }

    public static final int STAR_TILE_SIZE = 256;
    public void drawStars(Graphics2D g, int xoff, int yoff, int starscale) {
        int size = STAR_TILE_SIZE / starscale;
        int w = (int) (getWidth() / (scale / DEFAULT_SCALE));
        int h = (int) (getHeight() / (scale / DEFAULT_SCALE));
        int c = (int)(MinimWrapper.fft(4))[starscale];


        /* Set colors */
        if(starscale == 1) g.setColor(Color.cyan);
        else if(starscale == 2) g.setColor(Color.blue);
        else if(starscale == 3) g.setColor(Color.yellow);
        
        /* Top-left tile's top-left position. */
        int sx = ((xoff - w/2) / size) * size - size;
        int sy = ((yoff - h/2) / size) * size - size;

        /* Draw each tile currently in view. */
        for (int i = sx; i <= w + sx + size * 3; i += size) {
            for (int j = sy; j <= h + sy + size * 3; j += size) {
                int hash = mix(STAR_SEED, i, j);
                for (int n = 0; n < 3; n++) {
                    int px = (hash % size) + (i - xoff);
                    hash >>= 3;
                    int py = (hash % size) + (j - yoff);
                    hash >>= 3;
                    px *= scale / DEFAULT_SCALE;
                    py *= scale / DEFAULT_SCALE;
                    g.drawLine(px - c, py - c, px+c, py+ c);
                    g.drawLine(px + c, py - c, px-c, py+c);
                    g.drawLine(px-c, py , px + c, py);
                    g.drawLine(px, py-c , px, py+c);

                }
            }
        }
    }

    /** Robert Jenkins' 96 bit Mix Function.
     * @param a random bits
     * @param b random bits
     * @param c the "key" to be hashed
     */
    private static int mix(int a, int b, int c) {
        a=a-b;  a=a-c;  a=a^(c >>> 13);
        b=b-c;  b=b-a;  b=b^(a << 8);
        c=c-a;  c=c-b;  c=c^(b >>> 13);
        a=a-b;  a=a-c;  a=a^(c >>> 12);
        b=b-c;  b=b-a;  b=b^(a << 16);
        c=c-a;  c=c-b;  c=c^(b >>> 5);
        a=a-b;  a=a-c;  a=a^(c >>> 3);
        b=b-c;  b=b-a;  b=b^(a << 10);
        c=c-a;  c=c-b;  c=c^(b >>> 15);
        return c;
    }

    private static double now() {
        return Calendar.getInstance().getTimeInMillis() / 1000.0d;
    }

    public void screenshot(File file) {
        try {
            saves.put(file);
        } catch (Exception e) {
            log.error("Failed to queue screenshot: " + file + ": " + e);
        }
    }

    private class Saver implements Runnable {
        @Override
        public void run() {
            BufferedImage img = null;
            int w = 0, h = 0;
            File file = null;
            while (true) {
                try {
                    file = saves.take();
                    if (w != getWidth() || h != getHeight()) {
                        img = new BufferedImage(getWidth(), getHeight(),
                                                BufferedImage.TYPE_INT_RGB);
                        w = getWidth();
                        h = getHeight();
                    }
                    Graphics g = img.getGraphics();
                    paintComponent(g);
                    g.dispose();
                    ImageIO.write(img, "PNG", file);
                    log.info("Wrote " + file);
                } catch (Exception e) {
                    log.error("Failed to save screenshot: " + file + ": " + e);
                }
            }
        }
    }
}
