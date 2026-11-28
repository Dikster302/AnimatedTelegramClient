package com.dikster.specialgram;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.view.Surface;
import android.view.TextureView;
import android.widget.FrameLayout;
import java.io.IOException;

/** Looping video surface clipped to Specialgram's custom message shapes. */
public final class NativeShapeVideoView extends FrameLayout {
    private final TextureView texture;
    private final String path;
    private final String shape;
    private MediaPlayer player;
    private Surface surface;

    public NativeShapeVideoView(Context context,String path,String shape){
        super(context);this.path=path==null?"":path;this.shape=shape==null?"circle":shape;
        texture=new TextureView(context);texture.setOpaque(false);
        texture.setSurfaceTextureListener(new TextureView.SurfaceTextureListener(){
            @Override public void onSurfaceTextureAvailable(SurfaceTexture value,int width,int height){start(value);}
            @Override public void onSurfaceTextureSizeChanged(SurfaceTexture value,int width,int height){}
            @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture value){release();return true;}
            @Override public void onSurfaceTextureUpdated(SurfaceTexture value){}
        });
        addView(texture,new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
        setWillNotDraw(false);
    }

    private void start(SurfaceTexture textureSurface){
        release();
        if(path.isEmpty())return;
        try{
            player=new MediaPlayer();
            surface=new Surface(textureSurface);
            player.setSurface(surface);
            player.setDataSource(path);
            player.setLooping(true);
            player.setOnPreparedListener(MediaPlayer::start);
            player.prepareAsync();
        }catch(IOException|RuntimeException error){release();}
    }

    public void stop(){release();}

    private void release(){
        if(player!=null){try{player.stop();}catch(RuntimeException ignored){}try{player.release();}catch(RuntimeException ignored){}player=null;}
        if(surface!=null){try{surface.release();}catch(RuntimeException ignored){}surface=null;}
    }

    @Override protected void dispatchDraw(Canvas canvas){
        int save=canvas.save();
        Path clip=createPath(getWidth(),getHeight());
        canvas.clipPath(clip);
        super.dispatchDraw(canvas);
        canvas.restoreToCount(save);
    }

    private Path createPath(float width,float height){
        Path path=new Path();
        float w=width,h=height,min=Math.min(w,h);
        switch(shape){
            case "square":
                path.addRect(0,0,w,h,Path.Direction.CW);break;
            case "rounded":
                path.addRoundRect(0,0,w,h,dp(22),dp(22),Path.Direction.CW);break;
            case "heart":
                path.moveTo(w*.5f,h*.92f);
                path.cubicTo(w*.08f,h*.62f,-w*.04f,h*.30f,w*.22f,h*.16f);
                path.cubicTo(w*.38f,h*.06f,w*.5f,h*.22f,w*.5f,h*.29f);
                path.cubicTo(w*.5f,h*.22f,w*.62f,h*.06f,w*.78f,h*.16f);
                path.cubicTo(w*1.04f,h*.30f,w*.92f,h*.62f,w*.5f,h*.92f);
                path.close();break;
            case "star":
                for(int i=0;i<10;i++){
                    double angle=-Math.PI/2.0+Math.PI/5.0*i;
                    float radius=min*(i%2==0?.49f:.22f);
                    float x=w/2f+(float)Math.cos(angle)*radius;
                    float y=h/2f+(float)Math.sin(angle)*radius;
                    if(i==0)path.moveTo(x,y);else path.lineTo(x,y);
                }
                path.close();break;
            case "hexagon":
                for(int i=0;i<6;i++){
                    double angle=-Math.PI/2.0+Math.PI/3.0*i;
                    float x=w/2f+(float)Math.cos(angle)*min*.49f;
                    float y=h/2f+(float)Math.sin(angle)*min*.49f;
                    if(i==0)path.moveTo(x,y);else path.lineTo(x,y);
                }
                path.close();break;
            default:
                path.addCircle(w/2f,h/2f,min*.49f,Path.Direction.CW);
        }
        return path;
    }

    private float dp(float value){return value*getResources().getDisplayMetrics().density;}

    @Override protected void onDetachedFromWindow(){
        release();
        super.onDetachedFromWindow();
    }
}
