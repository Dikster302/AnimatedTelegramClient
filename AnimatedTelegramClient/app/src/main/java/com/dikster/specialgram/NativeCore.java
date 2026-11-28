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
}
