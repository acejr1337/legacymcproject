package net.minecraft;

import java.awt.Color;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.Cursor;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import java.awt.GridLayout;
import java.awt.Component;
import java.awt.Font;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.JLabel;
import java.awt.LayoutManager;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Frame;
import javax.swing.JDialog;

public class OptionsPanel extends JDialog
{
    private static final long serialVersionUID = 1L;
    
    public OptionsPanel(final Frame locationRelativeTo) {
        super(locationRelativeTo);
        this.setModal(true);
        final JPanel panel = new JPanel(new BorderLayout());
        final JLabel label = new JLabel("Launcher options", 0);
        label.setBorder(new EmptyBorder(0, 0, 16, 0));
        label.setFont(new Font("Default", 1, 16));
        panel.add(label, "North");
        final JPanel panel2 = new JPanel(new BorderLayout());
        final JPanel panel3 = new JPanel(new GridLayout(0, 1));
        final JPanel panel4 = new JPanel(new GridLayout(0, 1));
        panel2.add(panel3, "West");
        panel2.add(panel4, "Center");
        final JButton button = new JButton("Force update!");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent actionEvent) {
                GameUpdater.forceUpdate = true;
                button.setText("Will force!");
                button.setEnabled(false);
            }
        });
        panel3.add(new JLabel("Force game update: ", 4));
        panel4.add(button);
        panel3.add(new JLabel("Game location on disk: ", 4));
        final TransparentLabel transparentLabel = new TransparentLabel(Util.getWorkingDirectory().toString()) {
            private static final long serialVersionUID = 0L;
            
            @Override
            public void paint(final Graphics graphics) {
                super.paint(graphics);
                int n = 0;
                final FontMetrics fontMetrics = graphics.getFontMetrics();
                final int stringWidth = fontMetrics.stringWidth(this.getText());
                final int height = fontMetrics.getHeight();
                if (this.getAlignmentX() == 2.0f) {
                    n = 0;
                }
                else if (this.getAlignmentX() == 0.0f) {
                    n = this.getBounds().width / 2 - stringWidth / 2;
                }
                else if (this.getAlignmentX() == 4.0f) {
                    n = this.getBounds().width - stringWidth;
                }
                final int n2 = this.getBounds().height / 2 + height / 2 - 1;
                graphics.drawLine(n + 2, n2, n + stringWidth - 2, n2);
            }
            
            @Override
            public void update(final Graphics graphics) {
                this.paint(graphics);
            }
        };
        transparentLabel.setCursor(Cursor.getPredefinedCursor(12));
        transparentLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent mouseEvent) {
                try {
                    Util.openLink(Util.getWorkingDirectory().toURI());
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        transparentLabel.setForeground(new Color(2105599));
        panel4.add(transparentLabel);
        panel.add(panel2, "Center");
        final JPanel panel5 = new JPanel(new BorderLayout());
        panel5.add(new JPanel(), "Center");
        final JButton button2 = new JButton("Done");
        button2.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent actionEvent) {
                OptionsPanel.this.setVisible(false);
            }
        });
        panel5.add(button2, "East");
        panel5.setBorder(new EmptyBorder(16, 0, 0, 0));
        panel.add(panel5, "South");
        this.add(panel);
        panel.setBorder(new EmptyBorder(16, 24, 24, 24));
        this.pack();
        this.setLocationRelativeTo(locationRelativeTo);
    }
}
