package com.dikster.specialgram;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.List;

/** Native Telegram Stars gift picker with an explicit confirmation before purchase. */
public final class NativeGiftCatalogDialog {
    public interface Listener { void onSelect(TgGiftOption gift); }
    private NativeGiftCatalogDialog() { }

    public static void show(Context context,List<TgGiftOption> gifts,int textColor,int mutedColor,
                            int accentColor,Listener listener){
        Dialog dialog=new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout page=new LinearLayout(context);page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(context,18),dp(context,18),dp(context,18),dp(context,18));
        page.setBackground(round(0xF2171C22,dp(context,24)));
        LinearLayout header=new LinearLayout(context);header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=label(context,"Подарки",Color.WHITE,20,true);
        header.addView(title,new LinearLayout.LayoutParams(0,dp(context,48),1));
        TextView close=label(context,"×",Color.WHITE,28,false);close.setGravity(Gravity.CENTER);
        header.addView(close,new LinearLayout.LayoutParams(dp(context,44),dp(context,48)));
        close.setOnClickListener(v->dialog.dismiss());page.addView(header);

        TextView caption=label(context,"Выберите подарок и подтвердите отправку за Telegram Stars",0xBFFFFFFF,12,false);
        LinearLayout.LayoutParams cp=wrap();cp.bottomMargin=dp(context,12);page.addView(caption,cp);
        ScrollView scroll=new ScrollView(context);scroll.setFillViewport(true);
        LinearLayout list=new LinearLayout(context);list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        if(gifts==null||gifts.isEmpty()){
            TextView empty=label(context,"Сейчас нет доступных для отправки подарков",mutedColor,14,false);
            empty.setGravity(Gravity.CENTER);empty.setPadding(dp(context,18),dp(context,32),dp(context,18),dp(context,32));
            list.addView(empty,wrap());
        }else{
            for(TgGiftOption gift:gifts){
                LinearLayout row=new LinearLayout(context);row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(context,12),dp(context,10),dp(context,12),dp(context,10));
                row.setBackground(round(0x332FFFFFFF,dp(context,16)));
                LinearLayout.LayoutParams rp=wrap();rp.bottomMargin=dp(context,8);list.addView(row,rp);
                TextView emoji=label(context,gift.getEmoji(),Color.WHITE,30,false);
                emoji.setGravity(Gravity.CENTER);
                row.addView(emoji,new LinearLayout.LayoutParams(dp(context,54),dp(context,54)));
                LinearLayout details=new LinearLayout(context);details.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams detailsParams=new LinearLayout.LayoutParams(0,-2,1);detailsParams.leftMargin=dp(context,8);
                row.addView(details,detailsParams);
                TextView name=label(context,gift.isPremium()?"Подарок Premium":"Подарок",Color.WHITE,14,true);
                details.addView(name,wrap());
                String price="⭐ "+gift.getStarCount()+" Stars";
                if(gift.getUpgradeStarCount()>0)price+=" · улучшение "+gift.getUpgradeStarCount();
                TextView cost=label(context,price,0xBFFFFFFF,12,false);
                details.addView(cost,wrap());
                TextView send=label(context,"Выбрать",Color.WHITE,13,true);
                send.setGravity(Gravity.CENTER);send.setPadding(dp(context,12),dp(context,8),dp(context,12),dp(context,8));
                send.setBackground(round(accentColor,dp(context,14)));
                row.addView(send,new LinearLayout.LayoutParams(-2,dp(context,38)));
                row.setOnClickListener(v->confirm(context,dialog,gift,listener));
                send.setOnClickListener(v->confirm(context,dialog,gift,listener));
            }
        }
        dialog.setContentView(page);
        Window window=dialog.getWindow();
        if(window!=null)window.setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
        if(window!=null)window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.MATCH_PARENT);
    }

    private static void confirm(Context context,Dialog dialog,TgGiftOption gift,Listener listener){
        String message="Отправить "+gift.getEmoji()+" за "+gift.getStarCount()+" Telegram Stars?";
        new AlertDialog.Builder(context).setTitle("Подтверждение подарка").setMessage(message)
                .setNegativeButton("Отмена",(d,w)->d.dismiss())
                .setPositiveButton("Отправить",(d,w)->{
                    dialog.dismiss();if(listener!=null)listener.onSelect(gift);
                }).show();
    }
    private static TextView label(Context context,String value,int color,int size,boolean bold){
        TextView v=new TextView(context);v.setText(value);v.setTextColor(color);v.setTextSize(size);
        if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;
    }
    private static LinearLayout.LayoutParams wrap(){return new LinearLayout.LayoutParams(-1,-2);}
    private static GradientDrawable round(int color,int radius){
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(radius);return d;
    }
    private static int dp(Context context,int value){return Math.round(value*context.getResources().getDisplayMetrics().density);}
}
