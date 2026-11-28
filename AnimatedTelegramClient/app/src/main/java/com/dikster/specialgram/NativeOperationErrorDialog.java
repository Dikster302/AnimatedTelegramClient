package com.dikster.specialgram;

import android.app.AlertDialog;
import android.content.Context;

/** Native error dialog for TDLib and media-operation failures. */
public final class NativeOperationErrorDialog {
    private NativeOperationErrorDialog() { }
    public static void show(Context context,String message,Runnable onDismiss){
        AlertDialog dialog=new AlertDialog.Builder(context)
                .setTitle("Не удалось выполнить действие")
                .setMessage(message==null?"Неизвестная ошибка":message)
                .setPositiveButton("Понятно",(d,w)->{if(onDismiss!=null)onDismiss.run();})
                .create();
        dialog.setOnCancelListener(d->{if(onDismiss!=null)onDismiss.run();});
        dialog.show();
    }
}
