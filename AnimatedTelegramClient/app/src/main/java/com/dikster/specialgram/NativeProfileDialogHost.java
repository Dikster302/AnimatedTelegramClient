package com.dikster.specialgram;

import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.ClipData;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

/** Full-screen native profile and dedicated Telegram-style QR page. */
public final class NativeProfileDialogHost extends FrameLayout {
    public interface SectionCallback { void onResult(List<TgMessage> messages); }
    public interface Listener {
        void onClose();
        void onToggleMute(TgChat chat, boolean muted);
        void onOpenGiftCatalog(TgProfile profile);
        void onLoadProfileSection(TgChat chat,String tab,SectionCallback callback);
    }
    private final Dialog dialog;
    private final Listener listener;
    private TgChat currentChat;
    private TgProfile currentProfile;
    private List<TgMessage> currentMessages=Collections.emptyList();
    private final Map<String,List<TgMessage>> sectionCache=new HashMap<>();
    private final Set<String> loadingTabs=new HashSet<>();
    private int textColor,mutedColor,accentColor,panelColor;
    private String activeTab="Медиа";

    public NativeProfileDialogHost(Context context,TgChat chat,TgProfile profile,List<TgMessage> messages,
            int text,int muted,int accent,int panel,Listener listener){
        super(context);
        this.listener=listener;
        dialog=new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(this);
        dialog.setOnDismissListener(d->{if(this.listener!=null)this.listener.onClose();});
        update(chat,profile,messages,text,muted,accent,panel);
        Window w=dialog.getWindow();
        if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND|WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            w.setStatusBarColor(Color.TRANSPARENT);w.setNavigationBarColor(withAlpha(panelColor,0xE8));
            WindowManager.LayoutParams windowParams=w.getAttributes();windowParams.dimAmount=0.18f;w.setAttributes(windowParams);
            w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            w.setLayout(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.MATCH_PARENT);
            w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);}
        dialog.show();
        if(w!=null)w.setLayout(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.MATCH_PARENT);
        requestSection(activeTab);
    }

    public void update(TgChat chat,TgProfile profile,List<TgMessage> messages,int text,int muted,int accent,int panel){
        List<TgMessage> nextMessages=messages==null?Collections.emptyList():new ArrayList<>(messages);
        boolean changed=currentChat==null||!currentChat.equals(chat)
                ||(currentProfile==null?profile!=null:!currentProfile.equals(profile))
                ||!currentMessages.equals(nextMessages)
                ||textColor!=text||mutedColor!=muted||accentColor!=accent||panelColor!=panel;
        currentChat=chat;currentProfile=profile;currentMessages=nextMessages;textColor=text;mutedColor=muted;
        accentColor=accent;panelColor=panel;
        if(changed)render();
    }

    private void render(){
        removeAllViews();
        setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{withAlpha(panelColor,0xEA),withAlpha(panelColor,0xF4)}));
        LinearLayout page=new LinearLayout(getContext());
        page.setOrientation(LinearLayout.VERTICAL);
        addView(page,new FrameLayout.LayoutParams(-1,-1));

        LinearLayout top=new LinearLayout(getContext());
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(14),dp(8)+statusBarHeight(),dp(14),dp(8));
        TextView back=label("‹",textColor,32,true);
        back.setGravity(Gravity.CENTER);
        top.addView(back,new LinearLayout.LayoutParams(dp(42),dp(48)));
        back.setOnClickListener(v->dialog.dismiss());
        TextView title=label("Профиль",textColor,18,true);
        top.addView(title,new LinearLayout.LayoutParams(0,dp(48),1));
        TextView qr=label("▦",accentColor,25,true);
        qr.setGravity(Gravity.CENTER);
        qr.setBackground(round(withAlpha(accentColor,0x20),dp(14)));
        top.addView(qr,new LinearLayout.LayoutParams(dp(44),dp(48)));
        qr.setContentDescription("Показать QR-код");
        qr.setOnClickListener(v->showQr());
        page.addView(top);

        ScrollView scroll=new ScrollView(getContext());
        scroll.setFillViewport(true);
        page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout body=new LinearLayout(getContext());
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(18),dp(10),dp(18),dp(26));
        body.setGravity(Gravity.CENTER_HORIZONTAL);
        scroll.addView(body);

        String name=profileName();
        String avatarPath=currentProfile!=null&&!empty(currentProfile.getAvatarPath())
                ?currentProfile.getAvatarPath():currentChat.getAvatarPath();
        NativeAvatarView avatar=new NativeAvatarView(getContext(),avatarPath,first(name),
                currentChat.getId(),112,currentProfile!=null&&currentProfile.isOnline(),false,false,panelColor);
        body.addView(avatar,new LinearLayout.LayoutParams(dp(112),dp(112)));
        TextView nameView=label(name,textColor,23,true);
        nameView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams np=wrap();np.topMargin=dp(12);body.addView(nameView,np);

        String status=currentProfile==null?"Загружаем информацию…":currentProfile.getStatus();
        TextView statusView=label(status,currentProfile!=null&&currentProfile.isOnline()?0xFF35C759:mutedColor,14,false);
        statusView.setGravity(Gravity.CENTER);
        body.addView(statusView,wrap());

        if(currentProfile!=null&&currentProfile.isPremium())
            body.addView(centerLabel("✦ Telegram Premium",0xFFB58CFF,13,true,8));
        if(currentProfile!=null&&currentProfile.isVerified())
            body.addView(centerLabel("✓ Подтверждённый аккаунт",accentColor,13,true,4));

        LinearLayout actions=new LinearLayout(getContext());
        actions.setGravity(Gravity.CENTER);
        actions.setPadding(0,dp(20),0,dp(18));
        String[] actionNames={"Чат",currentChat.isMuted()?"Включить звук":"Отключить звук","Звонок","Подарок"};
        String[] glyphs={"▤","♫","☎","🎁"};
        for(int i=0;i<actionNames.length;i++){
            final int index=i;
            LinearLayout action=new LinearLayout(getContext());
            action.setOrientation(LinearLayout.VERTICAL);
            action.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(0,LayoutParams.WRAP_CONTENT,1);
            ap.leftMargin=dp(3);ap.rightMargin=dp(3);
            if(index==2&&currentProfile!=null&&!currentProfile.canBeCalled())action.setAlpha(0.55f);
            actions.addView(action,ap);
            TextView icon=label(glyphs[i],accentColor,22,true);
            icon.setGravity(Gravity.CENTER);
            icon.setBackground(round(withAlpha(accentColor,0x22),dp(17)));
            action.addView(icon,new LinearLayout.LayoutParams(dp(54),dp(54)));
            TextView caption=label(actionNames[i],textColor,12,true);
            caption.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams cp=wrap();cp.topMargin=dp(6);action.addView(caption,cp);
            action.setOnClickListener(v->{
                if(index==0)dialog.dismiss();
                else if(index==1){if(listener!=null)listener.onToggleMute(currentChat,!currentChat.isMuted());}
                else if(index==2){
                    String message=currentProfile!=null&&!currentProfile.canBeCalled()
                            ?"Пользователь ограничил входящие звонки"
                            :"Вызовы пока не подключены: требуется аудиомодуль и экран соединения";
                    Toast.makeText(getContext(),message,Toast.LENGTH_SHORT).show();
                }else{if(listener!=null)listener.onOpenGiftCatalog(currentProfile);}
            });
        }
        body.addView(actions);

        LinearLayout info=cardColumn();
        if(currentProfile!=null&&!empty(currentProfile.getPhone()))
            addInfo(info,"Телефон",formatPhone(currentProfile.getPhone()));
        if(currentProfile!=null&&!empty(currentProfile.getUsername()))
            addInfo(info,"Имя пользователя","@"+currentProfile.getUsername());
        if(currentProfile!=null&&!empty(currentProfile.getBio()))
            addInfo(info,"О себе",currentProfile.getBio());
        if(currentProfile!=null&&!empty(currentProfile.getBirthdate())){
            addInfo(info,"День рождения",currentProfile.getBirthdate());
            String age=calculateAge(currentProfile.getBirthdate());
            if(!age.isEmpty())addInfo(info,"Возраст",age);
        }
        if(currentProfile!=null&&currentProfile.getCommonGroups()>0)
            addInfo(info,"Общие группы",String.valueOf(currentProfile.getCommonGroups()));
        addInfo(info,"ID",String.valueOf(currentChat.getId()));
        body.addView(info,wrap());

        TextView tabsTitle=label("Материалы",textColor,17,true);
        LinearLayout.LayoutParams tp=wrap();tp.topMargin=dp(22);tp.bottomMargin=dp(10);body.addView(tabsTitle,tp);
        HorizontalScrollView tabsScroll=new HorizontalScrollView(getContext());
        tabsScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout tabs=new LinearLayout(getContext());tabs.setGravity(Gravity.CENTER_VERTICAL);
        tabsScroll.addView(tabs);
        String[] names={"Подарки","Медиа","Избранное","Файлы","Ссылки","Голосовые","GIF"};
        for(String tab:names){
            boolean selected=tab.equals(activeTab);
            TextView chip=label(tab,selected?Color.WHITE:textColor,13,true);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(14),dp(10),dp(14),dp(10));
            chip.setBackground(round(selected?accentColor:0x18000000,dp(18)));
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-2,dp(40));
            cp.rightMargin=dp(7);tabs.addView(chip,cp);
            chip.setOnClickListener(v->{activeTab=tab;render();requestSection(tab);});
        }
        body.addView(tabsScroll,new LinearLayout.LayoutParams(-1,dp(44)));
        LinearLayout contentCard=cardColumn();
        List<TgMessage> sectionMessages=messagesForTab(activeTab);
        if(loadingTabs.contains(activeTab)){
            TextView loading=label("Загружаем материалы…",mutedColor,13,false);
            loading.setGravity(Gravity.CENTER);loading.setPadding(dp(12),dp(22),dp(12),dp(22));
            contentCard.addView(loading,wrap());
        }else if("Подарки".equals(activeTab)&&currentProfile!=null&&!currentProfile.getGifts().isEmpty()){
            TextView hint=label("Подарки в профиле · "+currentProfile.getGiftCount(),mutedColor,11,false);
            LinearLayout.LayoutParams hintParams=wrap();hintParams.leftMargin=dp(14);hintParams.topMargin=dp(10);
            contentCard.addView(hint,hintParams);
            for(int i=0;i<currentProfile.getGifts().size();i++){
                TextView item=label("🎁  "+currentProfile.getGifts().get(i),textColor,14,false);
                item.setMaxLines(4);item.setPadding(dp(14),dp(12),dp(14),dp(12));
                contentCard.addView(item,wrap());
                if(i<currentProfile.getGifts().size()-1){View divider=new View(getContext());divider.setBackgroundColor(0x22FFFFFF);
                    contentCard.addView(divider,new LinearLayout.LayoutParams(-1,dp(1)));}
            }
        }else if("Подарки".equals(activeTab)&&currentProfile!=null&&currentProfile.getGiftCount()>0){
            TextView emptyText=label("В профиле указано подарков: "+currentProfile.getGiftCount()+". Список пока недоступен.",mutedColor,13,false);
            emptyText.setGravity(Gravity.CENTER);emptyText.setPadding(dp(12),dp(16),dp(12),dp(16));
            contentCard.addView(emptyText,wrap());
        }else if(sectionMessages.isEmpty()){
            TextView emptyIcon=label("▧",mutedColor,28,true);emptyIcon.setGravity(Gravity.CENTER);
            contentCard.addView(emptyIcon,wrap());
            TextView emptyText=label(emptyTextFor(activeTab),mutedColor,13,false);
            emptyText.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams ep=wrap();ep.topMargin=dp(5);contentCard.addView(emptyText,ep);
        }else{
            TextView hint=label("Материалы из загруженной истории чата",mutedColor,11,false);
            LinearLayout.LayoutParams hintParams=wrap();hintParams.leftMargin=dp(14);hintParams.topMargin=dp(10);
            contentCard.addView(hint,hintParams);
            int count=Math.min(sectionMessages.size(),30);
            for(int i=0;i<count;i++){
                TgMessage message=sectionMessages.get(i);
                String preview=!empty(message.getMediaName())?message.getMediaName():
                        !empty(message.getText())?message.getText():mediaLabel(message.getMediaKind());
                TextView item=label(preview,textColor,14,false);
                item.setMaxLines(2);item.setPadding(dp(14),dp(11),dp(14),dp(11));
                contentCard.addView(item,wrap());
                if(i<count-1){View divider=new View(getContext());divider.setBackgroundColor(0x22FFFFFF);
                    contentCard.addView(divider,new LinearLayout.LayoutParams(-1,dp(1)));}
            }
        }
        LinearLayout.LayoutParams emptyParams=wrap();emptyParams.topMargin=dp(10);
        body.addView(contentCard,emptyParams);
    }

    private void showQr(){
        String username=currentProfile==null?"":currentProfile.getUsername();
        String address=!empty(username)?"https://t.me/"+username:"tg://user?id="+currentChat.getId();
        Bitmap bitmap=makeQr(address);
        if(bitmap==null){Toast.makeText(getContext(),"Не удалось создать QR-код",Toast.LENGTH_SHORT).show();return;}
        Dialog qrDialog=new Dialog(getContext());
        LinearLayout page=new LinearLayout(getContext());page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18),dp(14)+statusBarHeight(),dp(18),dp(18));
        page.setBackground(round(0xE8171C22,dp(0)));
        qrDialog.setContentView(page);
        LinearLayout top=new LinearLayout(getContext());top.setGravity(Gravity.CENTER_VERTICAL);
        TextView scan=label("Сканировать QR-код",0x99FFFFFF,14,true);
        scan.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);
        top.addView(scan,new LinearLayout.LayoutParams(0,dp(48),1));
        TextView close=label("×",Color.WHITE,28,false);close.setGravity(Gravity.CENTER);
        top.addView(close,new LinearLayout.LayoutParams(dp(42),dp(48)));
        close.setOnClickListener(v->qrDialog.dismiss());page.addView(top);

        page.addView(new View(getContext()),new LinearLayout.LayoutParams(-1,0,1f));
        ImageView image=new ImageView(getContext());image.setImageBitmap(bitmap);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setPadding(dp(20),dp(20),dp(20),dp(20));image.setBackground(round(Color.WHITE,dp(22)));
        LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(-1,dp(280));
        ip.topMargin=dp(12);page.addView(image,ip);
        TextView handle=label("@"+(empty(username)?"user"+currentChat.getId():username),Color.BLACK,25,true);
        handle.setGravity(Gravity.CENTER);
        handle.setPadding(dp(12),dp(20),dp(12),dp(20));
        handle.setBackground(round(Color.WHITE,dp(22)));
        LinearLayout.LayoutParams hp=wrap();hp.topMargin=dp(12);page.addView(handle,hp);

        page.addView(new View(getContext()),new LinearLayout.LayoutParams(1,0,1f));
        LinearLayout bottom=new LinearLayout(getContext());bottom.setGravity(Gravity.CENTER_VERTICAL);
        TextView share=label("Поделиться",Color.WHITE,14,true);
        TextView save=label("Скачать QR-код",Color.WHITE,14,true);
        bottom.addView(share,new LinearLayout.LayoutParams(0,dp(54),1));
        bottom.addView(save,new LinearLayout.LayoutParams(0,dp(54),1));
        page.addView(bottom);
        share.setOnClickListener(v->shareQr(bitmap,address));
        save.setOnClickListener(v->saveQr(bitmap));
        Window w=qrDialog.getWindow();if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);
            w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            w.setStatusBarColor(Color.TRANSPARENT);w.setNavigationBarColor(0xF2171C22);}
        qrDialog.show();
        if(w!=null)w.setLayout(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.MATCH_PARENT);
    }

    private Bitmap makeQr(String value){
        try{
            BitMatrix matrix=new MultiFormatWriter().encode(value,BarcodeFormat.QR_CODE,640,640);
            Bitmap bitmap=Bitmap.createBitmap(640,640,Bitmap.Config.ARGB_8888);
            for(int y=0;y<640;y++)for(int x=0;x<640;x++)
                bitmap.setPixel(x,y,matrix.get(x,y)?Color.BLACK:Color.WHITE);
            return bitmap;
        }catch(Exception e){return null;}
    }

    private void shareQr(Bitmap bitmap,String profileAddress){
        File directory=new File(getContext().getCacheDir(),"outgoing_media");
        if(!directory.exists()&&!directory.mkdirs()){
            Toast.makeText(getContext(),"Не удалось подготовить QR-код",Toast.LENGTH_SHORT).show();return;
        }
        File file=new File(directory,"specialgram_profile_"+currentChat.getId()+".png");
        try(FileOutputStream out=new FileOutputStream(file)){
            if(!bitmap.compress(Bitmap.CompressFormat.PNG,100,out))throw new IllegalStateException("PNG encode failed");
            Uri uri=FileProvider.getUriForFile(getContext(),getContext().getPackageName()+".fileprovider",file);
            Intent send=new Intent(Intent.ACTION_SEND);send.setType("image/png");
            send.putExtra(Intent.EXTRA_STREAM,uri);send.putExtra(Intent.EXTRA_TEXT,profileAddress);
            send.setClipData(ClipData.newUri(getContext().getContentResolver(),"QR-код профиля",uri));
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            getContext().startActivity(Intent.createChooser(send,"Поделиться QR-кодом"));
        }catch(Exception e){
            Toast.makeText(getContext(),"Не удалось поделиться QR-кодом",Toast.LENGTH_SHORT).show();
        }
    }
    private int withAlpha(int color,int alpha){return (color&0x00FFFFFF)|((alpha&0xFF)<<24);}
    private void saveQr(Bitmap bitmap){
        try{
            if(Build.VERSION.SDK_INT>=29){
                ContentValues values=new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME,"specialgram_qr_"+currentChat.getId()+".png");
                values.put(MediaStore.Images.Media.MIME_TYPE,"image/png");
                values.put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/Specialgram");
                Uri uri=getContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);
                if(uri==null)throw new IllegalStateException("MediaStore unavailable");
                try(OutputStream out=getContext().getContentResolver().openOutputStream(uri)){
                    if(out==null)throw new IllegalStateException("Output unavailable");
                    bitmap.compress(Bitmap.CompressFormat.PNG,100,out);
                }
                Toast.makeText(getContext(),"QR-код сохранён в галерею",Toast.LENGTH_SHORT).show();
            }else Toast.makeText(getContext(),"Сохранение в галерею доступно на Android 10 и новее",Toast.LENGTH_LONG).show();
        }catch(Exception e){Toast.makeText(getContext(),"Не удалось сохранить QR-код",Toast.LENGTH_SHORT).show();}
    }

    private void requestSection(String tab){
        if("Подарки".equals(tab)||listener==null)return;
        loadingTabs.add(tab);render();
        listener.onLoadProfileSection(currentChat,tab,values->{
            if(!dialog.isShowing())return;
            sectionCache.put(tab,values==null?Collections.emptyList():new ArrayList<>(values));
            loadingTabs.remove(tab);
            if(tab.equals(activeTab))render();
        });
    }
    private List<TgMessage> messagesForTab(String tab){
        if(sectionCache.containsKey(tab))return sectionCache.get(tab);
        List<TgMessage> result=new ArrayList<>();
        for(int i=currentMessages.size()-1;i>=0;i--){
            TgMessage message=currentMessages.get(i);
            String kind=message.getMediaKind().toLowerCase(java.util.Locale.ROOT);
            String text=message.getText().toLowerCase(java.util.Locale.ROOT);
            boolean match=false;
            switch(tab){
                case "Медиа": match=kind.contains("photo")||kind.contains("video")||kind.contains("animation")||kind.contains("gif");break;
                case "Избранное": match=message.isPinned();break;
                case "Файлы": match=kind.contains("document")||kind.contains("file");break;
                case "Ссылки": match=text.contains("https://")||text.contains("http://")||text.contains("t.me/");break;
                case "Голосовые": match=kind.contains("voice")||kind.contains("audio");break;
                case "GIF": match=kind.contains("animation")||kind.contains("gif");break;
                default: break;
            }
            if(match)result.add(message);
        }
        return result;
    }
    private String mediaLabel(String kind){
        if(kind==null||kind.isEmpty())return "Сообщение";
        if(kind.contains("photo"))return "Фото";
        if(kind.contains("video"))return "Видео";
        if(kind.contains("voice"))return "Голосовое сообщение";
        if(kind.contains("audio"))return "Аудио";
        if(kind.contains("document")||kind.contains("file"))return "Файл";
        if(kind.contains("animation")||kind.contains("gif"))return "GIF";
        return kind;
    }
    private void addInfo(LinearLayout parent,String label,String value){
        LinearLayout row=new LinearLayout(getContext());row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(14),dp(11),dp(14),dp(11));
        TextView l=label(label,mutedColor,12,false),v=label(value,textColor,15,false);
        row.addView(l);LinearLayout.LayoutParams vp=wrap();vp.topMargin=dp(3);row.addView(v,vp);
        parent.addView(row,new LinearLayout.LayoutParams(-1,-2));
        View line=new View(getContext());line.setBackgroundColor(0x22FFFFFF);
        parent.addView(line,new LinearLayout.LayoutParams(-1,dp(1)));
    }
    private LinearLayout cardColumn(){
        LinearLayout card=new LinearLayout(getContext());card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(round(withAlpha(panelColor==Color.WHITE?0xFFF4F6F8:0xFF20262E,0xE8),dp(18)));
        return card;
    }
    private TextView centerLabel(String value,int color,int size,boolean bold,int top){
        TextView v=label(value,color,size,bold);v.setGravity(Gravity.CENTER);
        v.setPadding(0,dp(top),0,0);return v;
    }
    private String emptyTextFor(String tab){
        switch(tab){case "Подарки":return "Подарков пока нет";case "Медиа":return "Здесь будут фото и видео";
            case "Избранное":return "Избранные материалы";case "Файлы":return "Файлов пока нет";
            case "Ссылки":return "Ссылок пока нет";case "Голосовые":return "Голосовых сообщений пока нет";
            default:return "GIF пока нет";}
    }
    private String profileName(){return currentProfile!=null&&!empty(currentProfile.getName())?currentProfile.getName():currentChat.getTitle();}
    private String formatPhone(String phone){
        String digits=phone==null?"":phone.replaceAll("\\D","");
        if(digits.length()==11&&digits.startsWith("8"))digits="7"+digits.substring(1);
        if(digits.length()==10)digits="7"+digits;
        if(digits.length()==11&&digits.startsWith("7"))return "+7 ("+digits.substring(1,4)+") "+digits.substring(4,7)+"-"+digits.substring(7,9)+"-"+digits.substring(9,11);
        return phone;
    }
    private String calculateAge(String birthday){
        try{
            String[] parts=birthday.split("\\.");
            if(parts.length!=3)return "";
            int day=Integer.parseInt(parts[0]),month=Integer.parseInt(parts[1]),year=Integer.parseInt(parts[2]);
            java.util.Calendar birth=new java.util.GregorianCalendar(year,month-1,day);
            java.util.Calendar now=java.util.Calendar.getInstance();
            if(birth.after(now))return "";
            int age=now.get(java.util.Calendar.YEAR)-year;
            if(now.get(java.util.Calendar.DAY_OF_YEAR)<birth.get(java.util.Calendar.DAY_OF_YEAR))age--;
            return age>=0?String.valueOf(age):"";
        }catch(Exception ignored){return "";}
    }
    private TextView label(String value,int color,int size,boolean bold){
        TextView v=new TextView(getContext());v.setText(value==null?"":value);v.setTextColor(color);v.setTextSize(size);
        if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;
    }
    private GradientDrawable round(int color,int radius){
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(radius);return d;
    }
    private LinearLayout.LayoutParams wrap(){return new LinearLayout.LayoutParams(-1,-2);}
    private String first(String value){return value==null||value.isEmpty()?"?":value.substring(0,1).toUpperCase();}
    private boolean empty(String value){return value==null||value.trim().isEmpty();}
    private int statusBarHeight(){
        int id=getResources().getIdentifier("status_bar_height","dimen","android");
        return id>0?getResources().getDimensionPixelSize(id):dp(24);
    }
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    @Override protected void onDetachedFromWindow(){
        if(dialog.isShowing())dialog.dismiss();
        super.onDetachedFromWindow();
    }
}
