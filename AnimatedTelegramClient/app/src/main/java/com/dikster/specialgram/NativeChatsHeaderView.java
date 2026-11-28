package com.dikster.specialgram;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

/** Native main-list toolbar and its Telegram-style navigation/sort menu. */
public final class NativeChatsHeaderView extends LinearLayout {
    public interface Listener {
        void onSelectTab(int tab);
        void onSort(boolean newestFirst);
        void onToggleUnread();
        void onSearch();
    }
    private final TextView menuButton, title, subtitle, searchButton;
    private Listener listener;
    private int selectedTab;
    private int chatCount;
    private boolean unreadOnly, newestFirst;

    public NativeChatsHeaderView(Context context,int tab,int count,boolean unread,
                                 boolean newest,int textColor,Listener listener) {
        super(context);
        setOrientation(HORIZONTAL);setGravity(Gravity.CENTER_VERTICAL);
        setPadding(dp(16),dp(10),dp(8),dp(10));
        menuButton=text(context,"☰",Color.WHITE,23,true);menuButton.setGravity(Gravity.CENTER);
        addView(menuButton,new LayoutParams(dp(42),dp(42)));
        View spacer=new View(context);addView(spacer,new LayoutParams(dp(8),1));
        LinearLayout labels=new LinearLayout(context);labels.setOrientation(VERTICAL);
        addView(labels,new LayoutParams(0,LayoutParams.WRAP_CONTENT,1f));
        title=text(context,"Specialgram",Color.WHITE,20,true);
        subtitle=text(context,"Specialgram · Telegram",0xC7FFFFFF,12,false);
        labels.addView(title);labels.addView(subtitle);
        searchButton=text(context,"⌕",Color.WHITE,25,false);searchButton.setGravity(Gravity.CENTER);
        addView(searchButton,new LayoutParams(dp(42),dp(42)));
        menuButton.setOnClickListener(this::showMenu);
        searchButton.setOnClickListener(v->{if(listener!=null)listener.onSearch();});
        update(tab,count,unread,newest,textColor,listener);
    }

    public void update(int tab,int count,boolean unread,boolean newest,int textColor,Listener callback) {
        selectedTab=tab;chatCount=count;unreadOnly=unread;newestFirst=newest;listener=callback;
        title.setText(tab==2?"Настройки":tab==1?"Контакты":"Specialgram");
        subtitle.setText(tab==0?count+" диалогов":"Specialgram · Telegram");
        searchButton.setVisibility(tab==0?VISIBLE:GONE);
    }

    private void showMenu(View anchor) {
        PopupMenu popup=new PopupMenu(getContext(),anchor);
        popup.getMenu().add("Чаты").setOnMenuItemClickListener(item->{if(listener!=null)listener.onSelectTab(0);return true;});
        popup.getMenu().add("Контакты").setOnMenuItemClickListener(item->{if(listener!=null)listener.onSelectTab(1);return true;});
        popup.getMenu().add("Настройки").setOnMenuItemClickListener(item->{if(listener!=null)listener.onSelectTab(2);return true;});
        popup.getMenu().add("Сначала новые").setOnMenuItemClickListener(item->{if(listener!=null)listener.onSort(true);return true;});
        popup.getMenu().add("По названию: А — Я").setOnMenuItemClickListener(item->{if(listener!=null)listener.onSort(false);return true;});
        popup.getMenu().add(unreadOnly?"Показать все чаты":"Только непрочитанные")
                .setOnMenuItemClickListener(item->{if(listener!=null)listener.onToggleUnread();return true;});
        popup.show();
    }

    private TextView text(Context c,String value,int color,int size,boolean bold){
        TextView v=new TextView(c);v.setText(value);v.setTextColor(color);v.setTextSize(size);
        if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;
    }
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
