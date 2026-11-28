package com.dikster.specialgram;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Native Telegram-style profile dialog hosted by the conversation screen. */
public final class NativeProfileDialogHost extends FrameLayout {
    public interface Listener { void onClose(); }
    private final ScrollView scroll;
    private final AlertDialog dialog;
    private final Listener listener;
    private TgChat currentChat;
    private TgProfile currentProfile;
    private int textColor,mutedColor,accentColor,panelColor;

    public NativeProfileDialogHost(Context context,TgChat chat,TgProfile profile,
                                   int text,int muted,int accent,int panel,Listener listener){
        super(context);this.listener=listener;setVisibility(INVISIBLE);
        scroll=new ScrollView(context);scroll.setFillViewport(true);
        dialog=new AlertDialog.Builder(context).setView(scroll)
                .setPositiveButton("Закрыть",(d,w)->{}).create();
        dialog.setOnDismissListener(d->{if(this.listener!=null)this.listener.onClose();});
        update(chat,profile,text,muted,accent,panel);
        dialog.show();
    }

    public void update(TgChat chat,TgProfile profile,int text,int muted,int accent,int panel){
        boolean changed=currentChat==null||!currentChat.equals(chat)
                ||(currentProfile==null?profile!=null:!currentProfile.equals(profile))
                ||textColor!=text||mutedColor!=muted||accentColor!=accent||panelColor!=panel;
        currentChat=chat;currentProfile=profile;textColor=text;mutedColor=muted;
        accentColor=accent;panelColor=panel;
        String name=profile!=null&&!empty(profile.getName())?profile.getName():chat.getTitle();
        dialog.setTitle(name);
        if(!changed)return;
        LinearLayout body=new LinearLayout(getContext());body.setOrientation(LinearLayout.VERTICAL);
        body.setGravity(Gravity.CENTER_HORIZONTAL);body.setPadding(dp(18),dp(8),dp(18),dp(14));
        String avatarPath=profile!=null&&!empty(profile.getAvatarPath())?profile.getAvatarPath():chat.getAvatarPath();
        NativeAvatarView avatar=new NativeAvatarView(getContext(),avatarPath,first(name),chat.getId(),
                112,profile!=null&&profile.isOnline(),false,false,panelColor);
        body.addView(avatar,new LinearLayout.LayoutParams(dp(112),dp(112)));
        String status=profile==null?"Загружаем статус…":profile.getStatus();
        TextView statusView=text(status,profile!=null&&profile.isOnline()?0xFF35C759:mutedColor,14,true);
        LinearLayout.LayoutParams statusParams=wrap();statusParams.topMargin=dp(10);
        body.addView(statusView,statusParams);
        if(profile!=null&&profile.isVerified())body.addView(text("✓ Подтверждённый аккаунт",accentColor,12,true));
        if(profile!=null&&profile.isPremium())body.addView(text("✦ Telegram Premium",0xFFB58CFF,12,true));
        if(profile!=null&&!empty(profile.getUsername()))body.addView(text("@"+profile.getUsername(),accentColor,14,true));
        body.addView(text(profile==null?chat.getKind():profile.getKind(),mutedColor,13,false));
        if(profile!=null&&!empty(profile.getBio())){
            TextView bio=text(profile.getBio(),textColor,14,false);
            bio.setGravity(Gravity.CENTER);LinearLayout.LayoutParams bp=wrap();bp.topMargin=dp(8);body.addView(bio,bp);
        }
        if(profile!=null&&!empty(profile.getPhone()))addInfo(body,"Телефон",profile.getPhone());
        if(profile!=null&&profile.getCommonGroups()>0)addInfo(body,"Общие группы",String.valueOf(profile.getCommonGroups()));
        addInfo(body,"ID",String.valueOf(chat.getId()));
        scroll.removeAllViews();scroll.addView(body);
        if(dialog.isShowing())dialog.setTitle(name);
    }

    @Override protected void onDetachedFromWindow(){
        if(dialog.isShowing())dialog.dismiss();
        super.onDetachedFromWindow();
    }

    private void addInfo(LinearLayout parent,String label,String value){
        LinearLayout.LayoutParams params=wrap();params.topMargin=dp(9);
        parent.addView(NativeTextRows.profileInfo(getContext(),label,value,mutedColor,textColor),params);
    }
    private TextView text(String value,int color,int size,boolean bold){
        TextView view=new TextView(getContext());view.setText(value==null?"":value);
        view.setTextColor(color);view.setTextSize(size);
        if(bold)view.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);
        return view;
    }
    private LinearLayout.LayoutParams wrap(){return new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);}
    private String first(String value){return value==null||value.isEmpty()?"?":value.substring(0,1).toUpperCase();}
    private boolean empty(String value){return value==null||value.trim().isEmpty();}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
