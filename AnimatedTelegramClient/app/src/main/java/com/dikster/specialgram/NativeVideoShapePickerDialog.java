package com.dikster.specialgram;

import android.app.AlertDialog;
import android.content.Context;

/** Native selector for the six persistent Specialgram video-note display shapes. */
public final class NativeVideoShapePickerDialog {
    public interface Listener { void onSelect(String shape); }
    private static final String[] SHAPES={"circle","square","rounded","heart","star","hexagon"};
    private static final String[] LABELS={"Круг","Квадрат","Скруглённый квадрат","Сердце","Звезда","Шестиугольник"};
    private NativeVideoShapePickerDialog() { }

    public static void show(Context context,String selected,Listener listener) {
        int checked=0;
        for(int i=0;i<SHAPES.length;i++) if(SHAPES[i].equals(selected)){checked=i;break;}
        new AlertDialog.Builder(context)
                .setTitle("Форма видеосообщения")
                .setMessage("Форма сохраняется для следующих записей. В Specialgram она используется для отображения видеосообщения.")
                .setSingleChoiceItems(LABELS,checked,(dialog,which)->{
                    if(listener!=null&&which>=0&&which<SHAPES.length)listener.onSelect(SHAPES[which]);
                    dialog.dismiss();
                })
                .setNegativeButton("Отмена",null)
                .show();
    }
}
