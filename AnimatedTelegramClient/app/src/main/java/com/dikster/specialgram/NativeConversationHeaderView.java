package com.dikster.specialgram;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

/** Native conversation header with profile access and chat actions. */
public final class NativeConversationHeaderView extends LinearLayout {
    public interface Listener {
        void onBack();
        void onOpenProfile();
        void onSearch();
        void onClearHistory();
        void onToggleAnimated();
    }
    private final TextView back, title, status, menu;
    private final NativeAvatarView avatar;
    private Listener listener;
    private boolean animatedMode;
    private int textColor, mutedColor, accentColor, panelColor;

    public NativeConversationHeaderView(Context context,TgChat chat,TgProfile profile,
            boolean animated,int text,int muted,int accent,int panel,Listener listener) {
        super(context);
        setOrientation(HORIZONTAL);setGravity(Gravity.CENTER_VERTICAL);
        back=text(context,"‹",accent,32,true);back.setGravity(Gravity.CENTER);
        addView(back,new LayoutParams(dp(42),dp(52)));
        avatar=new NativeAvatarView(context,"","",chat.getId(),44,false,false,false,panel);
        addView(avatar,new LayoutParams(dp(44),dp(44)));
        LinearLayout labels=new LinearLayout(context);labels.setOrientation(VERTICAL);
        LayoutParams lp=new LayoutParams(0,LayoutParams.WRAP_CONTENT,1f);lp.leftMargin=dp(11);
        addView(labels,lp);
        title=text(context,"",text,16,true);title.setSingleLine(true);
        status=text(context,"",muted,15,true);status.setSingleLine(true);
        labels.addView(title);labels.addView(status);
        menu=text(context,"⋮",text,24,false);menu.setGravity(Gravity.CENTER);
        addView(menu,new LayoutParams(dp(44),dp(52)));
        back.setOnClickListener(v->{if(this.listener!=null)this.listener.onBack();});
        setOnClickListener(v->{if(this.listener!=null)this.listener.onOpenProfile();});
        menu.setOnClickListener(this::showMenu);
        update(chat,profile,animated,text,muted,accent,panel,listener);
    }

    public void update(TgChat chat,TgProfile profile,boolean animated,int text,int muted,
                       int accent,int panel,Listener callback) {
        listener=callback;animatedMode=animated;textColor=text;mutedColor=muted;
        accentColor=accent;panelColor=panel;
        String name=profile!=null&&!empty(profile.getName())?profile.getName():chat.getTitle();
        String path=profile!=null&&!empty(profile.getAvatarPath())?profile.getAvatarPath():chat.getAvatarPath();
        boolean online=profile!=null&&profile.isOnline();
        String presence=profile!=null&&!empty(profile.getStatus())?profile.getStatus():chat.getStatus();
        title.setText(name);status.setText(presence==null?"":presence);
        status.setTextColor(online?0xFF35C759:mutedColor);
        title.setTextColor(textColor);back.setTextColor(accentColor);menu.setTextColor(textColor);
        avatar.update(path,first(name),chat.getId(),44,online,false,false,panelColor);
    }

    private void showMenu(View anchor) {
        PopupMenu popup=new PopupMenu(getContext(),anchor);
        popup.getMenu().add("Поиск").setOnMenuItemClickListener(item->{if(listener!=null)listener.onSearch();return true;});
        popup.getMenu().add("Очистить переписку").setOnMenuItemClickListener(item->{if(listener!=null)listener.onClearHistory();return true;});
        popup.getMenu().add((animatedMode?"✓ ":"")+"Постепенная отправка").setOnMenuItemClickListener(item->{if(listener!=null)listener.onToggleAnimated();return true;});
        popup.show();
    }
    private TextView text(Context c,String value,int color,int size,boolean bold){
        TextView v=new TextView(c);v.setText(value);v.setTextColor(color);v.setTextSize(size);
        if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;
    }
    private String first(String value){return value==null||value.isEmpty()?"?":value.substring(0,1).toUpperCase();}
    private boolean empty(String value){return value==null||value.trim().isEmpty();}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
