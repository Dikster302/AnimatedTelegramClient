package com.dikster.specialgram;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

/** Compact Specialgram title bar following Telegram's familiar Android layout. */
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
        void onOpenArchive();
    }

    private final TextView title;
    private final SearchButton searchButton;
    private final TextView overflowButton;
    private Listener listener;
    private String themeName="Telegram";
    private int selectedTab;

    public NativeChatsHeaderView(Context context,int tab,int count,boolean unread,
            boolean newest,int textColor,TgAccount account,String themeName,Listener listener) {
        super(context);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setPadding(dp(8),dp(3),dp(8),dp(3));
        this.themeName=themeName==null?"Telegram":themeName;

        title=text(context,"Specialgram",textColor,20,true);
        title.setSingleLine(true);
        title.setContentDescription("Specialgram");
        title.setGravity(Gravity.CENTER_VERTICAL);
        addView(title,new LayoutParams(0,LayoutParams.WRAP_CONTENT,1f));

        searchButton=new SearchButton(context,textColor);
        searchButton.setVisibility(GONE);
        overflowButton=text(context,"⋮",textColor,25,true);
        overflowButton.setGravity(Gravity.CENTER);
        overflowButton.setContentDescription("Дополнительные действия");
        addView(overflowButton,new LayoutParams(dp(38),dp(46)));
        overflowButton.setOnClickListener(this::showMenu);
        update(tab,count,unread,newest,textColor,account,this.themeName,listener);
    }

    public void update(int tab,int count,boolean unread,boolean newest,int textColor,
            TgAccount account,String theme,Listener callback) {
        selectedTab=tab;
        themeName=theme==null?"Telegram":theme;
        listener=callback;
        title.setText(tab==0?"Specialgram":tab==1?"Контакты":tab==2?"Настройки":tab==3?"Профиль":"Звонки");
        title.setTextColor(textColor);
        overflowButton.setTextColor(0xFF8A9AA9);
        searchButton.setVisibility(tab==0?VISIBLE:GONE);
        overflowButton.setVisibility(tab==0?VISIBLE:GONE);
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
        menu.getMenu().add("▤ Архив").setOnMenuItemClickListener(item->{
            if(listener!=null)listener.onOpenArchive();return true;
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
                    getContext().getSharedPreferences("appearance",Context.MODE_PRIVATE)
                            .edit().putString("chatTitleEmoji",emojis[which]).apply();
                }).show();
    }

    private TextView text(Context context,String value,int color,int size,boolean bold) {
        TextView view=new TextView(context);view.setText(value);view.setTextColor(color);
        view.setTextSize(size);
        if(bold)view.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        view.setBackground(new RippleDrawable(android.content.res.ColorStateList.valueOf(0x2233B5E5),null,null));
        return view;
    }

    private final class SearchButton extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private int color;
        SearchButton(Context context,int color){super(context);this.color=color;}
        void setColor(int value){color=value;invalidate();}
        @Override protected void onDraw(Canvas canvas){
            super.onDraw(canvas);
            float cx=getWidth()/2f,cy=getHeight()/2f;
            paint.setColor(color);paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2));paint.setStrokeCap(Paint.Cap.ROUND);
            canvas.drawCircle(cx-dp(2),cy-dp(2),dp(7),paint);
            canvas.drawLine(cx+dp(3),cy+dp(3),cx+dp(9),cy+dp(9),paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }

    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
