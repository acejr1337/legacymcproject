package net.minecraft;

import java.awt.image.ImageObserver;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.awt.Dimension;
import javax.imageio.ImageIO;
import java.awt.Image;
import javax.swing.JPanel;

public class LogoPanel extends JPanel
{
    private static final long serialVersionUID = 1L;
    private Image bgImage;
    
    public LogoPanel() {
        this.setOpaque(true);
        try {
            final BufferedImage read = ImageIO.read(LoginForm.class.getResource("logo.png"));
            final int width = read.getWidth();
            final int height = read.getHeight();
            this.bgImage = read.getScaledInstance(width, height, 16);
            this.setPreferredSize(new Dimension(width + 32, height + 32));
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
        graphics.drawImage(this.bgImage, 24, 24, null);
    }
}
