package net.minecraft;

import javax.swing.JButton;

public class TransparentButton extends JButton
{
    private static final long serialVersionUID = 1L;
    
    public TransparentButton(final String s) {
        super(s);
    }
    
    @Override
    public boolean isOpaque() {
        return false;
    }
}
