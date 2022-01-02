package net.minecraft;

import java.awt.Color;
import javax.swing.JLabel;

public class TransparentLabel extends JLabel
{
    private static final long serialVersionUID = 1L;
    
    public TransparentLabel(final String s, final int n) {
        super(s, n);
        this.setForeground(Color.WHITE);
    }
    
    public TransparentLabel(final String s) {
        super(s);
        this.setForeground(Color.WHITE);
    }
    
    @Override
    public boolean isOpaque() {
        return false;
    }
}
