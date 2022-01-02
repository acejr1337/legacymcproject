package net.minecraft;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.UIManager;

import fr.litarvan.openauth.AuthPoints;
import fr.litarvan.openauth.Authenticator;
import fr.litarvan.openauth.microsoft.MicrosoftAuthResult;
import fr.litarvan.openauth.microsoft.MicrosoftAuthenticator;
import fr.litarvan.openauth.model.AuthAgent;
import fr.litarvan.openauth.model.response.AuthResponse;

public class LauncherFrame extends Frame
{
    public static final int VERSION = 13;
    private static final long serialVersionUID = 1L;
    public Map<String, String> customParameters;
    public Launcher launcher;
    public LoginForm loginForm;
    
    public LauncherFrame() {
        super("Minecraft Launcher");
        this.customParameters = new HashMap<String, String>();
        this.setBackground(Color.BLACK);
        this.loginForm = new LoginForm(this);
        final JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(this.loginForm, "Center");
        panel.setPreferredSize(new Dimension(854, 480));
        this.setLayout(new BorderLayout());
        this.add(panel, "Center");
        this.pack();
        this.setLocationRelativeTo(null);
        try {
            this.setIconImage(ImageIO.read(LauncherFrame.class.getResource("favicon.png")));
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent windowEvent) {
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(30000L);
                        }
                        catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                        System.out.println("FORCING EXIT!");
                        System.exit(0);
                    }
                }.start();
                if (LauncherFrame.this.launcher != null) {
                    LauncherFrame.this.launcher.stop();
                    LauncherFrame.this.launcher.destroy();
                }
                System.exit(0);
            }
        });
    }
    
    public void playCached(String s, final boolean b) {
        try {
            if (s == null || s.length() <= 0) {
                s = "Player";
            }
            this.launcher = new Launcher();
            this.launcher.customParameters.putAll(this.customParameters);
            this.launcher.customParameters.put("userName", s);
            this.launcher.customParameters.put("demo", "" + b);
            this.launcher.customParameters.put("sessionId", "1");
            this.launcher.init();
            this.removeAll();
            this.add(this.launcher, "Center");
            this.validate();
            this.launcher.start();
            this.loginForm = null;
            this.setTitle("Minecraft");
        }
        catch (Exception ex) {
            ex.printStackTrace();
            this.showError(ex.toString());
        }
    }
    
    // user // pass
    public void login(final String s, final String s2) {
        try {
        	// try with microsoft
        	MicrosoftAuthenticator authenticator = new MicrosoftAuthenticator();
        	MicrosoftAuthResult result = authenticator.loginWithCredentials(s, s2);
            if (result == null) {
                this.showError("Can't connect to minecraft.net");
                this.loginForm.setNoNetwork(false);
                return;
            }
            this.launcher = new Launcher();
            this.launcher.customParameters.putAll(this.customParameters);
            this.launcher.customParameters.put("userName", result.getProfile().getName());
            this.launcher.customParameters.put("sessionId", result.getAccessToken());

            this.launcher.init();
            this.removeAll();
            this.add(this.launcher, "Center");
            this.validate();
            this.launcher.start();
            this.loginForm.loginOk();
            this.loginForm = null;
            this.setTitle("Minecraft");
        }
        catch (Exception ex) {
        	// ok failed so now try with mojang.
            try {
            	
            	Authenticator authenticator = new Authenticator(Authenticator.MOJANG_AUTH_URL, AuthPoints.NORMAL_AUTH_POINTS);
            	AuthResponse result = authenticator.authenticate(AuthAgent.MINECRAFT, s, s2, null);
            	if (result == null) {
                    this.showError("Can't connect to minecraft.net");
                    this.loginForm.setNoNetwork(false);
                    return;
                }
                this.launcher = new Launcher();
                this.launcher.customParameters.putAll(this.customParameters);
                this.launcher.customParameters.put("userName", result.getSelectedProfile().getName());
                this.launcher.customParameters.put("sessionId", result.getAccessToken());

                this.launcher.init();
                this.removeAll();
                this.add(this.launcher, "Center");
                this.validate();
                this.launcher.start();
                this.loginForm.loginOk();
                this.loginForm = null;
                this.setTitle("Minecraft");
            } catch(Exception ex2) {
            	ex.printStackTrace();
                this.showError("Failed to login.");
                this.loginForm.setNoNetwork(false);
            }
        }
    }
    
    private void showError(final String error) {
        this.removeAll();
        this.add(this.loginForm);
        this.loginForm.setError(error);
        this.validate();
    }
    
    public boolean canPlayOffline(final String s) {
        final Launcher launcher = new Launcher();
        launcher.customParameters.putAll(this.customParameters);
        launcher.init(s, null, null, "1");
        return launcher.canPlayOffline();
    }
    
    public static void main(final String[] array) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception ex) {}
        System.out.println("asdf");
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.net.preferIPv6Addresses", "false");
        final LauncherFrame launcherFrame = new LauncherFrame();
        launcherFrame.setVisible(true);
        launcherFrame.customParameters.put("stand-alone", "true");
        for (final String s : array) {
            if (s.startsWith("-u=") || s.startsWith("--user=")) {
                final String argValue = getArgValue(s);
                launcherFrame.customParameters.put("username", argValue);
                launcherFrame.loginForm.userName.setText(argValue);
            }
            else if (s.startsWith("-p=") || s.startsWith("--password=")) {
                final String argValue2 = getArgValue(s);
                launcherFrame.customParameters.put("password", argValue2);
                launcherFrame.loginForm.password.setText(argValue2);
            }
            else if (s.startsWith("--noupdate")) {
                launcherFrame.customParameters.put("noupdate", "true");
            }
        }
        if (array.length >= 3) {
            String s2 = array[2];
            String s3 = "25565";
            if (s2.contains(":")) {
                final String[] split = s2.split(":");
                s2 = split[0];
                s3 = split[1];
            }
            launcherFrame.customParameters.put("server", s2);
            launcherFrame.customParameters.put("port", s3);
        }
    }
    
    private static String getArgValue(final String s) {
        final int index = s.indexOf(61);
        if (index < 0) {
            return "";
        }
        return s.substring(index + 1);
    }
}
