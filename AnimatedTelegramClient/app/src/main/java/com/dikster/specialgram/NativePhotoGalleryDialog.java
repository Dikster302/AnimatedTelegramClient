package com.dikster.specialgram;

import android.app.Dialog;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Full-screen, swipeable Telegram-style profile photo gallery. */
public final class NativePhotoGalleryDialog {
    private NativePhotoGalleryDialog(){ }

    public static void show(Context context,List<String> source,int initialIndex){
        List<String> paths=new ArrayList<>();
        if(source!=null)for(String path:source)
            if(path!=null&&!path.trim().isEmpty()&&new File(path).isFile())paths.add(path);
        if(paths.isEmpty())return;
        Dialog dialog=new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        FrameLayout root=new FrameLayout(context);
        root.setBackgroundColor(Color.BLACK);
        ImageView image=new ImageView(context);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        root.addView(image,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout top=new LinearLayout(context);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(context,12),dp(context,28),dp(context,12),dp(context,8));
        TextView back=new TextView(context);back.setText("‹");back.setTextSize(34);
        back.setTextColor(Color.WHITE);back.setGravity(Gravity.CENTER);
        top.addView(back,new LinearLayout.LayoutParams(dp(context,48),dp(context,48)));
        TextView counter=new TextView(context);counter.setTextColor(Color.WHITE);
        counter.setTextSize(15);counter.setGravity(Gravity.CENTER);
        top.addView(counter,new LinearLayout.LayoutParams(0,dp(context,48),1f));
        root.addView(top,new FrameLayout.LayoutParams(-1,-2,Gravity.TOP));
        dialog.setContentView(root);
        Window window=dialog.getWindow();
        if(window!=null){
            window.setBackgroundDrawableResource(android.R.color.black);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.BLACK);window.setNavigationBarColor(Color.BLACK);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.MATCH_PARENT);
        }
        final int[] index={Math.max(0,Math.min(initialIndex,paths.size()-1))};
        Runnable[] render=new Runnable[1];
        render[0]=()->{
            String path=paths.get(index[0]);
            try{
                BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=2;
                image.setImageBitmap(BitmapFactory.decodeFile(path,options));
            }catch(RuntimeException ignored){image.setImageDrawable(null);}
            counter.setText((index[0]+1)+"/"+paths.size());
        };
        back.setOnClickListener(v->dialog.dismiss());
        float[] start={0f,0f};
        root.setOnTouchListener((v,event)->{
            if(event.getActionMasked()==MotionEvent.ACTION_DOWN){
                start[0]=event.getX();start[1]=event.getY();return true;
            }
            if(event.getActionMasked()==MotionEvent.ACTION_UP){
                float dx=event.getX()-start[0],dy=event.getY()-start[1];
                if(dy>dp(context,80)){dialog.dismiss();return true;}
                int next=index[0];
                if(Math.abs(dx)>dp(context,42))next+=dx<0?1:-1;
                else if(start[0]<root.getWidth()*.28f)next--;
                else if(start[0]>root.getWidth()*.72f)next++;
                else return true;
                if(next<0||next>=paths.size())return true;
                final int targetIndex=next;
                image.animate().translationX(dx<0?-dp(context,28):dp(context,28)).alpha(.35f)
                        .setDuration(90L).withEndAction(()->{
                            index[0]=targetIndex;render[0].run();
                            image.setTranslationX(dx<0?dp(context,28):-dp(context,28));
                            image.animate().translationX(0).alpha(1f).setDuration(190L)
                                    .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
                        }).start();
                return true;
            }
            return true;
        });
        render[0].run();
        dialog.show();
        if(window!=null)window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.MATCH_PARENT);
    }

    private static int dp(Context context,int value){
        return Math.round(value*context.getResources().getDisplayMetrics().density);
    }
}
