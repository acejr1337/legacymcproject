package net.minecraft;

import java.applet.Applet;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.JarURLConnection;
import java.net.SocketPermission;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.MessageDigest;
import java.security.PermissionCollection;
import java.security.PrivilegedExceptionAction;
import java.security.SecureClassLoader;
import java.security.cert.Certificate;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;

public class GameUpdater implements Runnable
{
    public static final int STATE_INIT = 1;
    public static final int STATE_DETERMINING_PACKAGES = 2;
    public static final int STATE_CHECKING_CACHE = 3;
    public static final int STATE_DOWNLOADING = 4;
    public static final int STATE_EXTRACTING_PACKAGES = 5;
    public static final int STATE_UPDATING_CLASSPATH = 6;
    public static final int STATE_SWITCHING_APPLET = 7;
    public static final int STATE_INITIALIZE_REAL_APPLET = 8;
    public static final int STATE_START_REAL_APPLET = 9;
    public static final int STATE_DONE = 10;
    public int percentage;
    public int currentSizeDownload;
    public int totalSizeDownload;
    public int currentSizeExtract;
    public int totalSizeExtract;
    protected URL[] urlList;
    private static ClassLoader classLoader;
    protected Thread loaderThread;
    public boolean fatalError;
    public String fatalErrorDescription;
    protected String subtaskMessage;
    protected int state;
    protected boolean lzmaSupported;
    protected boolean pack200Supported;
    protected boolean certificateRefused;
    protected static boolean natives_loaded;
    public static boolean forceUpdate;
    private String latestVersion;
    private String mainGameUrl;
    public boolean pauseAskUpdate;
    public boolean shouldUpdate;
    public boolean skipUpdate;
    
    public GameUpdater(final String latestVersion, final String mainGameUrl, final boolean skipUpdate) {
        this.subtaskMessage = "";
        this.state = 1;
        this.lzmaSupported = false;
        this.pack200Supported = false;
        this.latestVersion = latestVersion;
        this.mainGameUrl = mainGameUrl;
        this.skipUpdate = skipUpdate;
    }
    
    public void init() {
        this.state = 1;
        try {
            Class.forName("LZMA.LzmaInputStream");
            this.lzmaSupported = true;
        }
        catch (ClassNotFoundException ex) {}
        try {
            Pack200.class.getSimpleName();
            this.pack200Supported = true;
        }
        catch (Throwable t) {}
    }
    
    private String generateStacktrace(final Exception ex) {
        final StringWriter stringWriter = new StringWriter();
        ex.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }
    
    protected String getDescriptionForState() {
        switch (this.state) {
            case 1: {
                return "Initializing loader";
            }
            case 2: {
                return "Determining packages to load";
            }
            case 3: {
                return "Checking cache for existing files";
            }
            case 4: {
                return "Downloading packages";
            }
            case 5: {
                return "Extracting downloaded packages";
            }
            case 6: {
                return "Updating classpath";
            }
            case 7: {
                return "Switching applet";
            }
            case 8: {
                return "Initializing real applet";
            }
            case 9: {
                return "Starting real applet";
            }
            case 10: {
                return "Done loading";
            }
            default: {
                return "unknown state";
            }
        }
    }
    
    protected String trimExtensionByCapabilities(String s) {
        if (!this.pack200Supported) {
            s = s.replaceAll(".pack", "");
        }
        if (!this.lzmaSupported) {
            s = s.replaceAll(".lzma", "");
        }
        return s;
    }
    
    protected void loadJarURLs() throws Exception {
        this.state = 2;
        final StringTokenizer stringTokenizer = new StringTokenizer(this.trimExtensionByCapabilities("lwjgl.jar, jinput.jar, lwjgl_util.jar, " + this.mainGameUrl), ", ");
        final int n = stringTokenizer.countTokens() + 1;
        this.urlList = new URL[n];
        final URL url = new URL("http://s3.amazonaws.com/MinecraftDownload/");
        for (int i = 0; i < n - 1; ++i) {
            this.urlList[i] = new URL(url, stringTokenizer.nextToken());
        }
        final String property = System.getProperty("os.name");
        String s = null;
        if (property.startsWith("Win")) {
            s = "windows_natives.jar.lzma";
        }
        else if (property.startsWith("Linux")) {
            s = "linux_natives.jar.lzma";
        }
        else if (property.startsWith("Mac")) {
            s = "macosx_natives.jar.lzma";
        }
        else if (property.startsWith("Solaris") || property.startsWith("SunOS")) {
            s = "solaris_natives.jar.lzma";
        }
        else {
            this.fatalErrorOccured("OS (" + property + ") not supported", null);
        }
        if (s == null) {
            this.fatalErrorOccured("no lwjgl natives files found", null);
        }
        else {
            this.urlList[n - 1] = new URL(url, this.trimExtensionByCapabilities(s));
        }
    }
    
