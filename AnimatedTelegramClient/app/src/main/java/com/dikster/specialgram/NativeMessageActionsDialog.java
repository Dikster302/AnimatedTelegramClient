package com.dikster.specialgram;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.List;

/** Native Telegram-style message context actions, forwarding and editing dialogs. */
public final class NativeMessageActionsDialog {
    public interface Listener {
        void onDismiss();
        void onReply(TgMessage message);
        void onReaction(TgMessage message,String emoji);
        void onForward(TgMessage message,TgChat destination);
        void onPin(TgMessage message);
        void onEdit(TgMessage message,String text);
        void onDelete(TgMessage message,boolean forEveryone);
    }
    private static final String[] REACTIONS={"👍","❤️","😂","😮","😢","🔥"};
    private NativeMessageActionsDialog() { }

    public static void show(Context context,TgMessage message,List<TgChat> chats,long currentChatId,
                            int textColor,int mutedColor,int accentColor,Listener listener) {
        LinearLayout root=vertical(context);
        root.setPadding(dp(context,18),dp(context,8),dp(context,18),dp(context,4));
        root.addView(label(context,message.getOutgoing()
                ?"Выберите действие для своего сообщения"
                :"Действия с сообщением",mutedColor,13,false));
        TextView reactionTitle=label(context,"Быстрая реакция",mutedColor,12,true);
        LinearLayout.LayoutParams titleParams=wrap();
        titleParams.topMargin=dp(context,12);root.addView(reactionTitle,titleParams);
        LinearLayout reactions=new LinearLayout(context);reactions.setGravity(Gravity.CENTER);
        for(String emoji:REACTIONS){
            TextView chip=label(context,emoji,textColor,22,false);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(context,5),dp(context,8),dp(context,5),dp(context,8));
            chip.setOnClickListener(v->{if(listener!=null)listener.onReaction(message,emoji);});
            reactions.addView(chip,new LinearLayout.LayoutParams(0,dp(context,46),1f));
        }
        root.addView(reactions);
        CheckBox deleteForAll=new CheckBox(context);deleteForAll.setText("Удалить у всех");
        deleteForAll.setTextColor(textColor);deleteForAll.setTextSize(13);
        deleteForAll.setVisibility(message.getOutgoing()?View.VISIBLE:View.GONE);
        root.addView(deleteForAll);
        AlertDialog dialog=new AlertDialog.Builder(context).setTitle("Сообщение")
                .setView(root).setNegativeButton("Отмена",null).create();
        final Runnable[] afterDismiss={null};
        dialog.setOnDismissListener(d->{
            if(listener!=null)listener.onDismiss();
            if(afterDismiss[0]!=null)afterDismiss[0].run();
        });
        addAction(context,root,"Ответить",()->{
            if(listener!=null)listener.onReply(message);dialog.dismiss();
        },accentColor);
        addAction(context,root,"Переслать",()->{
            afterDismiss[0]=()->showForward(context,message,chats,currentChatId,textColor,mutedColor,listener);
            dialog.dismiss();
        },textColor);
        addAction(context,root,message.isPinned()?"Открепить":"Закрепить",()->{
            if(listener!=null)listener.onPin(message);dialog.dismiss();
        },textColor);
        if(message.getOutgoing())addAction(context,root,"Изменить",()->{
            afterDismiss[0]=()->showEdit(context,message,textColor,mutedColor,listener);
            dialog.dismiss();
        },textColor);
        addAction(context,root,"Удалить",()->{
            if(listener!=null)listener.onDelete(message,deleteForAll.isChecked());
            dialog.dismiss();
        },0xFFFF6B6B);
        dialog.show();
    }

    private static void showForward(Context context,TgMessage message,List<TgChat> chats,
                                    long currentChatId,int textColor,int mutedColor,Listener listener){
        LinearLayout root=vertical(context);
        root.setPadding(dp(context,14),dp(context,4),dp(context,14),dp(context,4));
        TextView preview=label(context,message.getText(),mutedColor,13,false);
        preview.setMaxLines(2);preview.setEllipsize(android.text.TextUtils.TruncateAt.END);
        root.addView(preview);
        ScrollView scroll=new ScrollView(context);
        LinearLayout rows=vertical(context);
        if(chats!=null)for(TgChat chat:chats){
            if(chat==null||chat.getId()==currentChatId)continue;
            LinearLayout row=new LinearLayout(context);row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0,dp(context,8),0,dp(context,8));
            NativeAvatarView avatar=new NativeAvatarView(context,chat.getAvatarPath(),
                    first(chat.getTitle()),chat.getId(),40,false,chat.getHasStory(),
                    !chat.getHasUnviewedStory(),0xFF333333);
            row.addView(avatar,new LinearLayout.LayoutParams(dp(context,40),dp(context,40)));
            LinearLayout labels=vertical(context);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,1f);lp.leftMargin=dp(context,11);
            labels.addView(label(context,chat.getTitle(),textColor,14,true));
            labels.addView(label(context,chat.getKind(),mutedColor,11,false));
            row.addView(labels,lp);
            row.setOnClickListener(v->{
                AlertDialog current=(AlertDialog)v.getTag();
                if(current!=null)current.dismiss();
                if(listener!=null)listener.onForward(message,chat);
            });
            rows.addView(row);
        }
        scroll.addView(rows);
        root.addView(scroll,new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,dp(context,330)));
        AlertDialog dialog=new AlertDialog.Builder(context).setTitle("Переслать сообщение")
                .setView(root).setNegativeButton("Отмена",null).create();
        for(int i=0;i<rows.getChildCount();i++){
            View row=rows.getChildAt(i);row.setTag(dialog);
        }
        dialog.show();
    }

    private static void showEdit(Context context,TgMessage message,int textColor,int mutedColor,
                                 Listener listener){
        EditText input=new EditText(context);input.setText(message.getText());
        input.setTextColor(textColor);input.setTextSize(15);input.setMinLines(2);input.setMaxLines(6);
        input.setSelection(input.length());
        input.setHintTextColor(mutedColor);input.setHint("Текст сообщения");
        int pad=dp(context,16);input.setPadding(pad,pad,pad,pad);
        new AlertDialog.Builder(context).setTitle("Изменить сообщение").setView(input)
                .setPositiveButton("Сохранить",(d,w)->{
                    if(listener!=null)listener.onEdit(message,input.getText().toString());
                }).setNegativeButton("Отмена",null).show();
    }

    private static void addAction(Context context,LinearLayout parent,String title,Runnable action,int color){
        TextView item=label(context,title,color,15,true);
        item.setGravity(Gravity.CENTER_VERTICAL);item.setPadding(dp(context,4),dp(context,11),dp(context,4),dp(context,11));
        item.setOnClickListener(v->{if(action!=null)action.run();});
        parent.addView(item,new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
    }
    private static LinearLayout vertical(Context c){LinearLayout v=new LinearLayout(c);v.setOrientation(LinearLayout.VERTICAL);return v;}
    private static TextView label(Context c,String value,int color,int size,boolean bold){
        TextView v=new TextView(c);v.setText(value==null?"":value);v.setTextColor(color);v.setTextSize(size);
        if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;
    }
    private static LinearLayout.LayoutParams wrap(){return new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);}
    private static String first(String s){return s==null||s.isEmpty()?"?":s.substring(0,1).toUpperCase();}
    private static int dp(Context c,int value){return Math.round(value*c.getResources().getDisplayMetrics().density);}
}
