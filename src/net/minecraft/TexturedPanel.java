package net.minecraft;

import java.awt.Paint;
import java.awt.GradientPaint;
import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.Graphics2D;
import java.awt.image.ImageObserver;
import java.awt.Graphics;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.awt.Image;
import javax.swing.JPanel;

public class TexturedPanel extends JPanel
{
    private static final long serialVersionUID = 1L;
    private Image img;
    private Image bgImage;
    
    public TexturedPanel() {
        this.setOpaque(true);
        try {
            this.bgImage = ImageIO.read(LoginForm.class.getResource("dirt.png")).getScaledInstance(32, 32, 16);
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    @Override
    public void update(final Graphics graphics) {
        this.paint(graphics);
    }
    
    public void paintComponent(final Graphics graphics) {
        final int n = this.getWidth() / 2 + 1;
        final int n2 = this.getHeight() / 2 + 1;
        if (this.img == null || this.img.getWidth(null) != n || this.img.getHeight(null) != n2) {
            this.img = this.createImage(n, n2);
            final Graphics graphics2 = this.img.getGraphics();
            for (int i = 0; i <= n / 32; ++i) {
                for (int j = 0; j <= n2 / 32; ++j) {
                    graphics2.drawImage(this.bgImage, i * 32, j * 32, null);
                }
            }
            if (graphics2 instanceof Graphics2D) {
                final Graphics2D graphics2D = (Graphics2D)graphics2;
                final int n3 = 1;
                graphics2D.setPaint(new GradientPaint(new Point2D.Float(0.0f, 0.0f), new Color(553648127, true), new Point2D.Float(0.0f, (float)n3), new Color(0, true)));
                graphics2D.fillRect(0, 0, n, n3);
                final int n4 = n2;
                graphics2D.setPaint(new GradientPaint(new Point2D.Float(0.0f, 0.0f), new Color(0, true), new Point2D.Float(0.0f, (float)n4), new Color(1610612736, true)));
                graphics2D.fillRect(0, 0, n, n4);
            }
            graphics2.dispose();
        }
        graphics.drawImage(this.img, 0, 0, n * 2, n2 * 2, null);
    }
}
