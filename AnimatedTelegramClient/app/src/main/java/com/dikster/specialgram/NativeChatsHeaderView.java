package com.dikster.specialgram;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
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
    private PopupWindow drawer;
    private Listener listener;
    private int selectedTab;
    private int chatCount;
    private boolean unreadOnly, newestFirst;

    public NativeChatsHeaderView(Context context,int tab,int count,boolean unread,
                                 boolean newest,int textColor,TgAccount account,Listener listener) {
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
        update(tab,count,unread,newest,textColor,account,listener);
    }

    public void update(int tab,int count,boolean unread,boolean newest,int textColor,TgAccount account,Listener callback) {
        selectedTab=tab;chatCount=count;unreadOnly=unread;newestFirst=newest;listener=callback;
        title.setText(tab==2?"Настройки":tab==1?"Контакты":"Specialgram");
        subtitle.setText(tab==0?count+" диалогов":"Specialgram · Telegram");
        searchButton.setVisibility(tab==0?VISIBLE:GONE);
    }

    private void showMenu(View anchor) {
        if(drawer!=null&&drawer.isShowing()){drawer.dismiss();return;}
        Context context=getContext();
        LinearLayout panel=new LinearLayout(context);panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackgroundColor(0xFF17212B);
        LinearLayout profile=new LinearLayout(context);profile.setOrientation(LinearLayout.VERTICAL);
        profile.setPadding(dp(20),dp(28),dp(16),dp(18));profile.setBackgroundColor(0xFF202B36);
        TextView avatar=text(context,accountInitial(),Color.WHITE,25,true);avatar.setGravity(Gravity.CENTER);
        GradientDrawable circle=new GradientDrawable();circle.setShape(GradientDrawable.OVAL);circle.setColor(0xFF527DA3);
        avatar.setBackground(circle);profile.addView(avatar,new LinearLayout.LayoutParams(dp(58),dp(58)));
        TextView name=text(context,accountName(),Color.WHITE,16,true);
        LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);
        np.topMargin=dp(14);profile.addView(name,np);
        String phone=account==null?"":account.getPhone();
        TextView detail=text(context,phone,0xFFB7C5D2,13,false);
        profile.addView(detail);
        panel.addView(profile,new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
        addDrawerItem(panel,"Контакты","♙",1);
        addDrawerItem(panel,"Настройки","⚙",2);
        View divider=new View(context);divider.setBackgroundColor(0xFF34414D);
        LinearLayout.LayoutParams dpLine=new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,dp(1));
        dpLine.topMargin=dp(10);panel.addView(divider,dpLine);
        ScrollView scroll=new ScrollView(context);scroll.addView(panel);
        drawer=new PopupWindow(scroll,Math.min(dp(320),getResources().getDisplayMetrics().widthPixels-dp(48)),
                LayoutParams.MATCH_PARENT,true);
        drawer.setBackgroundDrawable(new ColorDrawable(0xFF17212B));
        drawer.setOutsideTouchable(true);drawer.setElevation(dp(16));
        drawer.setAnimationStyle(android.R.style.Animation_Activity);
        drawer.setOnDismissListener(()->drawer=null);
        drawer.showAtLocation(anchor.getRootView(),Gravity.TOP|Gravity.LEFT,0,0);
    }

    private void addDrawerItem(LinearLayout panel,String label,String symbol,int tab){
        LinearLayout item=new LinearLayout(getContext());item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dp(20),0,dp(16),0);
        TextView icon=text(getContext(),symbol,0xFFB7C5D2,21,false);
        icon.setGravity(Gravity.CENTER);item.addView(icon,new LinearLayout.LayoutParams(dp(36),dp(52)));
        TextView title=text(getContext(),label,Color.WHITE,15,false);
        LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,dp(52));
        tp.leftMargin=dp(16);item.addView(title,tp);
        item.setBackgroundResource(android.R.drawable.list_selector_background);
        item.setOnClickListener(v->{if(drawer!=null)drawer.dismiss();if(listener!=null)listener.onSelectTab(tab);});
        panel.addView(item,new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,dp(52)));
    }

    private String accountName(){
        if(account==null||account.getName().trim().isEmpty())return "Specialgram";
        return account.getName();
    }
    private String accountInitial(){
        String name=accountName().trim();return name.isEmpty()?"S":name.substring(0,1).toUpperCase(java.util.Locale.ROOT);
    }

    private TextView text(Context c,String value,int color,int size,boolean bold){
        TextView v=new TextView(c);v.setText(value);v.setTextColor(color);v.setTextSize(size);
        if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;
    }
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
