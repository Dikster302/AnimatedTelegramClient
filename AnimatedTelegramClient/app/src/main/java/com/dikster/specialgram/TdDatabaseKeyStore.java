package com.dikster.specialgram;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.security.KeyStore;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Creates and retrieves the encrypted TDLib database key using Android Keystore. */
public final class TdDatabaseKeyStore {
    private static final String KEY_ALIAS = "animatedtelegram_tdlib_db";
    private static final String PREFS_NAME = "tdlib_secure";
    private static final String PREF_KEY = "dbkey";
    private TdDatabaseKeyStore() { }

    public static byte[] getOrCreate(Context context) throws Exception {
        Context app = context.getApplicationContext();
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);

        SecretKey secret = (SecretKey) store.getKey(KEY_ALIAS, null);
        if (secret == null) {
            KeyGenerator generator = KeyGenerator.getInstance("AES", "AndroidKeyStore");
            generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build());
            secret = generator.generateKey();
        }

        SharedPreferences preferences = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String saved = preferences.getString(PREF_KEY, null);
        if (saved != null) {
            byte[] encrypted = Base64.decode(saved, Base64.DEFAULT);
            byte[] iv = new byte[12];
            if (encrypted.length <= iv.length) {
                throw new java.security.GeneralSecurityException("Invalid encrypted TDLib database key");
            }
            System.arraycopy(encrypted, 0, iv, 0, iv.length);
            byte[] ciphertext = new byte[encrypted.length - iv.length];
            System.arraycopy(encrypted, iv.length, ciphertext, 0, ciphertext.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secret, new GCMParameterSpec(128, iv));
            return cipher.doFinal(ciphertext);
        }

        byte[] raw = new byte[32];
        new SecureRandom().nextBytes(raw);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, secret);
        byte[] iv = cipher.getIV();
        byte[] ciphertext = cipher.doFinal(raw);
        byte[] stored = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, stored, 0, iv.length);
        System.arraycopy(ciphertext, 0, stored, iv.length, ciphertext.length);
        preferences.edit().putString(PREF_KEY, Base64.encodeToString(stored, Base64.DEFAULT)).apply();
        return raw;
    }
}
