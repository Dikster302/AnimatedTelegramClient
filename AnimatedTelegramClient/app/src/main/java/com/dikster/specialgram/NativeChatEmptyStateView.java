package com.dikster.specialgram;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Native empty-state panel for Telegram-style chat folders and search. */
public final class NativeChatEmptyStateView extends FrameLayout {
    private final TextView icon,title,subtitle;
    public NativeChatEmptyStateView(Context context,ListState state,int textColor,int mutedColor){
        super(context);
        LinearLayout content=new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        content.setPadding(dp(24),dp(20),dp(24),dp(20));
        icon=new TextView(context);icon.setTextSize(34);icon.setGravity(Gravity.CENTER);
        title=new TextView(context);title.setTextSize(16);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        title.setGravity(Gravity.CENTER);title.setTextColor(textColor);
        subtitle=new TextView(context);subtitle.setTextSize(13);subtitle.setGravity(Gravity.CENTER);
        subtitle.setTextColor(mutedColor);
        content.addView(icon);
        LinearLayout.LayoutParams titleParams=new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
        titleParams.topMargin=dp(10);content.addView(title,titleParams);
        LinearLayout.LayoutParams subtitleParams=new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
        subtitleParams.topMargin=dp(5);content.addView(subtitle,subtitleParams);
        addView(content,new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT,Gravity.CENTER));
        update(state,textColor,mutedColor);
    }
    public void update(ListState state,int textColor,int mutedColor){
        icon.setText(state.loading?"⌛":state.searching?"⌕":"☁");
        icon.setTextColor(mutedColor);
        title.setText(state.loading?"Загружаем чаты":state.searching?"Ничего не найдено":"Здесь пока пусто");
        title.setTextColor(textColor);
        subtitle.setText(state.loading?"Подключаемся к Telegram":state.searching
                ?"Попробуйте изменить запрос":"Новые переписки появятся здесь");
        subtitle.setTextColor(mutedColor);
    }
    public static final class ListState {
        public final boolean loading,searching;
        public ListState(boolean loading,boolean searching){this.loading=loading;this.searching=searching;}
    }
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
