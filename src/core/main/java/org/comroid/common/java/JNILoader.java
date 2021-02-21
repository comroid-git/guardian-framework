package org.comroid.common.java;

import org.comroid.common.os.OS;
import org.comroid.util.StackTraceUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.UUID;

public final class JNILoader {
    private JNILoader() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public static void loadLibrary(String name) {
        try {
            System.loadLibrary(name);
        } catch (UnsatisfiedLinkError ignored) {
            String jarName = getNameOfJarfile(StackTraceUtils.callerClass(1));
            if (jarName == null)
                loadFromJar(UUID.randomUUID().toString(), name);
            else loadFromJar(jarName, name);
        }
    }

    public static @Nullable String getNameOfJarfile(Class<?> ofClass) {
        try {
            return new File(ofClass.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI())
                    .getName();
        } catch (URISyntaxException ignored) {
            return null;
        }
    }

    public static void loadFromJar(String jarName, String name) {
        String extension = OS.current.getLibraryExtension();
        name = name + extension;
        File fileOut = null;

        try {
            // have to use a stream
            // always write to different location
            fileOut = new File(jarName + File.separator + name);

            if (!fileOut.exists()) {
                if (!fileOut.createNewFile())
                    throw new RuntimeException("Could not create file " + fileOut);
                try (
                        InputStream in = ClassLoader.getSystemResourceAsStream(name);
                        OutputStream out = new FileOutputStream(fileOut, false)
                ) {
                    if (in == null)
                        throw new UnsatisfiedLinkError("No such library: " + name);

                    byte[] buf = new byte[8192];
                    int length;
                    while ((length = in.read(buf)) > 0)
                        out.write(buf, 0, length);
                }
            }
            System.load(fileOut.toString());
        } catch (Exception e) {
            throw new RuntimeException("Error loading library into file: "
                    + (fileOut == null ? "null" : fileOut.getAbsolutePath()), e);
        }
    }
}
