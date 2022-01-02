package net.minecraft;

import java.awt.Color;
import javax.swing.JCheckBox;

public class TransparentCheckbox extends JCheckBox
{
    private static final long serialVersionUID = 1L;
    
    public TransparentCheckbox(final String s) {
        super(s);
        this.setForeground(Color.WHITE);
    }
    
    @Override
    public boolean isOpaque() {
        return false;
    }
}
