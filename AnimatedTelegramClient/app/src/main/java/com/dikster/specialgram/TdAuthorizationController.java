package com.dikster.specialgram;

import android.content.Context;
import android.os.Build;

import org.drinkless.tdlib.TdApi;

import java.io.File;

/** Builds TDLib runtime parameters on the Java side of the Android client. */
public final class TdAuthorizationController {
    private TdAuthorizationController() { }

    public static TdApi.SetTdlibParameters createParameters(
            Context context, String apiId, String apiHash) throws Exception {
        Context app = context.getApplicationContext();
        File databaseDirectory = new File(app.getFilesDir(), "tdlib");
        File filesDirectory = new File(app.getFilesDir(), "tdlib-files");
        if (!databaseDirectory.isDirectory() && !databaseDirectory.mkdirs()) {
            throw new java.io.IOException("Unable to create TDLib database directory");
        }
        if (!filesDirectory.isDirectory() && !filesDirectory.mkdirs()) {
            throw new java.io.IOException("Unable to create TDLib files directory");
        }

        TdApi.SetTdlibParameters parameters = new TdApi.SetTdlibParameters();
        parameters.useTestDc = false;
        parameters.databaseDirectory = databaseDirectory.getAbsolutePath();
        parameters.filesDirectory = filesDirectory.getAbsolutePath();
        parameters.databaseEncryptionKey = TdDatabaseKeyStore.getOrCreate(app);
        parameters.useFileDatabase = true;
        parameters.useChatInfoDatabase = true;
        parameters.useMessageDatabase = true;
        parameters.useSecretChats = true;
        parameters.apiId = Integer.parseInt(apiId);
        parameters.apiHash = apiHash;
        parameters.systemLanguageCode = "ru";
        parameters.deviceModel = Build.MANUFACTURER + " " + Build.MODEL;
        parameters.systemVersion = Build.VERSION.RELEASE;
        parameters.applicationVersion = "1.0.0";
        return parameters;
    }
}
