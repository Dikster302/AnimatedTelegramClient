package com.dikster.specialgram;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Collections;
import java.util.List;

/** Read-only, non-opening chat peek. It deliberately does not call openChat or viewMessages. */
public final class NativeChatPreviewDialog {
    private NativeChatPreviewDialog(){ }

    public static void show(Context context,TgChat chat,List<TgMessage> messages,
            int text,int muted,int accent,int panel,int outgoing){
        if(chat==null)return;
        Dialog dialog=new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root=new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable background=new GradientDrawable();
        background.setColor(panel);background.setCornerRadius(dp(context,22));
        root.setBackground(background);
        root.setClipToOutline(true);
        LinearLayout header=new LinearLayout(context);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(context,18),dp(context,12),dp(context,14),dp(context,10));
        LinearLayout titles=new LinearLayout(context);titles.setOrientation(LinearLayout.VERTICAL);
        TextView name=new TextView(context);name.setText(chat.getTitle());name.setTextColor(text);
        name.setTextSize(17);name.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);
        titles.addView(name);
        TextView hint=new TextView(context);hint.setText("Предпросмотр · сообщения не прочитаны");
        hint.setTextColor(muted);hint.setTextSize(11);
        titles.addView(hint);
        header.addView(titles,new LinearLayout.LayoutParams(0,-2,1f));
        TextView close=new TextView(context);close.setText("×");close.setTextSize(26);
        close.setTextColor(muted);close.setGravity(Gravity.CENTER);
        header.addView(close,new LinearLayout.LayoutParams(dp(context,40),dp(context,42)));
        root.addView(header);
        View divider=new View(context);divider.setBackgroundColor(0x2AFFFFFF);
        root.addView(divider,new LinearLayout.LayoutParams(-1,dp(context,1)));
        NativeMessageListView history=new NativeMessageListView(context,chat.getId(),
                messages==null?Collections.emptyList():messages,false,-1,text,muted,accent,
                panel,outgoing,panel,20f,new NativeMessageListView.Listener(){
            @Override public void onLongPress(TgMessage message){ }
            @Override public void onReaction(TgMessage message,String emoji){ }
            @Override public void onLoadOlder(){ }
        });
        root.addView(history,new LinearLayout.LayoutParams(-1,0,1f));
        dialog.setContentView(root);
        close.setOnClickListener(v->dialog.dismiss());
        Window window=dialog.getWindow();
        if(window!=null){
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams params=window.getAttributes();
            params.dimAmount=.35f;window.setAttributes(params);
            window.setLayout((int)(context.getResources().getDisplayMetrics().widthPixels*.94f),
                    (int)(context.getResources().getDisplayMetrics().heightPixels*.78f));
        }
        dialog.show();
        if(window!=null)window.setLayout((int)(context.getResources().getDisplayMetrics().widthPixels*.94f),
                (int)(context.getResources().getDisplayMetrics().heightPixels*.78f));
    }

    private static int dp(Context context,int value){
        return Math.round(value*context.getResources().getDisplayMetrics().density);
    }
}
