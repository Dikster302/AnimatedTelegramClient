package com.dikster.specialgram;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Native selectable chat-folder tabs. */
public final class NativeChatFoldersView extends HorizontalScrollView {
    public interface Listener { void onSelect(String folder); }
    private static final String[] FOLDERS={"Все","Личные","Группы","Каналы","Архив"};
    private String selected="Все";
    private int textColor,mutedColor,accentColor;
    private Listener listener;
    private final LinearLayout content;

    public NativeChatFoldersView(Context context,String selected,int text,int muted,int accent,Listener listener){
        super(context);setHorizontalScrollBarEnabled(false);setFillViewport(false);
        setOverScrollMode(OVER_SCROLL_NEVER);
        content=new LinearLayout(context);content.setOrientation(LinearLayout.HORIZONTAL);
        content.setGravity(Gravity.CENTER_VERTICAL);
        content.setPadding(dp(8),dp(2),dp(8),dp(2));addView(content,new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT));
        update(selected,text,muted,accent,listener);
    }
    public void update(String value,int text,int muted,int accent,Listener callback){
        listener=callback;
        if(selected.equals(value)&&textColor==text&&mutedColor==muted&&accentColor==accent)return;
        selected=value==null?"Все":value;textColor=text;mutedColor=muted;accentColor=accent;
        content.removeAllViews();
        for(String folder:FOLDERS){
            boolean active=folder.equals(selected);
            LinearLayout item=new LinearLayout(getContext());item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER_HORIZONTAL);
            item.setMinimumWidth(dp(68));
            item.setPadding(dp(10),dp(7),dp(10),dp(7));
            TextView label=new TextView(getContext());label.setText(folder);
            label.setTextColor(active?accentColor:mutedColor);label.setTextSize(14);
            label.setSingleLine(true);label.setIncludeFontPadding(false);
            if(active)label.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
            item.addView(label);
            android.view.View underline=new android.view.View(getContext());
            GradientDrawable pill=new GradientDrawable();pill.setColor(active?accentColor:0x00000000);
            pill.setCornerRadius(dp(4));underline.setBackground(pill);
            LinearLayout.LayoutParams up=new LinearLayout.LayoutParams(dp(active?25:8),dp(3));up.topMargin=dp(7);
            item.addView(underline,up);
            item.setOnClickListener(v->{if(listener!=null)listener.onSelect(folder);});
            content.addView(item,new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT));
        }
    }
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
