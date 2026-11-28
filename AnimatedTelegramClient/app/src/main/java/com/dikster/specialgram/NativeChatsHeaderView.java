package com.dikster.specialgram;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

/** Telegram-inspired title row: no hamburger, one search field below and an overflow menu. */
public final class NativeChatsHeaderView extends LinearLayout {
    public interface Listener {
        void onSelectTab(int tab);
        void onSort(boolean newestFirst);
        void onToggleUnread();
        void onSearch();
        void onToggleTheme();
        void onOpenCalls();
        void onCreateGroup();
        void onSavedMessages();
    }

    private final TextView title;
    private final TextView premiumEmoji;
    private final TextView moreButton;
    private Listener listener;
    private TgAccount account;
    private String themeName="Telegram";
    private String selectedEmoji="⭐";
    private int selectedTab;

    public NativeChatsHeaderView(Context context,int tab,int count,boolean unread,
            boolean newest,int textColor,TgAccount account,String themeName,Listener listener) {
        super(context);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setPadding(dp(16),dp(5),dp(10),dp(5));
        this.account=account;
        this.themeName=themeName==null?"Telegram":themeName;
        selectedEmoji=context.getSharedPreferences("appearance",Context.MODE_PRIVATE)
                .getString("chatTitleEmoji","⭐");

        title=text(context,"Specialgram",Color.WHITE,20,true);
        addView(title,new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT));

        premiumEmoji=text(context,selectedEmoji,Color.WHITE,18,true);
        premiumEmoji.setGravity(Gravity.CENTER);
        GradientDrawable emojiBg=new GradientDrawable();
        emojiBg.setColor(0x332AABEE);emojiBg.setCornerRadius(dp(12));
        premiumEmoji.setBackground(emojiBg);
        LayoutParams ep=new LayoutParams(dp(34),dp(32));
        ep.leftMargin=dp(8);
        addView(premiumEmoji,ep);
        premiumEmoji.setOnClickListener(v->chooseEmoji());

        View spacer=new View(context);
        addView(spacer,new LayoutParams(0,1,1f));
        moreButton=text(context,"⋮",Color.WHITE,27,true);
        moreButton.setGravity(Gravity.CENTER);
        addView(moreButton,new LayoutParams(dp(42),dp(42)));
        moreButton.setOnClickListener(this::showMenu);
        update(tab,count,unread,newest,textColor,account,this.themeName,listener);
    }

    public void update(int tab,int count,boolean unread,boolean newest,int textColor,
            TgAccount account,String theme,Listener callback) {
        selectedTab=tab;this.account=account;
        themeName=theme==null?"Telegram":theme;
        listener=callback;
        title.setText(tab==0?"Specialgram":tab==1?"Контакты":tab==2?"Настройки":tab==3?"Профиль":"Звонки");
        premiumEmoji.setVisibility(tab==0?VISIBLE:GONE);
    }

    private void showMenu(View anchor) {
        PopupMenu menu=new PopupMenu(getContext(),anchor);
        menu.getMenu().add(themeName.equalsIgnoreCase("Midnight")||themeName.equalsIgnoreCase("Чёрная")
                ?"☀ Дневной режим":"☾ Ночной режим").setOnMenuItemClickListener(item->{
            if(listener!=null)listener.onToggleTheme();return true;
        });
        menu.getMenu().add("♙ Контакты").setOnMenuItemClickListener(item->{
            if(listener!=null)listener.onSelectTab(1);return true;
        });
        menu.getMenu().add("☎ Звонки").setOnMenuItemClickListener(item->{
            if(listener!=null)listener.onOpenCalls();return true;
        });
        menu.getMenu().add("＋ Создать группу").setOnMenuItemClickListener(item->{
            if(listener!=null)listener.onCreateGroup();return true;
        });
        menu.getMenu().add("★ Избранное").setOnMenuItemClickListener(item->{
            if(listener!=null)listener.onSavedMessages();return true;
        });
        menu.getMenu().add("Изменить эмодзи заголовка").setOnMenuItemClickListener(item->{
            chooseEmoji();return true;
        });
        menu.show();
    }

    private void chooseEmoji() {
        String[] emojis={"⭐","✨","🔥","💎","🚀","🌙","❤️","🦋","⚡","☀️","🎯","👑"};
        new AlertDialog.Builder(getContext()).setTitle("Эмодзи Specialgram")
                .setItems(emojis,(dialog,which)->{
                    selectedEmoji=emojis[which];
                    premiumEmoji.setText(selectedEmoji);
                    getContext().getSharedPreferences("appearance",Context.MODE_PRIVATE)
                            .edit().putString("chatTitleEmoji",selectedEmoji).apply();
                }).show();
    }

    private TextView text(Context context,String value,int color,int size,boolean bold) {
        TextView view=new TextView(context);view.setText(value);view.setTextColor(color);
        view.setTextSize(size);
        if(bold)view.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return view;
    }
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
