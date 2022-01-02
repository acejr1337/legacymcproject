package net.minecraft;

import java.awt.event.MouseEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.awt.FontMetrics;
import java.awt.Font;
import java.awt.Color;
import java.awt.image.ImageObserver;
import java.awt.Graphics;
import java.awt.Component;
import java.awt.LayoutManager;
import java.awt.BorderLayout;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.HashMap;
import java.awt.image.VolatileImage;
import java.awt.Image;
import java.util.Map;
import java.awt.event.MouseListener;
import java.applet.AppletStub;
import java.applet.Applet;

public class Launcher extends Applet implements Runnable, AppletStub, MouseListener
{
    private static final long serialVersionUID = 1L;
    public Map<String, String> customParameters;
    private GameUpdater gameUpdater;
    private boolean gameUpdaterStarted;
    private Applet applet;
    private Image bgImage;
    private boolean active;
    private int context;
    private boolean hasMouseListener;
    private VolatileImage img;
    
    public Launcher() {
        this.customParameters = new HashMap<String, String>();
        this.gameUpdaterStarted = false;
        this.active = false;
        this.context = 0;
        this.hasMouseListener = false;
    }
    
    @Override
    public boolean isActive() {
        if (this.context == 0) {
            this.context = -1;
            try {
                if (this.getAppletContext() != null) {
                    this.context = 1;
                }
            }
            catch (Exception ex) {}
        }
        if (this.context == -1) {
            return this.active;
        }
        return super.isActive();
    }
    
