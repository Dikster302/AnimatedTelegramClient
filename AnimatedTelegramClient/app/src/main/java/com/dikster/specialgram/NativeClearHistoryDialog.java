package com.dikster.specialgram;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.CheckBox;

/** Native confirmation dialog for clearing a Telegram conversation. */
public final class NativeClearHistoryDialog {
    public interface Listener { void onConfirm(boolean forEveryone); }
    private NativeClearHistoryDialog() { }
    public static void show(Context context,String chatTitle,Listener listener){
        CheckBox revoke=new CheckBox(context);revoke.setText("Удалить у всех участников чата");
        new AlertDialog.Builder(context).setTitle("Очистить переписку?")
                .setMessage("История будет удалена у вас. При необходимости отметьте удаление для всех участников.")
                .setView(revoke)
                .setPositiveButton("Очистить",(dialog,which)->{
                    if(listener!=null)listener.onConfirm(revoke.isChecked());
                })
                .setNegativeButton("Отмена",null).show();
    }
}
