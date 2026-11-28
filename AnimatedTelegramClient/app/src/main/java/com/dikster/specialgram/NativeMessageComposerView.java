package com.dikster.specialgram;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

/** Native message composer with reply preview, attachments and video-note actions. */
public final class NativeMessageComposerView extends LinearLayout {
    public interface Listener {
        void onDraftChanged(String value);
        void onSend(String value);
        void onSendPhoto();
        void onSendDocument();
        void onRecordVideo();
        void onChooseVideoShape();
        void onClearReply();
    }

    private final LinearLayout replyBox, replyLabels;
    private final TextView replyTitle, replyText, clearReply;
    private final TextView attachment, record, send;
    private final EditText input;
    private Listener listener;
    private boolean changing;
    private int textColor, mutedColor, accentColor, panelColor, panelLightColor;

    public NativeMessageComposerView(Context context,String draft,TgMessage replyingTo,String chatTitle,
                                     int text,int muted,int accent,int panel,int panelLight,
                                     Listener listener) {
        super(context);
        setOrientation(VERTICAL);
        setPadding(dp(10),dp(7),dp(10),dp(7));
        replyBox=new LinearLayout(context);replyBox.setOrientation(HORIZONTAL);
        replyBox.setGravity(Gravity.CENTER_VERTICAL);
        replyBox.setPadding(dp(10),dp(6),dp(4),dp(6));
        GradientDrawable replyBg=new GradientDrawable();replyBg.setColor(panelLight);replyBg.setCornerRadius(dp(10));
        replyBox.setBackground(replyBg);
        replyLabels=new LinearLayout(context);replyLabels.setOrientation(VERTICAL);
        replyTitle=text(context,"",accent,12,true);
        replyText=text(context,"",text,12,false);replyText.setSingleLine(true);
        replyText.setEllipsize(android.text.TextUtils.TruncateAt.END);
        replyLabels.addView(replyTitle);replyLabels.addView(replyText);
        replyBox.addView(replyLabels,new LayoutParams(0,LayoutParams.WRAP_CONTENT,1f));
        clearReply=text(context,"✕",muted,18,false);clearReply.setGravity(Gravity.CENTER);
        replyBox.addView(clearReply,new LayoutParams(dp(38),dp(34)));
        clearReply.setOnClickListener(v->{if(this.listener!=null)this.listener.onClearReply();});
        addView(replyBox,new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));