    public void init(final String s, final String s2, final String s3, final String s4) {
        try {
            this.bgImage = ImageIO.read(LoginForm.class.getResource("dirt.png")).getScaledInstance(32, 32, 16);
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
        this.customParameters.put("username", s);
        this.customParameters.put("sessionid", s4);
        this.gameUpdater = new GameUpdater(s2, "minecraft.jar?user=" + s + "&ticket=" + s3, this.customParameters.containsKey("noupdate"));
    }
    
    public boolean canPlayOffline() {
        return this.gameUpdater.canPlayOffline();
    }
    
    @Override
    public void init() {
        if (this.applet != null) {
            this.applet.init();
            return;
        }
        this.init(this.getParameter("userName"), this.getParameter("latestVersion"), this.getParameter("downloadTicket"), this.getParameter("sessionId"));
    }
    
    @Override
    public void start() {
        if (this.applet != null) {
            this.applet.start();
            return;
        }
        if (this.gameUpdaterStarted) {
            return;
        }
        final Thread thread = new Thread() {
            @Override
            public void run() {
                Launcher.this.gameUpdater.run();
                try {
                    if (!Launcher.this.gameUpdater.fatalError) {
                        Launcher.this.replace(Launcher.this.gameUpdater.createApplet());
                    }
                }
                catch (ClassNotFoundException ex) {
                    ex.printStackTrace();
                }
                catch (InstantiationException ex2) {
                    ex2.printStackTrace();
                }
                catch (IllegalAccessException ex3) {
                    ex3.printStackTrace();
                }
            }
        };
        thread.setDaemon(true);
        thread.start();
        final Thread thread2 = new Thread() {
            @Override
            public void run() {
                while (Launcher.this.applet == null) {
                    Launcher.this.repaint();
                    try {
                        Thread.sleep(10L);
                    }
                    catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        };
        thread2.setDaemon(true);
        thread2.start();
        this.gameUpdaterStarted = true;
    }
    
    @Override
    public void stop() {
        if (this.applet != null) {
            this.active = false;
            this.applet.stop();
        }
    }
    
    @Override
    public void destroy() {
        if (this.applet != null) {
            this.applet.destroy();
        }
    }
    
    public void replace(final Applet applet) {
        (this.applet = applet).setStub(this);
        applet.setSize(this.getWidth(), this.getHeight());
        this.setLayout(new BorderLayout());
        this.add(applet, "Center");
        applet.init();
        this.active = true;
        applet.start();
        this.validate();
    }
    
    @Override
    public void update(final Graphics graphics) {
        this.paint(graphics);
    }
    
    @Override
    public void paint(final Graphics graphics) {
        if (this.applet != null) {
            return;
        }
        final int n = this.getWidth() / 2;
        final int n2 = this.getHeight() / 2;
        if (this.img == null || this.img.getWidth() != n || this.img.getHeight() != n2) {
            this.img = this.createVolatileImage(n, n2);
        }
        final Graphics graphics2 = this.img.getGraphics();
        for (int i = 0; i <= n / 32; ++i) {
            for (int j = 0; j <= n2 / 32; ++j) {
                graphics2.drawImage(this.bgImage, i * 32, j * 32, null);
            }
        }
        if (this.gameUpdater.pauseAskUpdate) {
            if (!this.hasMouseListener) {
                this.hasMouseListener = true;
                this.addMouseListener(this);
            }
            graphics2.setColor(Color.LIGHT_GRAY);
            final String s = "New update available";
            graphics2.setFont(new Font(null, 1, 20));
            final FontMetrics fontMetrics = graphics2.getFontMetrics();
            graphics2.drawString(s, n / 2 - fontMetrics.stringWidth(s) / 2, n2 / 2 - fontMetrics.getHeight() * 2);
            graphics2.setFont(new Font(null, 0, 12));
            final FontMetrics fontMetrics2 = graphics2.getFontMetrics();
            graphics2.fill3DRect(n / 2 - 56 - 8, n2 / 2, 56, 20, true);
            graphics2.fill3DRect(n / 2 + 8, n2 / 2, 56, 20, true);
            final String s2 = "Would you like to update?";
            graphics2.drawString(s2, n / 2 - fontMetrics2.stringWidth(s2) / 2, n2 / 2 - 8);
            graphics2.setColor(Color.BLACK);
            final String s3 = "Yes";
            graphics2.drawString(s3, n / 2 - 56 - 8 - fontMetrics2.stringWidth(s3) / 2 + 28, n2 / 2 + 14);
            final String s4 = "Not now";
            graphics2.drawString(s4, n / 2 + 8 - fontMetrics2.stringWidth(s4) / 2 + 28, n2 / 2 + 14);
        }
        else {
            graphics2.setColor(Color.LIGHT_GRAY);
            String s5 = "Updating Minecraft";
            if (this.gameUpdater.fatalError) {
                s5 = "Failed to launch";
            }
            graphics2.setFont(new Font(null, 1, 20));
            final FontMetrics fontMetrics3 = graphics2.getFontMetrics();
            graphics2.drawString(s5, n / 2 - fontMetrics3.stringWidth(s5) / 2, n2 / 2 - fontMetrics3.getHeight() * 2);
            graphics2.setFont(new Font(null, 0, 12));
            final FontMetrics fontMetrics4 = graphics2.getFontMetrics();
            String s6 = this.gameUpdater.getDescriptionForState();
            if (this.gameUpdater.fatalError) {
                s6 = this.gameUpdater.fatalErrorDescription;
            }
            graphics2.drawString(s6, n / 2 - fontMetrics4.stringWidth(s6) / 2, n2 / 2 + fontMetrics4.getHeight() * 1);
            final String subtaskMessage = this.gameUpdater.subtaskMessage;
            graphics2.drawString(subtaskMessage, n / 2 - fontMetrics4.stringWidth(subtaskMessage) / 2, n2 / 2 + fontMetrics4.getHeight() * 2);
            if (!this.gameUpdater.fatalError) {
                graphics2.setColor(Color.black);
                graphics2.fillRect(64, n2 - 64, n - 128 + 1, 5);
                graphics2.setColor(new Color(32768));
                graphics2.fillRect(64, n2 - 64, this.gameUpdater.percentage * (n - 128) / 100, 4);
                graphics2.setColor(new Color(2138144));
                graphics2.fillRect(65, n2 - 64 + 1, this.gameUpdater.percentage * (n - 128) / 100 - 2, 1);
            }
        }
        graphics2.dispose();
        graphics.drawImage(this.img, 0, 0, n * 2, n2 * 2, null);
    }
    
    public void run() {
    }
    
    @Override
    public String getParameter(final String s) {
        final String s2 = this.customParameters.get(s);
        if (s2 != null) {
            return s2;
        }
        try {
            return super.getParameter(s);
        }
        catch (Exception ex) {
            this.customParameters.put(s, null);
            return null;
        }
    }
    
    public void appletResize(final int n, final int n2) {
    }
    
    @Override
    public URL getDocumentBase() {
        try {
            return new URL("http://www.minecraft.net/game/");
        }
        catch (MalformedURLException ex) {
            ex.printStackTrace();
            return null;
        }
    }
    
    public void mouseClicked(final MouseEvent mouseEvent) {
    }
    
    public void mouseEntered(final MouseEvent mouseEvent) {
    }
    
    public void mouseExited(final MouseEvent mouseEvent) {
    }
    
    public void mousePressed(final MouseEvent mouseEvent) {
        final int n = mouseEvent.getX() / 2;
        final int n2 = mouseEvent.getY() / 2;
        final int n3 = this.getWidth() / 2;
        final int n4 = this.getHeight() / 2;
        if (this.contains(n, n2, n3 / 2 - 56 - 8, n4 / 2, 56, 20)) {
            this.removeMouseListener(this);
            this.gameUpdater.shouldUpdate = true;
            this.gameUpdater.pauseAskUpdate = false;
            this.hasMouseListener = false;
        }
        if (this.contains(n, n2, n3 / 2 + 8, n4 / 2, 56, 20)) {
            this.removeMouseListener(this);
            this.gameUpdater.shouldUpdate = false;
            this.gameUpdater.pauseAskUpdate = false;
            this.hasMouseListener = false;
        }
    }
    
    private boolean contains(final int n, final int n2, final int n3, final int n4, final int n5, final int n6) {
        return n >= n3 && n2 >= n4 && n < n3 + n5 && n2 < n4 + n6;
    }
    
    public void mouseReleased(final MouseEvent mouseEvent) {
    }
}
