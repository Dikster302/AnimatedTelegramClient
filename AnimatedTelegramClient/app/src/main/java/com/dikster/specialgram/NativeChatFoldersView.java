package com.dikster.specialgram;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Native selectable, horizontally scrollable chat-folder tabs. */
public final class NativeChatFoldersView extends HorizontalScrollView {
    public interface Listener { void onSelect(String folder); }

    private static final String[] FOLDERS={"Все","Личные","Группы","Каналы","Архив"};
    private String selected="Все";
    private int textColor,mutedColor,accentColor;
    private Listener listener;
    private final LinearLayout content;

    public NativeChatFoldersView(Context context,String selected,int text,int muted,int accent,
            Listener listener){
        super(context);
        setHorizontalScrollBarEnabled(false);
        setFillViewport(false);
        setOverScrollMode(OVER_SCROLL_NEVER);
        setClipToPadding(false);
        content=new LinearLayout(context);
        content.setOrientation(LinearLayout.HORIZONTAL);
        content.setGravity(Gravity.CENTER_VERTICAL);
        content.setPadding(dp(8),dp(2),dp(8),dp(2));
        addView(content,new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT));
        update(selected,text,muted,accent,listener);
    }

    public void update(String value,int text,int muted,int accent,Listener callback){
        listener=callback;
        String next=value==null?"Все":value;
        if(selected.equals(next)&&textColor==text&&mutedColor==muted&&accentColor==accent){
            revealSelected();
            return;
        }
        selected=next;
        textColor=text;
        mutedColor=muted;
        accentColor=accent;
        content.removeAllViews();
        for(String folder:FOLDERS){
            boolean active=folder.equals(selected);
            LinearLayout item=new LinearLayout(getContext());
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER_HORIZONTAL);
            item.setMinimumWidth(dp(76));
            item.setPadding(dp(10),dp(7),dp(10),dp(7));
            item.setBackground(new RippleDrawable(
                    ColorStateList.valueOf(0x2233B5E5),rounded(0x00000000,dp(12)),null));
            item.setFocusable(true);
            item.setClickable(true);
            item.setContentDescription(folder+" — папка чатов");
            item.setSelected(active);

            TextView label=new TextView(getContext());
            label.setText(folder);
            label.setTextColor(active?accentColor:textColor);
            label.setTextSize(14);
            label.setSingleLine(true);
            label.setIncludeFontPadding(false);
            label.setGravity(Gravity.CENTER);
            if(active)label.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
            item.addView(label,new LinearLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT));

            View underline=new View(getContext());
            underline.setBackground(rounded(active?accentColor:0x00000000,dp(4)));
            LinearLayout.LayoutParams up=new LinearLayout.LayoutParams(
                    dp(active?25:8),dp(3));
            up.topMargin=dp(7);
            item.addView(underline,up);

            item.setOnClickListener(v->{
                if(listener!=null)listener.onSelect(folder);
            });
            content.addView(item,new LinearLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT));
        }
        revealSelected();
    }

    private void revealSelected(){
        post(()->{
            for(int i=0;i<content.getChildCount();i++){
                View child=content.getChildAt(i);
                if(FOLDERS[i].equals(selected)){
                    int left=child.getLeft();
                    int right=child.getRight();
                    int visibleLeft=getScrollX();
                    int visibleRight=visibleLeft+getWidth();
                    if(left<visibleLeft)smoothScrollTo(Math.max(0,left-dp(8)),0);
                    else if(right>visibleRight)smoothScrollTo(right-getWidth()+dp(8),0);
                    return;
                }
            }
        });
    }

    private GradientDrawable rounded(int color,float radius){
        GradientDrawable shape=new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(radius);
        return shape;
    }

    private int dp(float value){
        return Math.round(value*getResources().getDisplayMetrics().density);
    }
}
