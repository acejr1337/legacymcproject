package net.minecraft;

import java.net.URI;
import java.security.cert.Certificate;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import java.util.Iterator;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.io.File;

public class Util
{
    private static File workDir;
    
    public static OS getPlatform() {
        final String lowerCase = System.getProperty("os.name").toLowerCase();
        if (lowerCase.contains("win")) {
            return OS.WINDOWS;
        }
        if (lowerCase.contains("mac")) {
            return OS.MACOS;
        }
        if (lowerCase.contains("solaris")) {
            return OS.SOLARIS;
        }
        if (lowerCase.contains("sunos")) {
            return OS.SOLARIS;
        }
        if (lowerCase.contains("linux")) {
            return OS.LINUX;
        }
        if (lowerCase.contains("unix")) {
            return OS.LINUX;
        }
        return OS.UNKNOWN;
    }
    
    public static File getWorkingDirectory() {
        if (Util.workDir == null) {
            Util.workDir = getWorkingDirectory("minecraft");
        }
        return Util.workDir;
    }
    
    public static File getWorkingDirectory(final String s) {
        final String property = System.getProperty("user.home", ".");
        File file = null;
        switch (getPlatform()) {
            case LINUX:
            case SOLARIS: {
                file = new File(property, '.' + s + '/');
                break;
            }
            case WINDOWS: {
                final String getenv = System.getenv("APPDATA");
                file = new File((getenv != null) ? getenv : property, '.' + s + '/');
                break;
            }
            case MACOS: {
                file = new File(property, "Library/Application Support/" + s);
                break;
            }
            default: {
                file = new File(property, s + '/');
                break;
            }
        }
        if (!file.exists() && !file.mkdirs()) {
            throw new RuntimeException("The working directory could not be created: " + file);
        }
        return file;
    }
    
    public static String buildQuery(final Map<String, Object> map) {
        final StringBuilder sb = new StringBuilder();
        for (final Map.Entry entry : map.entrySet()) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            try {
                sb.append(URLEncoder.encode((String)entry.getKey(), "UTF-8"));
            }
            catch (UnsupportedEncodingException ex) {
                ex.printStackTrace();
            }
            if (entry.getValue() != null) {
                sb.append('=');
                try {
                    sb.append(URLEncoder.encode(entry.getValue().toString(), "UTF-8"));
                }
                catch (UnsupportedEncodingException ex2) {
                    ex2.printStackTrace();
                }
            }
        }
        return sb.toString();
    }
    
    public static String executePost(final String s, final Map<String, Object> map) {
        return executePost(s, buildQuery(map));
    }
    
    public static String executePost(final String s, final String s2) {
        HttpsURLConnection httpsURLConnection = null;
        try {
            httpsURLConnection = (HttpsURLConnection)new URL(s).openConnection();
            httpsURLConnection.setRequestMethod("POST");
            httpsURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            httpsURLConnection.setRequestProperty("Content-Length", "" + Integer.toString(s2.getBytes().length));
            httpsURLConnection.setRequestProperty("Content-Language", "en-US");
            httpsURLConnection.setUseCaches(false);
            httpsURLConnection.setDoInput(true);
            httpsURLConnection.setDoOutput(true);
            httpsURLConnection.connect();
            final Certificate[] serverCertificates = httpsURLConnection.getServerCertificates();
            final byte[] array = new byte[294];
            final DataInputStream dataInputStream = new DataInputStream(Util.class.getResourceAsStream("minecraft.key"));
            dataInputStream.readFully(array);
            dataInputStream.close();
            final byte[] encoded = serverCertificates[0].getPublicKey().getEncoded();
            for (int i = 0; i < encoded.length; ++i) {
                if (encoded[i] != array[i]) {
                    throw new RuntimeException("Public key mismatch");
                }
            }
            final DataOutputStream dataOutputStream = new DataOutputStream(httpsURLConnection.getOutputStream());
            dataOutputStream.writeBytes(s2);
            dataOutputStream.flush();
            dataOutputStream.close();
            final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpsURLConnection.getInputStream()));
            final StringBuffer sb = new StringBuffer();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
                sb.append('\r');
            }
            bufferedReader.close();
            return sb.toString();
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
        finally {
            if (httpsURLConnection != null) {
                httpsURLConnection.disconnect();
            }
        }
    }
    
    public static boolean isEmpty(final String s) {
        return s == null || s.length() == 0;
    }
    
    public static void openLink(final URI uri) {
        try {
            final Object invoke = Class.forName("java.awt.Desktop").getMethod("getDesktop", (Class<?>[])new Class[0]).invoke(null, new Object[0]);
            invoke.getClass().getMethod("browse", URI.class).invoke(invoke, uri);
        }
        catch (Throwable t) {
            System.out.println("Failed to open link " + uri.toString());
        }
    }
    
    static {
        Util.workDir = null;
    }
    
    public enum OS
    {
        LINUX, 
        SOLARIS, 
        WINDOWS, 
        MACOS, 
        UNKNOWN;
    }
}
