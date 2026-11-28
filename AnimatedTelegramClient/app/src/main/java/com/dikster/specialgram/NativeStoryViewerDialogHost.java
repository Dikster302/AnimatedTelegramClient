package com.dikster.specialgram;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

/** Native dialog host for the full-screen Telegram-style story viewer. */
public final class NativeStoryViewerDialogHost extends FrameLayout {
    public interface Listener {
        void onDismiss();
        void onPrevious();
        void onNext();
        void onReact();
        void onReply(String text);
    }
    private final Dialog dialog;
    private final NativeStoryViewerView viewer;
    private Listener listener;

    public NativeStoryViewerDialogHost(Context context,TgStory story,int position,int count,Listener listener){
        super(context);this.listener=listener;setVisibility(INVISIBLE);
        viewer=new NativeStoryViewerView(context,story,position,count,bridge());
        dialog=new Dialog(context,android.R.style.Theme_Black_NoTitleBar);
        dialog.setContentView(viewer);
        dialog.setOnDismissListener(d->{if(this.listener!=null)this.listener.onDismiss();});
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
        Window window=dialog.getWindow();
        if(window!=null){
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams params=window.getAttributes();
            params.width=WindowManager.LayoutParams.MATCH_PARENT;
            params.height=dp(620);
            params.gravity=Gravity.CENTER;
            params.dimAmount=0.82f;
            window.setAttributes(params);
        }
    }

    public void update(TgStory story,int position,int count,Listener callback){
        listener=callback;
        viewer.update(story,position,count,bridge());
    }

    private NativeStoryViewerView.Listener bridge(){
        return new NativeStoryViewerView.Listener(){
            @Override public void onDismiss(){if(dialog.isShowing())dialog.dismiss();}
            @Override public void onPrevious(){if(listener!=null)listener.onPrevious();}
            @Override public void onNext(){if(listener!=null)listener.onNext();}
            @Override public void onReact(){if(listener!=null)listener.onReact();}
            @Override public void onReply(String text){if(listener!=null)listener.onReply(text);}
        };
    }

    @Override protected void onDetachedFromWindow(){
        if(dialog.isShowing())dialog.dismiss();
        super.onDetachedFromWindow();
    }
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