    public void run() {
        this.init();
        this.state = 3;
        this.percentage = 5;
        try {
            this.loadJarURLs();
            final String s = AccessController.doPrivileged((PrivilegedExceptionAction<String>)new PrivilegedExceptionAction() {
                public Object run() throws Exception {
                    return Util.getWorkingDirectory() + File.separator + "bin" + File.separator;
                }
            });
            final File file = new File(s);
            if (!file.exists()) {
                file.mkdirs();
            }
            if (this.latestVersion != null) {
                final File file2 = new File(file, "version");
                boolean b = false;
                if (!this.skipUpdate && !GameUpdater.forceUpdate && file2.exists() && (this.latestVersion.equals("-1") || this.latestVersion.equals(this.readVersionFile(file2)))) {
                    b = true;
                    this.percentage = 90;
                }
                if (!this.skipUpdate && (GameUpdater.forceUpdate || !b)) {
                    this.shouldUpdate = true;
                    if (!GameUpdater.forceUpdate && file2.exists()) {
                        this.checkShouldUpdate();
                    }
                    if (this.shouldUpdate) {
                        this.writeVersionFile(file2, "");
                        this.downloadJars(s);
                        this.extractJars(s);
                        this.extractNatives(s);
                        if (this.latestVersion != null) {
                            this.percentage = 90;
                            this.writeVersionFile(file2, this.latestVersion);
                        }
                    }
                    else {
                        this.percentage = 90;
                    }
                }
            }
            this.updateClassPath(file);
            this.state = 10;
        }
        catch (AccessControlException ex) {
            this.fatalErrorOccured(ex.getMessage(), ex);
            this.certificateRefused = true;
        }
        catch (Exception ex2) {
            this.fatalErrorOccured(ex2.getMessage(), ex2);
        }
        finally {
            this.loaderThread = null;
        }
    }
    
