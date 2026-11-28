package com.dikster.specialgram;

/** JNI boundary for code shared by Android now and iOS later. */
public final class NativeCore {
    private static final boolean AVAILABLE;
    static {
        boolean loaded;
        try { System.loadLibrary("specialgram_core"); loaded = true; }
        catch (UnsatisfiedLinkError error) { loaded = false; }
        AVAILABLE = loaded;
    }
    private NativeCore() { }
    private static native String nativeSanitizeDiagnosticRequest(String request);
    private static native String nativeEncodeVideoShape(String shape);
    private static native String nativeDecodeVideoShape(String caption);

    public static String sanitizeDiagnosticRequest(String request) {
        String value = request == null ? "" : request;
        if (AVAILABLE) {
            try {
                String sanitized = nativeSanitizeDiagnosticRequest(value);
                if (sanitized != null && !sanitized.isEmpty()) return sanitized;
            } catch (LinkageError ignored) { }
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < value.length() && result.length() < 64; i++) {
            char c = value.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_') result.append(c);
        }
        return result.length() == 0 ? "UnknownRequest" : result.toString();
    }
    public static String encodeVideoShape(String shape) {
        String value = shape == null ? "circle" : shape;
        if (AVAILABLE) {
            try {
                String encoded = nativeEncodeVideoShape(value);
                if (encoded != null && !encoded.isEmpty()) return encoded;
            } catch (LinkageError ignored) { }
        }
        String[] names = {"circle", "square", "rounded", "heart", "star", "hexagon"};
        int index = 0;
        for (int i = 0; i < names.length; i++) if (names[i].equals(value)) index = i;
        String bits = String.format(java.util.Locale.ROOT, "%3s",
                Integer.toBinaryString(index)).replace(' ', '0');
        StringBuilder result = new StringBuilder("\u2063");
        for (int i = 0; i < bits.length(); i++)
            result.append(bits.charAt(i) == '1' ? '\u200c' : '\u200b');
        return result.append('\u2063').toString();
    }

    public static String decodeVideoShape(String caption) {
        String value = caption == null ? "" : caption;
        if (AVAILABLE) {
            try {
                String decoded = nativeDecodeVideoShape(value);
                if (decoded != null && !decoded.isEmpty()) return decoded;
            } catch (LinkageError ignored) { }
        }
        int start = value.indexOf('\u2063');
        int end = start < 0 ? -1 : value.indexOf('\u2063', start + 1);
        if (start < 0 || end < 0) return "circle";
        StringBuilder bits = new StringBuilder();
        for (int i = start + 1; i < end; i++) {
            char c = value.charAt(i);
            if (c == '\u200c') bits.append('1');
            else if (c == '\u200b') bits.append('0');
            else return "circle";
        }
        try {
            int index = Integer.parseInt(bits.toString(), 2);
            String[] names = {"circle", "square", "rounded", "heart", "star", "hexagon"};
            return index >= 0 && index < names.length ? names[index] : "circle";
        } catch (RuntimeException ignored) {
            return "circle";
        }
    }

}
