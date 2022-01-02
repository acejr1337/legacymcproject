package net.minecraft;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.border.MatteBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

public class LoginForm extends TransparentPanel
{
    private static final HyperlinkListener EXTERNAL_HYPERLINK_LISTENER;
    private static final int PANEL_SIZE = 100;
    private static final long serialVersionUID = 1L;
    private static final Color LINK_COLOR;
    public JTextField userName;
    public JPasswordField password;
    private JScrollPane scrollPane;
    private TransparentCheckbox rememberBox;
    private TransparentButton launchButton;
    private TransparentButton optionsButton;
    private TransparentButton retryButton;
    private TransparentButton offlineButton;
    private TransparentLabel errorLabel;
    private LauncherFrame launcherFrame;
    private boolean outdated;
    private boolean playOfflineAsDemo;
    
    public LoginForm(final LauncherFrame launcherFrame) {
        this.userName = new JTextField(20);
        this.password = new JPasswordField(20);
        this.rememberBox = new TransparentCheckbox("Remember password");
        this.launchButton = new TransparentButton("Login");
        this.optionsButton = new TransparentButton("Options");
        this.retryButton = new TransparentButton("Try again");
        this.offlineButton = new TransparentButton("Play offline");
        this.errorLabel = new TransparentLabel("", 0);
        this.outdated = false;
        this.playOfflineAsDemo = false;
        this.launcherFrame = launcherFrame;
        this.setLayout(new BorderLayout());
        this.add(this.buildMainLoginPanel(), "Center");
        this.readUsername();
        final ActionListener actionListener = new ActionListener() {
            public void actionPerformed(final ActionEvent actionEvent) {
                LoginForm.this.doLogin();
            }
        };
        this.userName.addActionListener(actionListener);
        this.password.addActionListener(actionListener);
        this.retryButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent actionEvent) {
                LoginForm.this.errorLabel.setText("");
                LoginForm.this.removeAll();
                LoginForm.this.add(LoginForm.this.buildMainLoginPanel(), "Center");
                LoginForm.this.validate();
            }
        });
        this.offlineButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent actionEvent) {
                launcherFrame.playCached(LoginForm.this.userName.getText(), LoginForm.this.playOfflineAsDemo);
            }
        });
        this.launchButton.addActionListener(actionListener);
        this.optionsButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent actionEvent) {
                new OptionsPanel(launcherFrame).setVisible(true);
            }
        });
    }
    
    public void doLogin() {
        this.setLoggingIn();
        new Thread() {
            @Override
            public void run() {
                try {
                    LoginForm.this.launcherFrame.login(LoginForm.this.userName.getText(), new String(LoginForm.this.password.getPassword()));
                }
                catch (Exception ex) {
                    LoginForm.this.setError(ex.toString());
                }
            }
        }.start();
    }
    
    private void readUsername() {
        try {
            final File file = new File(Util.getWorkingDirectory(), "lastlogin");
            final Cipher cipher = this.getCipher(2, "passwordfile");
            DataInputStream dataInputStream;
            if (cipher != null) {
                dataInputStream = new DataInputStream(new CipherInputStream(new FileInputStream(file), cipher));
            }
            else {
                dataInputStream = new DataInputStream(new FileInputStream(file));
            }
            this.userName.setText(dataInputStream.readUTF());
            this.password.setText(dataInputStream.readUTF());
            this.rememberBox.setSelected(this.password.getPassword().length > 0);
            dataInputStream.close();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    private void writeUsername() {
        try {
            final File file = new File(Util.getWorkingDirectory(), "lastlogin");
            final Cipher cipher = this.getCipher(1, "passwordfile");
            DataOutputStream dataOutputStream;
            if (cipher != null) {
                dataOutputStream = new DataOutputStream(new CipherOutputStream(new FileOutputStream(file), cipher));
            }
            else {
                dataOutputStream = new DataOutputStream(new FileOutputStream(file));
            }
            dataOutputStream.writeUTF(this.userName.getText());
            dataOutputStream.writeUTF(this.rememberBox.isSelected() ? new String(this.password.getPassword()) : "");
            dataOutputStream.close();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    private Cipher getCipher(final int n, final String s) throws Exception {
        final Random random = new Random(43287234L);
        final byte[] array = new byte[8];
        random.nextBytes(array);
        final PBEParameterSpec pbeParameterSpec = new PBEParameterSpec(array, 5);
        final SecretKey generateSecret = SecretKeyFactory.getInstance("PBEWithMD5AndDES").generateSecret(new PBEKeySpec(s.toCharArray()));
        final Cipher instance = Cipher.getInstance("PBEWithMD5AndDES");
        instance.init(n, generateSecret, pbeParameterSpec);
        return instance;
    }
    
    private JScrollPane getUpdateNews() {
        if (this.scrollPane != null) {
            return this.scrollPane;
        }
        try {
            final JTextPane textPane = new JTextPane() {
                private static final long serialVersionUID = 1L;
            };
            textPane.setEditable(false);
            textPane.setMargin(null);
            textPane.setBackground(Color.DARK_GRAY);
            textPane.setContentType("text/html");
            textPane.setText("<html><body><font color=\"#808080\"><br><br><br><br><br><br><br><center><h1>Loading update news..</h1></center></font></body></html>");
            textPane.addHyperlinkListener(LoginForm.EXTERNAL_HYPERLINK_LISTENER);
            new Thread() {
                @Override
                public void run() {
                    try {
                        textPane.setPage("http://mcupdate.tumblr.com/");
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                        textPane.setText("<html><body><font color=\"#808080\"><br><br><br><br><br><br><br><center><h1>Failed to update news</h1><br>" + ex.toString() + "</center></font></body></html>");
                    }
                }
            }.start();
            (this.scrollPane = new JScrollPane(textPane)).setBorder(null);
            this.scrollPane.setBorder(new MatteBorder(0, 0, 2, 0, Color.BLACK));
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return this.scrollPane;
    }
    
    private JPanel buildMainLoginPanel() {
        final TransparentPanel transparentPanel = new TransparentPanel(new BorderLayout());
        transparentPanel.add(this.getUpdateNews(), "Center");
        final TexturedPanel texturedPanel = new TexturedPanel();
        texturedPanel.setLayout(new BorderLayout());
        texturedPanel.add(new LogoPanel(), "West");
        texturedPanel.add(new TransparentPanel(), "Center");
        texturedPanel.add(this.center(this.buildLoginPanel()), "East");
        texturedPanel.setPreferredSize(new Dimension(100, 100));
        transparentPanel.add(texturedPanel, "South");
        return transparentPanel;
    }
    
    private JPanel buildLoginPanel() {
        final TransparentPanel transparentPanel = new TransparentPanel();
        transparentPanel.setInsets(4, 0, 4, 0);
        final BorderLayout layout = new BorderLayout();
        layout.setHgap(0);
        layout.setVgap(8);
        transparentPanel.setLayout(layout);
        final GridLayout gridLayout = new GridLayout(0, 1);
        gridLayout.setVgap(2);
        final GridLayout gridLayout2 = new GridLayout(0, 1);
        gridLayout2.setVgap(2);
        final GridLayout gridLayout3 = new GridLayout(0, 1);
        gridLayout3.setVgap(2);
        final TransparentPanel transparentPanel2 = new TransparentPanel(gridLayout);
        final TransparentPanel transparentPanel3 = new TransparentPanel(gridLayout2);
        transparentPanel2.add(new TransparentLabel("Username/Email:", 4));
        transparentPanel2.add(new TransparentLabel("Password:", 4));
        transparentPanel2.add(new TransparentLabel("", 4));
        transparentPanel3.add(this.userName);
        transparentPanel3.add(this.password);
        transparentPanel3.add(this.rememberBox);
        transparentPanel.add(transparentPanel2, "West");
        transparentPanel.add(transparentPanel3, "Center");
        final TransparentPanel transparentPanel4 = new TransparentPanel(new BorderLayout());
        final TransparentPanel transparentPanel5 = new TransparentPanel(gridLayout3);
        transparentPanel2.setInsets(0, 0, 0, 4);
        transparentPanel5.setInsets(0, 10, 0, 10);
        transparentPanel5.add(this.optionsButton);
        transparentPanel5.add(this.launchButton);
        try {
            if (this.outdated) {
                transparentPanel5.add(this.getUpdateLink());
            }
            else {
                final TransparentLabel transparentLabel = new TransparentLabel("Need account?") {
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
                            Util.openLink(new URL("http://www.minecraft.net/register.jsp").toURI());
                        }
                        catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
                transparentLabel.setForeground(LoginForm.LINK_COLOR);
                transparentPanel5.add(transparentLabel);
            }
        }
        catch (Error error) {}
        transparentPanel4.add(transparentPanel5, "Center");
        transparentPanel.add(transparentPanel4, "East");
        this.errorLabel.setFont(new Font(null, 2, 16));
        this.errorLabel.setForeground(new Color(16728128));
        this.errorLabel.setText("");
        transparentPanel.add(this.errorLabel, "North");
        return transparentPanel;
    }
    
    private TransparentLabel getUpdateLink() {
        final TransparentLabel transparentLabel = new TransparentLabel("You need to update the launcher!") {
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
                    Util.openLink(new URL("http://www.minecraft.net/download.jsp").toURI());
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        transparentLabel.setForeground(LoginForm.LINK_COLOR);
        return transparentLabel;
    }
    
    private JPanel buildMainOfflinePanel(final boolean b) {
        final TransparentPanel transparentPanel = new TransparentPanel(new BorderLayout());
        transparentPanel.add(this.getUpdateNews(), "Center");
        final TexturedPanel texturedPanel = new TexturedPanel();
        texturedPanel.setLayout(new BorderLayout());
        texturedPanel.add(new LogoPanel(), "West");
        texturedPanel.add(new TransparentPanel(), "Center");
        texturedPanel.add(this.center(this.buildOfflinePanel(b)), "East");
        texturedPanel.setPreferredSize(new Dimension(100, 100));
        transparentPanel.add(texturedPanel, "South");
        return transparentPanel;
    }
    
    private Component center(final Component component) {
        final TransparentPanel transparentPanel = new TransparentPanel(new GridBagLayout());
        transparentPanel.add(component);
        return transparentPanel;
    }
    
    private TransparentPanel buildOfflinePanel(final boolean b) {
        final TransparentPanel transparentPanel = new TransparentPanel();
        transparentPanel.setInsets(0, 0, 0, 20);
        transparentPanel.setLayout(new BorderLayout());
        final TransparentPanel transparentPanel2 = new TransparentPanel(new BorderLayout());
        final GridLayout gridLayout = new GridLayout(0, 1);
        gridLayout.setVgap(2);
        final TransparentPanel transparentPanel3 = new TransparentPanel(gridLayout);
        transparentPanel3.setInsets(0, 8, 0, 0);
        if (b) {
            this.offlineButton.setText("Play Demo");
        }
        else {
            this.offlineButton.setText("Play Offline");
        }
        transparentPanel3.add(this.retryButton);
        transparentPanel3.add(this.offlineButton);
        transparentPanel2.add(transparentPanel3, "East");
        final boolean enabled = this.launcherFrame.canPlayOffline(this.userName.getText()) || b;
        this.offlineButton.setEnabled(enabled);
        if (!enabled) {
            transparentPanel2.add(new TransparentLabel("(Not downloaded)", 4), "South");
        }
        transparentPanel.add(transparentPanel2, "Center");
        final TransparentPanel transparentPanel4 = new TransparentPanel(new GridLayout(0, 1));
        this.errorLabel.setFont(new Font(null, 2, 16));
        this.errorLabel.setForeground(new Color(16728128));
        transparentPanel4.add(this.errorLabel);
        if (this.outdated) {
            transparentPanel4.add(this.getUpdateLink());
        }
        transparentPanel2.add(transparentPanel4, "Center");
        return transparentPanel;
    }
    
    public void setError(final String text) {
        this.removeAll();
        this.add(this.buildMainLoginPanel(), "Center");
        this.errorLabel.setText(text);
        this.validate();
    }
    
    public void loginOk() {
        this.writeUsername();
    }
    
    public void setLoggingIn() {
        this.removeAll();
        final JPanel panel = new JPanel(new BorderLayout());
        panel.add(this.getUpdateNews(), "Center");
        final TexturedPanel texturedPanel = new TexturedPanel();
        texturedPanel.setLayout(new BorderLayout());
        texturedPanel.add(new LogoPanel(), "West");
        texturedPanel.add(new TransparentPanel(), "Center");
        final TransparentLabel transparentLabel = new TransparentLabel("Logging in...                      ", 0);
        transparentLabel.setFont(new Font(null, 1, 16));
        texturedPanel.add(this.center(transparentLabel), "East");
        texturedPanel.setPreferredSize(new Dimension(100, 100));
        panel.add(texturedPanel, "South");
        this.add(panel, "Center");
        this.validate();
    }
    
    public void setNoNetwork(final boolean playOfflineAsDemo) {
        this.playOfflineAsDemo = playOfflineAsDemo;
        this.removeAll();
        this.add(this.buildMainOfflinePanel(playOfflineAsDemo), "Center");
        this.validate();
    }
    
    public void setOutdated() {
        this.outdated = true;
    }
    
    static {
        EXTERNAL_HYPERLINK_LISTENER = new HyperlinkListener() {
            public void hyperlinkUpdate(final HyperlinkEvent hyperlinkEvent) {
                if (hyperlinkEvent.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    try {
                        Util.openLink(hyperlinkEvent.getURL().toURI());
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        };
        LINK_COLOR = new Color(8421631);
    }
}