    private void checkShouldUpdate() {
        this.pauseAskUpdate = true;
        while (this.pauseAskUpdate) {
            try {
                Thread.sleep(1000L);
            }
            catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    protected String readVersionFile(final File file) throws Exception {
        final DataInputStream dataInputStream = new DataInputStream(new FileInputStream(file));
        final String utf = dataInputStream.readUTF();
        dataInputStream.close();
        return utf;
    }
    
    protected void writeVersionFile(final File file, final String s) throws Exception {
        final DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(file));
        dataOutputStream.writeUTF(s);
        dataOutputStream.close();
    }
    
    protected void updateClassPath(final File file) throws Exception {
        this.state = 6;
        this.percentage = 95;
        final URL[] array = new URL[this.urlList.length];
        for (int i = 0; i < this.urlList.length; ++i) {
            array[i] = new File(file, this.getJarName(this.urlList[i])).toURI().toURL();
        }
        if (GameUpdater.classLoader == null) {
            GameUpdater.classLoader = new URLClassLoader(array) {
                @Override
                protected PermissionCollection getPermissions(final CodeSource codeSource) {
                    PermissionCollection collection = null;
                    try {
                        final Method declaredMethod = SecureClassLoader.class.getDeclaredMethod("getPermissions", CodeSource.class);
                        declaredMethod.setAccessible(true);
                        collection = (PermissionCollection)declaredMethod.invoke(this.getClass().getClassLoader(), codeSource);
                        collection.add(new SocketPermission("www.minecraft.net", "connect,accept"));
                        collection.add(new FilePermission("<<ALL FILES>>", "read"));
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    return collection;
                }
            };
        }
        String s = file.getAbsolutePath();
        if (!s.endsWith(File.separator)) {
            s += File.separator;
        }
        this.unloadNatives(s);
        System.setProperty("org.lwjgl.librarypath", s + "natives");
        System.setProperty("net.java.games.input.librarypath", s + "natives");
        GameUpdater.natives_loaded = true;
    }
    
    private void unloadNatives(final String s) {
        if (!GameUpdater.natives_loaded) {
            return;
        }
        try {
            final Field declaredField = ClassLoader.class.getDeclaredField("loadedLibraryNames");
            declaredField.setAccessible(true);
            final Vector vector = (Vector)declaredField.get(this.getClass().getClassLoader());
            final String canonicalPath = new File(s).getCanonicalPath();
            for (int i = 0; i < vector.size(); ++i) {
                if (((String)vector.get(i)).startsWith(canonicalPath)) {
                    vector.remove(i);
                    --i;
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public Applet createApplet() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        return (Applet)GameUpdater.classLoader.loadClass("net.minecraft.client.MinecraftApplet").newInstance();
    }
    
    protected void downloadJars(final String s) throws Exception {
        final File file = new File(s, "md5s");
        final Properties properties = new Properties();
        if (file.exists()) {
            try {
                final FileInputStream fileInputStream = new FileInputStream(file);
                properties.load(fileInputStream);
                fileInputStream.close();
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        this.state = 4;
        final int[] array = new int[this.urlList.length];
        final boolean[] array2 = new boolean[this.urlList.length];
        for (int i = 0; i < this.urlList.length; ++i) {
            final URLConnection openConnection = this.urlList[i].openConnection();
            openConnection.setDefaultUseCaches(false);
            array2[i] = false;
            if (openConnection instanceof HttpURLConnection) {
                ((HttpURLConnection)openConnection).setRequestMethod("HEAD");
                final String string = "\"" + properties.getProperty(this.getFileName(this.urlList[i])) + "\"";
                if (!GameUpdater.forceUpdate && string != null) {
                    openConnection.setRequestProperty("If-None-Match", string);
                }
                if (((HttpURLConnection)openConnection).getResponseCode() / 100 == 3) {
                    array2[i] = true;
                }
            }
            array[i] = openConnection.getContentLength();
            this.totalSizeDownload += array[i];
        }
        final int percentage = 10;
        this.percentage = percentage;
        final int n = percentage;
        final byte[] array3 = new byte[65536];
        for (int j = 0; j < this.urlList.length; ++j) {
            if (array2[j]) {
                this.percentage = n + array[j] * 45 / this.totalSizeDownload;
            }
            else {
                try {
                    properties.remove(this.getFileName(this.urlList[j]));
                    properties.store(new FileOutputStream(file), "md5 hashes for downloaded files");
                }
                catch (Exception ex2) {
                    ex2.printStackTrace();
                }
                int n2 = 0;
                final int n3 = 3;
                int k = 1;
                while (k != 0) {
                    k = 0;
                    final URLConnection openConnection2 = this.urlList[j].openConnection();
                    String substring = "";
                    if (openConnection2 instanceof HttpURLConnection) {
                        openConnection2.setRequestProperty("Cache-Control", "no-cache");
                        openConnection2.connect();
                        final String headerField = openConnection2.getHeaderField("ETag");
                        substring = headerField.substring(1, headerField.length() - 1);
                    }
                    final String fileName = this.getFileName(this.urlList[j]);
                    final InputStream jarInputStream = this.getJarInputStream(fileName, openConnection2);
                    final FileOutputStream fileOutputStream = new FileOutputStream(s + fileName);
                    long currentTimeMillis = System.currentTimeMillis();
                    int n4 = 0;
                    int n5 = 0;
                    String string2 = "";
                    final MessageDigest instance = MessageDigest.getInstance("MD5");
                    int read;
                    while ((read = jarInputStream.read(array3, 0, array3.length)) != -1) {
                        fileOutputStream.write(array3, 0, read);
                        instance.update(array3, 0, read);
                        this.currentSizeDownload += read;
                        n5 += read;
                        this.percentage = n + this.currentSizeDownload * 45 / this.totalSizeDownload;
                        this.subtaskMessage = "Retrieving: " + fileName + " " + this.currentSizeDownload * 100 / this.totalSizeDownload + "%";
                        n4 += read;
                        final long n6 = System.currentTimeMillis() - currentTimeMillis;
                        if (n6 >= 1000L) {
                            string2 = " @ " + (int)(n4 / (float)n6 * 100.0f) / 100.0f + " KB/sec";
                            n4 = 0;
                            currentTimeMillis += 1000L;
                        }
                        this.subtaskMessage += string2;
                    }
                    jarInputStream.close();
                    fileOutputStream.close();
                    String s2;
                    for (s2 = new BigInteger(1, instance.digest()).toString(16); s2.length() < 32; s2 = "0" + s2) {}
                    boolean equals = true;
                    if (substring != null) {
                        equals = s2.equals(substring);
                    }
                    if (openConnection2 instanceof HttpURLConnection) {
                        Label_0895: {
                            if (equals) {
                                if (n5 != array[j]) {
                                    if (array[j] > 0) {
                                        break Label_0895;
                                    }
                                }
                                try {
                                    properties.setProperty(this.getFileName(this.urlList[j]), substring);
                                    properties.store(new FileOutputStream(file), "md5 hashes for downloaded files");
                                }
                                catch (Exception ex3) {
                                    ex3.printStackTrace();
                                }
                                continue;
                            }
                        }
                        if (++n2 >= n3) {
                            throw new Exception("failed to download " + fileName);
                        }
                        k = 1;
                        this.currentSizeDownload -= n5;
                    }
                }
            }
        }
        this.subtaskMessage = "";
    }
    
    protected InputStream getJarInputStream(final String s, final URLConnection urlConnection) throws Exception {
        final InputStream[] array = { null };
        for (int n = 0; n < 3 && array[0] == null; ++n) {
            final Thread thread = new Thread() {
                @Override
                public void run() {
                    try {
                        array[0] = urlConnection.getInputStream();
                    }
                    catch (IOException ex) {}
                }
            };
            thread.setName("JarInputStreamThread");
            thread.start();
            int n2 = 0;
            while (array[0] == null && n2++ < 5) {
                try {
                    thread.join(1000L);
                }
                catch (InterruptedException ex) {}
            }
            if (array[0] == null) {
                try {
                    thread.interrupt();
                    thread.join();
                }
                catch (InterruptedException ex2) {}
            }
        }
        if (array[0] == null) {
            throw new Exception("Unable to download " + s);
        }
        return array[0];
    }
    
    protected void extractLZMA(final String s, final String s2) throws Exception {
        final File file = new File(s);
        if (!file.exists()) {
            return;
        }
        final InputStream inputStream = (InputStream)Class.forName("LZMA.LzmaInputStream").getDeclaredConstructor(InputStream.class).newInstance(new FileInputStream(file));
        final FileOutputStream fileOutputStream = new FileOutputStream(s2);
        final byte[] array = new byte[16384];
        for (int i = inputStream.read(array); i >= 1; i = inputStream.read(array)) {
            fileOutputStream.write(array, 0, i);
        }
        inputStream.close();
        fileOutputStream.close();
        file.delete();
    }
    
    protected void extractPack(final String s, final String s2) throws Exception {
        final File file = new File(s);
        if (!file.exists()) {
            return;
        }
        final JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(s2));
        Pack200.newUnpacker().unpack(file, jarOutputStream);
        jarOutputStream.close();
        file.delete();
    }
    
    protected void extractJars(final String s) throws Exception {
        this.state = 5;
        final float n = 10.0f / this.urlList.length;
        for (int i = 0; i < this.urlList.length; ++i) {
            this.percentage = 55 + (int)(n * (i + 1));
            final String fileName = this.getFileName(this.urlList[i]);
            if (fileName.endsWith(".pack.lzma")) {
                this.subtaskMessage = "Extracting: " + fileName + " to " + fileName.replaceAll(".lzma", "");
                this.extractLZMA(s + fileName, s + fileName.replaceAll(".lzma", ""));
                this.subtaskMessage = "Extracting: " + fileName.replaceAll(".lzma", "") + " to " + fileName.replaceAll(".pack.lzma", "");
                this.extractPack(s + fileName.replaceAll(".lzma", ""), s + fileName.replaceAll(".pack.lzma", ""));
            }
            else if (fileName.endsWith(".pack")) {
                this.subtaskMessage = "Extracting: " + fileName + " to " + fileName.replace(".pack", "");
                this.extractPack(s + fileName, s + fileName.replace(".pack", ""));
            }
            else if (fileName.endsWith(".lzma")) {
                this.subtaskMessage = "Extracting: " + fileName + " to " + fileName.replace(".lzma", "");
                this.extractLZMA(s + fileName, s + fileName.replace(".lzma", ""));
            }
        }
    }
    
    protected void extractNatives(final String s) throws Exception {
        this.state = 5;
        final int percentage = this.percentage;
        final String jarName = this.getJarName(this.urlList[this.urlList.length - 1]);
        Certificate[] array = Launcher.class.getProtectionDomain().getCodeSource().getCertificates();
        if (array == null) {
            final JarURLConnection jarURLConnection = (JarURLConnection)new URL("jar:" + Launcher.class.getProtectionDomain().getCodeSource().getLocation().toString() + "!/net/minecraft/Launcher.class").openConnection();
            jarURLConnection.setDefaultUseCaches(true);
            try {
                array = jarURLConnection.getCertificates();
            }
            catch (Exception ex) {}
        }
        final File file = new File(s + "natives");
        if (!file.exists()) {
            file.mkdir();
        }
        final File file2 = new File(s + jarName);
        if (!file2.exists()) {
            return;
        }
        final JarFile jarFile = new JarFile(file2, true);
        final Enumeration<JarEntry> entries = jarFile.entries();
        this.totalSizeExtract = 0;
        while (entries.hasMoreElements()) {
            final JarEntry jarEntry = entries.nextElement();
            if (!jarEntry.isDirectory()) {
                if (jarEntry.getName().indexOf(47) != -1) {
                    continue;
                }
                this.totalSizeExtract += (int)jarEntry.getSize();
            }
        }
        this.currentSizeExtract = 0;
        final Enumeration<JarEntry> entries2 = jarFile.entries();
        while (entries2.hasMoreElements()) {
            final JarEntry jarEntry2 = entries2.nextElement();
            if (!jarEntry2.isDirectory()) {
                if (jarEntry2.getName().indexOf(47) != -1) {
                    continue;
                }
                final File file3 = new File(s + "natives" + File.separator + jarEntry2.getName());
                if (file3.exists() && !file3.delete()) {
                    continue;
                }
                final InputStream inputStream = jarFile.getInputStream(jarFile.getEntry(jarEntry2.getName()));
                final FileOutputStream fileOutputStream = new FileOutputStream(s + "natives" + File.separator + jarEntry2.getName());
                final byte[] array2 = new byte[65536];
                int read;
                while ((read = inputStream.read(array2, 0, array2.length)) != -1) {
                    fileOutputStream.write(array2, 0, read);
                    this.currentSizeExtract += read;
                    this.percentage = percentage + this.currentSizeExtract * 20 / this.totalSizeExtract;
                    this.subtaskMessage = "Extracting: " + jarEntry2.getName() + " " + this.currentSizeExtract * 100 / this.totalSizeExtract + "%";
                }
                validateCertificateChain(array, jarEntry2.getCertificates());
                inputStream.close();
                fileOutputStream.close();
            }
        }
        this.subtaskMessage = "";
        jarFile.close();
        new File(s + jarName).delete();
    }
    
    protected static void validateCertificateChain(final Certificate[] array, final Certificate[] array2) throws Exception {
        if (array == null) {
            return;
        }
        if (array2 == null) {
            throw new Exception("Unable to validate certificate chain. Native entry did not have a certificate chain at all");
        }
        if (array.length != array2.length) {
            throw new Exception("Unable to validate certificate chain. Chain differs in length [" + array.length + " vs " + array2.length + "]");
        }
        for (int i = 0; i < array.length; ++i) {
            if (!array[i].equals(array2[i])) {
                throw new Exception("Certificate mismatch: " + array[i] + " != " + array2[i]);
            }
        }
    }
    
    protected String getJarName(final URL url) {
        String s = url.getFile();
        if (s.contains("?")) {
            s = s.substring(0, s.indexOf("?"));
        }
        if (s.endsWith(".pack.lzma")) {
            s = s.replaceAll(".pack.lzma", "");
        }
        else if (s.endsWith(".pack")) {
            s = s.replaceAll(".pack", "");
        }
        else if (s.endsWith(".lzma")) {
            s = s.replaceAll(".lzma", "");
        }
        return s.substring(s.lastIndexOf(47) + 1);
    }
    
    protected String getFileName(final URL url) {
        String s = url.getFile();
        if (s.contains("?")) {
            s = s.substring(0, s.indexOf("?"));
        }
        return s.substring(s.lastIndexOf(47) + 1);
    }
    
    protected void fatalErrorOccured(final String s, final Exception ex) {
        ex.printStackTrace();
        this.fatalError = true;
        this.fatalErrorDescription = "Fatal error occured (" + this.state + "): " + s;
        System.out.println(this.fatalErrorDescription);
        System.out.println(this.generateStacktrace(ex));
    }
    
    public boolean canPlayOffline() {
        try {
            final File file = new File(AccessController.doPrivileged((PrivilegedExceptionAction<String>)new PrivilegedExceptionAction() {
                public Object run() throws Exception {
                    return Util.getWorkingDirectory() + File.separator + "bin" + File.separator;
                }
            }));
            if (!file.exists()) {
                return false;
            }
            final File file2 = new File(file, "version");
            if (!file2.exists()) {
                return false;
            }
            if (file2.exists()) {
                final String versionFile = this.readVersionFile(file2);
                if (versionFile != null && versionFile.length() > 0) {
                    return true;
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
        return false;
    }
    
    static {
        GameUpdater.natives_loaded = false;
        GameUpdater.forceUpdate = false;
    }
}