        LinearLayout row=new LinearLayout(context);row.setGravity(Gravity.CENTER_VERTICAL);
        LayoutParams rowParams=new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);
        rowParams.topMargin=dp(5);addView(row,rowParams);
        attachment=text(context,"＋",accent,27,false);attachment.setGravity(Gravity.CENTER);
        row.addView(attachment,new LayoutParams(dp(40),dp(48)));
        attachment.setOnClickListener(this::showAttachments);

        input=new EditText(context);input.setTextSize(16);input.setTextColor(text);
        input.setHintTextColor(muted);input.setHint("Сообщение");
        input.setPadding(dp(14),dp(9),dp(14),dp(9));
        input.setMinHeight(dp(46));input.setMaxLines(4);
        input.setImeOptions(EditorInfo.IME_ACTION_NONE);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        GradientDrawable inputBg=new GradientDrawable();inputBg.setColor(panelLight);inputBg.setCornerRadius(dp(23));
        input.setBackground(inputBg);
        row.addView(input,new LayoutParams(0,LayoutParams.WRAP_CONTENT,1f));
        record=text(context,"◎",accent,25,false);record.setGravity(Gravity.CENTER);
        LayoutParams recordParams=new LayoutParams(dp(48),dp(49));recordParams.leftMargin=dp(5);
        row.addView(record,recordParams);
        GradientDrawable recordBg=new GradientDrawable();recordBg.setColor(panelLight);recordBg.setShape(GradientDrawable.OVAL);record.setBackground(recordBg);
        record.setOnClickListener(v->{if(this.listener!=null)this.listener.onRecordVideo();});
        record.setOnLongClickListener(v->{if(this.listener!=null)this.listener.onChooseVideoShape();return true;});
        send=text(context,"➤",Color.WHITE,21,true);send.setGravity(Gravity.CENTER);
        LayoutParams sendParams=new LayoutParams(dp(48),dp(49));sendParams.leftMargin=dp(5);
        row.addView(send,sendParams);
        send.setBackground(round(panelLight));
        send.setOnClickListener(v->submit());
        input.addTextChangedListener(new TextWatcher(){
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count){
                String value=s==null?"":s.toString();
                send.setEnabled(!value.trim().isEmpty());
                send.setAlpha(value.trim().isEmpty()?0.55f:1f);
                send.setBackground(round(value.trim().isEmpty()?panelLightColor:accentColor));
                if(!changing&&NativeMessageComposerView.this.listener!=null)
                    NativeMessageComposerView.this.listener.onDraftChanged(value);
            }
            @Override public void afterTextChanged(Editable e){}
        });
        update(draft,replyingTo,chatTitle,text,muted,accent,panel,panelLight,listener);
    }

    public void update(String draft,TgMessage replyingTo,String chatTitle,int text,int muted,
                       int accent,int panel,int panelLight,Listener callback) {
        listener=callback;textColor=text;mutedColor=muted;accentColor=accent;
        panelColor=panel;panelLightColor=panelLight;
        setBackgroundColor(panelColor);
        input.setTextColor(textColor);input.setHintTextColor(mutedColor);
        GradientDrawable inputBg=new GradientDrawable();inputBg.setColor(panelLightColor);inputBg.setCornerRadius(dp(23));
        input.setBackground(inputBg);
        record.setTextColor(accentColor);attachment.setTextColor(accentColor);
        GradientDrawable recordBackground=new GradientDrawable();recordBackground.setColor(panelLightColor);recordBackground.setShape(GradientDrawable.OVAL);record.setBackground(recordBackground);
        GradientDrawable replyBg=new GradientDrawable();replyBg.setColor(panelLightColor);replyBg.setCornerRadius(dp(10));
        replyBox.setBackground(replyBg);replyTitle.setTextColor(accentColor);
        replyText.setTextColor(textColor);clearReply.setTextColor(mutedColor);
        boolean hasReply=replyingTo!=null;
        replyBox.setVisibility(hasReply?VISIBLE:GONE);
        if(hasReply){
            replyTitle.setText(replyingTo.getOutgoing()?"Ответ на ваше сообщение":"Ответ на сообщение "+chatTitle);
            replyText.setText(replyingTo.getText());
        }
        if(!draft.equals(input.getText().toString())){
            changing=true;input.setText(draft);input.setSelection(input.length());changing=false;
        }
        boolean enabled=!draft.trim().isEmpty();send.setEnabled(enabled);send.setAlpha(enabled?1f:0.55f);
        send.setBackground(round(enabled?accentColor:panelLightColor));
    }

    private void submit(){
        String value=input.getText().toString().trim();
        if(value.isEmpty()||listener==null)return;
        changing=true;input.setText("");changing=false;
        listener.onDraftChanged("");
        listener.onSend(value);
    }

    private void showAttachments(View anchor){
        PopupMenu popup=new PopupMenu(getContext(),anchor);
        popup.getMenu().add("Фото или изображение").setOnMenuItemClickListener(item->{if(listener!=null)listener.onSendPhoto();return true;});
        popup.getMenu().add("Файл или документ").setOnMenuItemClickListener(item->{if(listener!=null)listener.onSendDocument();return true;});
        popup.show();
    }

    private GradientDrawable round(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setShape(GradientDrawable.OVAL);return d;}
    private TextView text(Context c,String value,int color,int size,boolean bold){
        TextView v=new TextView(c);v.setText(value);v.setTextColor(color);v.setTextSize(size);
        if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;
    }
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
