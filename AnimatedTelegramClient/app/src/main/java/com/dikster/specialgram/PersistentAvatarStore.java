package com.dikster.specialgram;

import android.content.Context;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/** Keeps downloaded profile photos in app-private storage instead of TDLib's transient cache. */
public final class PersistentAvatarStore {
    private PersistentAvatarStore() { }

    public static String persist(Context context, int fileId, String sourcePath) {
        if (context == null || fileId <= 0 || sourcePath == null || sourcePath.trim().isEmpty()) return sourcePath;
        File source = new File(sourcePath);
        if (!source.isFile() || source.length() <= 0L) return sourcePath;

        String extension = extensionOf(source.getName());
        File directory = new File(context.getFilesDir(), "avatars");
        if (!directory.exists() && !directory.mkdirs()) return sourcePath;
        File target = new File(directory, "avatar_" + fileId + extension);
        if (target.isFile() && target.length() == source.length()) return target.getAbsolutePath();

        File temporary = new File(directory, target.getName() + ".tmp");
        try (BufferedInputStream input = new BufferedInputStream(new FileInputStream(source));
             BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(temporary))) {
            byte[] buffer = new byte[16 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            output.flush();
        } catch (IOException failure) {
            temporary.delete();
            return sourcePath;
        }

        if (target.exists() && !target.delete()) {
            temporary.delete();
            return sourcePath;
        }
        if (!temporary.renameTo(target)) {
            temporary.delete();
            return sourcePath;
        }
        return target.getAbsolutePath();
    }

    private static String extensionOf(String name) {
        int dot = name == null ? -1 : name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) return ".jpg";
        String extension = name.substring(dot).toLowerCase(java.util.Locale.ROOT);
        return extension.matches("\\.[a-z0-9]{2,5}") ? extension : ".jpg";
    }
}
