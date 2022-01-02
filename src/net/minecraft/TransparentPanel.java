package net.minecraft;

import java.awt.LayoutManager;
import java.awt.Insets;
import javax.swing.JPanel;

public class TransparentPanel extends JPanel
{
    private static final long serialVersionUID = 1L;
    private Insets insets;
    
    public TransparentPanel() {
    }
    
    public TransparentPanel(final LayoutManager layout) {
        this.setLayout(layout);
    }
    
    @Override
    public boolean isOpaque() {
        return false;
    }
    
    public void setInsets(final int n, final int n2, final int n3, final int n4) {
        this.insets = new Insets(n, n2, n3, n4);
    }
    
    @Override
    public Insets getInsets() {
        if (this.insets == null) {
            return super.getInsets();
        }
        return this.insets;
    }
}
