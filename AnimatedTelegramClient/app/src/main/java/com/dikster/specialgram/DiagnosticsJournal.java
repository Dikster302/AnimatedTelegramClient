package com.dikster.specialgram;
import android.content.Context;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Private, bounded diagnostic journal. Sensitive identifiers and raw payloads are excluded. */
public final class DiagnosticsJournal {
    private static final String FILE_NAME = "specialgram-diagnostics.log";
    private static final int MAX_LINES = 300;
    private static final int MAX_READ_BYTES = 64 * 1024;
    private DiagnosticsJournal() { }

    public static synchronized void record(Context context, String request, int code, String message) {
        try {
            String safeRequest = NativeCore.sanitizeDiagnosticRequest(request);
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date());
            String safeMessage = message == null ? "" : message
                    .replaceAll("\\b\\d{5,}\\b", "[number]")
                    .replaceAll("[\\r\\n|]", " ")
                    .replaceAll("[^\\p{L}\\p{N} ._:\\-\\[\\]]", "")
                    .trim();
            if (safeMessage.length() > 120) safeMessage = safeMessage.substring(0, 120);
            String detail = safeMessage.isEmpty() ? "" : " | message=" + safeMessage;
            File file = new File(context.getApplicationContext().getFilesDir(), FILE_NAME);
            List<String> lines = new ArrayList<>();
            if (file.isFile()) {
                byte[] bytes = readAll(file);
                String[] parts = new String(bytes, StandardCharsets.UTF_8).split("\\r?\\n");
                int start = Math.max(0, parts.length - (MAX_LINES - 1));
                for (int i = start; i < parts.length; i++) if (!parts[i].isEmpty()) lines.add(parts[i]);
            }
            lines.add(timestamp + " | TDLib | " + safeRequest + " | code=" + code + detail);
            if (lines.size() > MAX_LINES) lines = lines.subList(lines.size() - MAX_LINES, lines.size());
            writeAll(file, (String.join("\n", lines) + "\n").getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) { }
    }

    public static synchronized String read(Context context) {
        try {
            File file = new File(context.getApplicationContext().getFilesDir(), FILE_NAME);
            if (!file.isFile()) return "";
            byte[] bytes = readAll(file);
            int start = Math.max(0, bytes.length - MAX_READ_BYTES);
            return new String(bytes, start, bytes.length - start, StandardCharsets.UTF_8);
        } catch (Exception ignored) { return ""; }
    }

    public static synchronized void clear(Context context) {
        try {
            File file = new File(context.getApplicationContext().getFilesDir(), FILE_NAME);
            if (file.isFile() && !file.delete()) throw new java.io.IOException("Unable to delete journal");
        } catch (Exception ignored) { }
    }

    private static byte[] readAll(File file) throws java.io.IOException {
        try (FileInputStream input = new FileInputStream(file);
             java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return output.toByteArray();
        }
    }

    private static void writeAll(File file, byte[] bytes) throws java.io.IOException {
        try (FileOutputStream output = new FileOutputStream(file, false)) {
            output.write(bytes);
            output.flush();
        }
    }
}
