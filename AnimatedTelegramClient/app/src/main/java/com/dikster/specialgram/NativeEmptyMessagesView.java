package com.dikster.specialgram;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Native empty/search-empty state for a Telegram conversation. */
public final class NativeEmptyMessagesView extends FrameLayout {
    private final TextView icon,title,subtitle;
    private int mutedColor;
    public NativeEmptyMessagesView(Context context,boolean searching,String query,String chatKind,
                                   int text,int muted){
        super(context);
        LinearLayout content=new LinearLayout(context);content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);content.setPadding(dp(24),dp(12),dp(24),dp(12));
        icon=new TextView(context);icon.setTextSize(36);icon.setGravity(Gravity.CENTER);
        title=new TextView(context);title.setTextSize(14);title.setGravity(Gravity.CENTER);
        subtitle=new TextView(context);subtitle.setTextSize(12);subtitle.setGravity(Gravity.CENTER);
        content.addView(icon);content.addView(title);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
        p.topMargin=dp(6);content.addView(subtitle,p);
        FrameLayout.LayoutParams fp=new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT,Gravity.CENTER);addView(content,fp);
        update(searching,query,chatKind,text,muted);
    }
    public void update(boolean searching,String query,String chatKind,int text,int muted){
        mutedColor=muted;
        boolean hasQuery=searching&&query!=null&&!query.trim().isEmpty();
        icon.setText(hasQuery?"⌕":"✉");icon.setTextColor(muted);
        title.setText(hasQuery?"Ничего не найдено":"Пока нет сообщений");title.setTextColor(muted);
        subtitle.setText(searching?("Секретный чат".equals(chatKind)
                ?"Поиск по загруженным сообщениям":"Поиск по истории Telegram")
                :"Отправьте первое сообщение");
        subtitle.setTextColor((muted&0x00FFFFFF)|0xB3000000);
    }
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
