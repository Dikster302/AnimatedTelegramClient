package com.dikster.specialgram;

import android.app.AlertDialog;
import android.content.Context;

/** Native photo/video story creation chooser. */
public final class NativeStoryComposerDialog {
    private NativeStoryComposerDialog() { }
    public static void show(Context context,Runnable onPhoto,Runnable onVideo){
        new AlertDialog.Builder(context).setTitle("Добавить историю")
                .setMessage("Выбери фото или видео из галереи.")
                .setPositiveButton("Фото",(d,w)->{if(onPhoto!=null)onPhoto.run();})
                .setNeutralButton("Видео",(d,w)->{if(onVideo!=null)onVideo.run();})
                .setNegativeButton("Отмена",null).show();
    }
}
