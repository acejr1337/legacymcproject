package net.minecraft;

import java.util.List;
import java.util.ArrayList;

public class MinecraftLauncher
{
    private static final int MIN_HEAP = 511;
    private static final int RECOMMENDED_HEAP = 1024;
    
    public static void main(final String[] array) throws Exception {
        if (Runtime.getRuntime().maxMemory() / 1024L / 1024L > 511.0f) {
            LauncherFrame.main(array);
        }
        else {
            try {
                final String path = MinecraftLauncher.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
                final ArrayList list = new ArrayList();
                if (Util.getPlatform().equals(Util.OS.WINDOWS)) {
                    list.add("javaw");
                }
                else {
                    list.add("java");
                }
                list.add("-Xmx1024m");
                list.add("-Dsun.java2d.noddraw=true");
                list.add("-Dsun.java2d.d3d=false");
                list.add("-Dsun.java2d.opengl=false");
                list.add("-Dsun.java2d.pmoffscreen=false");
                list.add("-classpath");
                list.add(path);
                list.add("net.minecraft.LauncherFrame");
                if (new ProcessBuilder(list).start() == null) {
                    throw new Exception("!");
                }
                System.exit(0);
            }
            catch (Exception ex) {
                ex.printStackTrace();
                LauncherFrame.main(array);
            }
        }
    }
}
