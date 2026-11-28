package com.dikster.specialgram;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;

/** Native Java settings pages; Compose only supplies live account/preferences state. */
public final class NativeSettingsPanelView extends LinearLayout {
    public interface Listener {
        void onSaveMyProfile(String firstName,String lastName,String bio);
        void onPickProfilePhoto();
        void onDismissOperationError();
        void onPrivacyLastSeen(String value);
        void onPrivacyPhone(String value);
        void onPrivacyPhoto(String value);
        void onTheme(String value);
        void onGlass(float value);
        void onFontScale(float value);
        void onAccent(String value);
        void onBubbleRadius(float value);
        void onWallpaper(String value);
        void onCompactChats(boolean value);
        void onPickHeaderWallpaper();
        void onClearHeaderWallpaper();
        void onSelectCosmicCover();
    }

    private static final String[] THEMES={"Telegram","Midnight","Aurora","Rose","Космос"};
    private static final String[] ACCENTS={"По теме","Синий","Фиолетовый","Мятный","Розовый"};
    private static final String[] WALLPAPERS={"По теме","Глубокая ночь","Океан","Сумерки"};
    private final LinearLayout root;
    private final LinearLayout body;
    private final ScrollView scroll;
    private final OnBackPressedCallback backCallback;
    private final ComponentActivity hostActivity;
    private boolean backRegistered;
    private Listener listener;
    private TgAccount account;
    private String operationError,privacyLastSeen,privacyPhone,privacyPhoto;
    private String themeName,accentName,wallpaperName,headerWallpaper;
    private float glass,fontScale,bubbleRadius;
    private boolean compactChats;
    private int backgroundColor,panelColor,textColor,mutedColor,accentColor,panelLightColor;
    private String page="main",renderKey="",lastShownError="";

    public NativeSettingsPanelView(Context context,TgAccount account,String operationError,
            String privacyLastSeen,String privacyPhone,String privacyPhoto,String themeName,
            float glass,float fontScale,String accentName,float bubbleRadius,String wallpaperName,
            boolean compactChats,String headerWallpaper,int background,int panel,int text,int muted,
            int accent,int panelLight,Listener listener){
        super(context);this.listener=listener;
        setOrientation(VERTICAL);setBackgroundColor(background);
        root=new LinearLayout(context);root.setOrientation(VERTICAL);
        addView(root,new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
        scroll=new ScrollView(context);scroll.setFillViewport(true);
        body=new LinearLayout(context);body.setOrientation(VERTICAL);
        body.setPadding(dp(16),0,dp(16),dp(18));scroll.addView(body);
        backCallback=new OnBackPressedCallback(false){
            @Override public void handleOnBackPressed(){goBack();}
        };
        Activity activity=activity(context);
        hostActivity=activity instanceof ComponentActivity?(ComponentActivity)activity:null;
        update(account,operationError,privacyLastSeen,privacyPhone,privacyPhoto,themeName,
                glass,fontScale,accentName,bubbleRadius,wallpaperName,compactChats,
                headerWallpaper,background,panel,text,muted,accent,panelLight,listener);
    }

    public void update(TgAccount account,String error,String lastSeen,String phonePrivacy,
            String photoPrivacy,String theme,float glass,float fontScale,String accentName,
            float radius,String wallpaper,boolean compact,String headerWallpaper,int background,
            int panel,int text,int muted,int accent,int panelLight,Listener callback){
        listener=callback;this.account=account;operationError=error;
        privacyLastSeen=lastSeen;privacyPhone=phonePrivacy;privacyPhoto=photoPrivacy;
        themeName=theme;this.glass=glass;this.fontScale=fontScale;this.accentName=accentName;
        bubbleRadius=radius;wallpaperName=wallpaper;compactChats=compact;
        this.headerWallpaper=headerWallpaper;backgroundColor=background;panelColor=panel;
        textColor=text;mutedColor=muted;accentColor=accent;panelLightColor=panelLight;
        setBackgroundColor(backgroundColor);
        String key=page+"|"+account+"|"+error+"|"+lastSeen+"|"+phonePrivacy+"|"+photoPrivacy
                +"|"+theme+"|"+glass+"|"+fontScale+"|"+accentName+"|"+radius+"|"+wallpaper
                +"|"+compact+"|"+headerWallpaper+"|"+background+"|"+panel+"|"+text+"|"+muted
                +"|"+accent+"|"+panelLight;
        if(!key.equals(renderKey)){renderKey=key;render();}
        if(error!=null&&!error.isEmpty()&&!error.equals(lastShownError)){
            lastShownError=error;
            new AlertDialog.Builder(getContext()).setTitle("Не удалось выполнить действие")
                    .setMessage(error).setPositiveButton("Понятно",(d,w)->{
                        if(listener!=null)listener.onDismissOperationError();
                    }).setOnCancelListener(d->{if(listener!=null)listener.onDismissOperationError();}).show();
        }
        if(error==null||error.isEmpty())lastShownError="";
        backCallback.setEnabled(!"main".equals(page));
    }

    @Override protected void onAttachedToWindow(){
        super.onAttachedToWindow();
        if(hostActivity!=null&&!backRegistered){
            hostActivity.getOnBackPressedDispatcher().addCallback(hostActivity,backCallback);
            backRegistered=true;
        }
    }

    @Override protected void onDetachedFromWindow(){
        if(backRegistered){backCallback.remove();backRegistered=false;}
        super.onDetachedFromWindow();
    }

    private void render(){
        root.removeAllViews();body.removeAllViews();
        if(!"main".equals(page)){
            LinearLayout header=new LinearLayout(getContext());header.setGravity(Gravity.CENTER_VERTICAL);
            header.setPadding(dp(14),dp(11),dp(14),dp(11));header.setBackgroundColor(panelColor);
            TextView back=text("‹",accentColor,30,false);header.addView(back);
            TextView title=text(pageTitle(),textColor,18,true);
            LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,LayoutParams.WRAP_CONTENT,1f);
            tp.leftMargin=dp(12);header.addView(title,tp);
            header.setOnClickListener(v->goBack());root.addView(header);
        }
        root.addView(scroll,new LayoutParams(LayoutParams.MATCH_PARENT,0,1f));
        switch(page){
            case "main":renderMain();break;
            case "appearance":renderAppearance();break;
            case "cover":renderCover();break;
            case "privacy":renderPrivacy();break;
            case "account":renderAccount();break;
            case "notifications":renderNotifications();break;
            case "storage":renderStorage();break;
            case "diagnostics":renderDiagnostics();break;
            default:renderAbout();break;
        }
        backCallback.setEnabled(!"main".equals(page));
    }

    private void renderMain(){
        LinearLayout accountRow=new LinearLayout(getContext());accountRow.setGravity(Gravity.CENTER_VERTICAL);
        accountRow.setPadding(0,dp(17),0,dp(17));
        String name=account==null?"Мой аккаунт Telegram":account.getName();
        NativeAvatarView avatar=new NativeAvatarView(getContext(),account==null?"":account.getAvatarPath(),
                first(name),0,60,false,false,false,panelLightColor);
        accountRow.addView(avatar,new LayoutParams(dp(60),dp(60)));
        LinearLayout labels=vertical();LinearLayout.LayoutParams lp=new LayoutParams(0,LayoutParams.WRAP_CONTENT,1f);
        lp.leftMargin=dp(14);accountRow.addView(labels,lp);
        labels.addView(text(name,textColor,18,true));
        String username=account==null?"":account.getUsername();
        labels.addView(text(empty(username)?"Подключён через TDLib":"@"+username,mutedColor,13,false));
        if(account!=null&&account.isPremium())labels.addView(text("✦ Telegram Premium",0xFFB58CFF,12,true));
        body.addView(accountRow);divider();
        navigation("Внешний вид","Темы, обложка и оформление","appearance");
        navigation("Аккаунт","Номер телефона и профиль","account");
        navigation("Уведомления","Звуки и оповещения","notifications");
        navigation("Конфиденциальность","Последний визит и безопасность","privacy");
        navigation("Данные и память","Сеть и хранилище","storage");
        navigation("Диагностика","Журнал технических ошибок TDLib","diagnostics");
        navigation("О приложении","Specialgram · 0.3","about");
        section("Авторская функция");
        paragraph("Постепенную отправку текста можно включить отдельно в меню каждого чата.");
    }

    private void renderAppearance(){
        section("SPECIALGRAM · ВНЕШНИЙ ВИД");
        heading("Цветовая тема");chips(THEMES,themeName,value->{if(listener!=null)listener.onTheme(value);});
        heading("Акцентный цвет");chips(ACCENTS,accentName,value->{if(listener!=null)listener.onAccent(value);});
        slider("Эффект стекла","Прозрачность панелей и карточек",glass,0f,1f,"%",v->{if(listener!=null)listener.onGlass(v);});
        preview();
        navigation("Обложка Specialgram","Стандартная, космическая или своя фотография","cover");
        heading("Фон переписки");chips(WALLPAPERS,wallpaperName,value->{if(listener!=null)listener.onWallpaper(value);});
        toggle("Компактный список чатов","Больше диалогов на экране",compactChats,
                value->{if(listener!=null)listener.onCompactChats(value);});
        slider("Скругление сообщений","Форма пузырей в переписке",bubbleRadius,8f,28f," dp",
                v->{if(listener!=null)listener.onBubbleRadius(v);});
        slider("Размер текста","Масштаб интерфейса",fontScale,0.85f,1.25f,"%",
                v->{if(listener!=null)listener.onFontScale(v);});
    }

    private void renderCover(){
        paragraph("Выбери, что будет отображаться в верхней части списка чатов.");
        cover("Стандартная","Обычный фон Telegram",headerWallpaper==null,
                ()->{if(listener!=null)listener.onClearHeaderWallpaper();});
        cover("Космос","Градиент со звёздами","preset:cosmic".equals(headerWallpaper),
                ()->{if(listener!=null)listener.onSelectCosmicCover();});
        cover("Моя фотография","Выбрать изображение из галереи",
                headerWallpaper!=null&&!"preset:cosmic".equals(headerWallpaper),
                ()->{if(listener!=null)listener.onPickHeaderWallpaper();});
    }

    private void renderPrivacy(){
        paragraph("Настройки применяются к аккаунту Telegram и синхронизируются с другими клиентами.");
        body.addView(NativeSettingsRows.privacy(getContext(),
                "Кто видит время последнего посещения и статус «в сети»",privacyLastSeen,
                textColor,mutedColor,value->{if(listener!=null)listener.onPrivacyLastSeen(value);}));
        divider();
        body.addView(NativeSettingsRows.privacy(getContext(),"Кто видит мой номер телефона",
                privacyPhone,textColor,mutedColor,value->{if(listener!=null)listener.onPrivacyPhone(value);}));
        divider();
        body.addView(NativeSettingsRows.privacy(getContext(),"Кто видит мою фотографию профиля",
                privacyPhoto,textColor,mutedColor,value->{if(listener!=null)listener.onPrivacyPhoto(value);}));
        paragraph("Если в Telegram настроены исключения для отдельных людей, они будут обозначены как «Особые настройки». Выбор выше заменит такие исключения общим правилом.");
    }

    private void renderAccount(){
        String name=account==null?"Мой аккаунт":account.getName();
        LinearLayout row=new LinearLayout(getContext());row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0,dp(16),0,dp(12));
        NativeAvatarView avatar=new NativeAvatarView(getContext(),account==null?"":account.getAvatarPath(),
                first(name),0,76,false,false,false,panelLightColor);
        row.addView(avatar,new LayoutParams(dp(76),dp(76)));
        LinearLayout labels=vertical();LinearLayout.LayoutParams lp=new LayoutParams(0,LayoutParams.WRAP_CONTENT,1f);
        lp.leftMargin=dp(14);row.addView(labels,lp);
        labels.addView(text(name,textColor,18,true));
        String username=account==null?"":account.getUsername();
        labels.addView(text(empty(username)?"Имя пользователя не задано":"@"+username,mutedColor,13,false));
        if(account!=null&&account.isPremium())labels.addView(text("✦ Telegram Premium",0xFFB58CFF,12,true));
        body.addView(row);
        Button photo=button("Изменить фотографию профиля",()->{if(listener!=null)listener.onPickProfilePhoto();});
        body.addView(photo,match());
        EditText first=edit(account==null?"":account.getFirstName(),"Имя",true);
        EditText last=edit(account==null?"":account.getLastName(),"Фамилия",true);
        EditText bio=edit(account==null?"":account.getBio(),"О себе",false);
        bio.setMinLines(2);bio.setMaxLines(3);bio.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(70)});
        body.addView(first,match());body.addView(last,match());body.addView(bio,match());
        Button save=button("Сохранить изменения",()->{
            String firstName=first.getText().toString().trim();
            if(firstName.isEmpty()){first.setError("Укажите имя");return;}
            if(listener!=null)listener.onSaveMyProfile(firstName,last.getText().toString().trim(),
                    bio.getText().toString().replace("\n",""));
        });
        body.addView(save,match());
        String phone=account==null?"Загружаем…":empty(account.getPhone())?"не указан":account.getPhone();
        paragraph("Телефон: "+phone);
        paragraph("Изменения отправляются непосредственно в Telegram.");
    }

    private void renderNotifications(){
        heading("Уведомления Specialgram");
        paragraph("Управление разрешением, звуком и отображением уведомлений выполняется Android. Уведомления чатов синхронизируются с Telegram.");
        navigation("Системные настройки уведомлений","Разрешения, звук, экран блокировки",()->{
            try{
                Intent intent;
                if(Build.VERSION.SDK_INT>=26)intent=new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE,getContext().getPackageName());
                else intent=new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:"+getContext().getPackageName()));
                getContext().startActivity(intent);
            }catch(RuntimeException e){toast("Не удалось открыть настройки Android");}
        });
        paragraph("Если уведомления отключены в Android, включи их для Specialgram. Отдельные настройки чатов остаются на стороне Telegram.");
    }

    private void renderStorage(){
        heading("Данные и память");
        paragraph("Локальное хранилище TDLib и временные файлы приложения.");
        long media=MediaPreparation.directoryBytes(new java.io.File(getContext().getFilesDir(),"tdlib-files"));
        long cache=MediaPreparation.directoryBytes(getContext().getCacheDir());
        body.addView(NativeSettingsRows.setting(getContext(),"Медиа и файлы Telegram",
                MediaPreparation.formatFileSize(media),textColor,mutedColor));
        body.addView(NativeSettingsRows.setting(getContext(),"Временные файлы",
                MediaPreparation.formatFileSize(cache),textColor,mutedColor));
        navigation("Открыть хранилище Android","Просмотр размера и очистка данных приложения",()->{
            try{getContext().startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:"+getContext().getPackageName())));}
            catch(RuntimeException e){toast("Не удалось открыть настройки Android");}
        });
        paragraph("Очистка данных Android удалит локальную базу и может потребовать повторного входа. Не очищай данные приложения, если не хочешь заново авторизоваться.");
    }

    private void renderDiagnostics(){
        heading("Журнал ошибок");
        paragraph("Здесь сохраняются технические ошибки TDLib: название запроса, код и время. Переписки, имена, номера телефонов и идентификаторы чатов не записываются.");
        String journal=DiagnosticsJournal.read(getContext());
        TextView log=text(empty(journal)?"Ошибок пока нет. Если появится Too Many Requests, запись появится здесь.":journal,textColor,12,false);
        log.setPadding(dp(12),dp(12),dp(12),dp(12));log.setBackground(round(panelColor,12));
        body.addView(log,match());
        body.addView(button("Скопировать журнал",()->{
            String value=DiagnosticsJournal.read(getContext());
            if(empty(value)){toast("Журнал пока пуст");return;}
            ClipboardManager clipboard=(ClipboardManager)getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("Specialgram diagnostics",value));
            toast("Журнал скопирован");
        }),match());
        body.addView(button("Экспортировать журнал",()->{
            String value=DiagnosticsJournal.read(getContext());
            if(empty(value)){toast("Журнал пока пуст");return;}
            Intent intent=new Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_SUBJECT,"Specialgram — журнал диагностики")
                    .putExtra(Intent.EXTRA_TEXT,value);
            getContext().startActivity(Intent.createChooser(intent,"Экспортировать журнал"));
        }),match());
        paragraph("Журнал хранится только в памяти приложения. Перед отправкой можешь просмотреть его содержимое.");
    }

    private void renderAbout(){
        heading("О приложении");
        paragraph("Specialgram — клиент Telegram на базе TDLib.\n\nЗащищённая диагностика: приложение отправляет технические отчёты о сбоях и ошибках TDLib. Тексты сообщений, переписки, контакты и номер телефона в отчёты не включаются.");
    }

    private void preview(){
        LinearLayout card=vertical();card.setPadding(dp(14),dp(13),dp(14),dp(13));
        card.setBackground(round(withAlpha(panelColor,1f-glass*0.30f),18));
        card.addView(text("Предпросмотр Specialgram",textColor,15,true));
        card.addView(text("Так будут выглядеть панели приложения",mutedColor,12,false));
        LinearLayout inner=new LinearLayout(getContext());inner.setGravity(Gravity.CENTER_VERTICAL);
        inner.setPadding(dp(12),dp(12),dp(12),dp(12));inner.setBackground(round(withAlpha(panelLightColor,1f-glass*0.38f),14));
        TextView icon=text("A",Color.WHITE,15,true);icon.setGravity(Gravity.CENTER);
        icon.setBackground(round(accentColor,30));inner.addView(icon,new LayoutParams(dp(34),dp(34)));
        LinearLayout labels=vertical();LinearLayout.LayoutParams lp=new LayoutParams(0,LayoutParams.WRAP_CONTENT,1f);
        lp.leftMargin=dp(10);inner.addView(labels,lp);
        labels.addView(text("Specialgram",textColor,13,true));
        labels.addView(text("Пример полупрозрачной карточки",mutedColor,11,false));
        inner.addView(text("✓✓",accentColor,12,true));
        LinearLayout.LayoutParams ip=match();ip.topMargin=dp(10);card.addView(inner,ip);
        LinearLayout.LayoutParams cp=match();cp.topMargin=dp(8);cp.bottomMargin=dp(10);body.addView(card,cp);
    }

    private void chips(String[] values,String selected,java.util.function.Consumer<String> action){
        HorizontalScrollView horizontal=new HorizontalScrollView(getContext());horizontal.setHorizontalScrollBarEnabled(false);
        LinearLayout row=new LinearLayout(getContext());row.setGravity(Gravity.CENTER_VERTICAL);
        for(String value:values){
            TextView chip=text(value,textColor,13,true);chip.setPadding(dp(13),dp(9),dp(13),dp(9));
            chip.setBackground(round(value.equals(selected)?withAlpha(accentColor,0.24f):panelColor,22));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
            p.rightMargin=dp(7);row.addView(chip,p);chip.setOnClickListener(v->action.accept(value));
        }
        horizontal.addView(row);LinearLayout.LayoutParams p=match();p.topMargin=dp(7);p.bottomMargin=dp(8);body.addView(horizontal,p);
    }

    private void slider(String title,String subtitle,float value,float min,float max,String suffix,
                        java.util.function.Consumer<Float> action){
        LinearLayout labels=new LinearLayout(getContext());labels.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout texts=vertical();texts.addView(text(title,textColor,15,true));
        texts.addView(text(subtitle,mutedColor,12,false));
        labels.addView(texts,new LayoutParams(0,LayoutParams.WRAP_CONTENT,1f));
        labels.addView(text(formatValue(value,min,max,suffix),accentColor,13,true));body.addView(labels);
        SeekBar seek=new SeekBar(getContext());seek.setMax(1000);
        seek.setProgress(Math.round((value-min)/(max-min)*1000f));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override public void onProgressChanged(SeekBar bar,int progress,boolean fromUser){}
            @Override public void onStartTrackingTouch(SeekBar bar){}
            @Override public void onStopTrackingTouch(SeekBar bar){
                float next=min+(max-min)*bar.getProgress()/1000f;
                action.accept(next);
            }
        });
        body.addView(seek,match());
    }

    private void toggle(String title,String subtitle,boolean checked,java.util.function.Consumer<Boolean> action){
        LinearLayout row=new LinearLayout(getContext());row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout labels=vertical();labels.addView(text(title,textColor,15,true));
        labels.addView(text(subtitle,mutedColor,12,false));
        row.addView(labels,new LayoutParams(0,LayoutParams.WRAP_CONTENT,1f));
        Switch sw=new Switch(getContext());sw.setChecked(checked);sw.setOnCheckedChangeListener((v,c)->action.accept(c));
        row.addView(sw);body.addView(row,new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,dp(58)));
    }

    private void navigation(String title,String subtitle,String target){
        navigation(title,subtitle,()->{page=target;renderKey="";render();});
    }
    private void navigation(String title,String subtitle,Runnable click){
        body.addView(NativeSettingsRows.navigation(getContext(),title,subtitle,textColor,mutedColor,click));
    }
    private void cover(String title,String subtitle,boolean selected,Runnable click){
        body.addView(NativeSettingsRows.coverChoice(getContext(),title,subtitle,selected,
                textColor,mutedColor,click));
    }
    private void section(String value){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);
        p.topMargin=dp(14);p.bottomMargin=dp(6);TextView v=text(value,accentColor,14,true);body.addView(v,p);
    }
    private void heading(String value){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);
        p.topMargin=dp(14);p.bottomMargin=dp(5);body.addView(text(value,textColor,16,true),p);
    }
    private void paragraph(String value){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);
        p.topMargin=dp(8);p.bottomMargin=dp(10);TextView v=text(value,mutedColor,13,false);
        v.setLineSpacing(dp(3),1f);body.addView(v,p);
    }
    private void divider(){
        View line=new View(getContext());line.setBackgroundColor(withAlpha(mutedColor,0.18f));
        body.addView(line,new LayoutParams(LayoutParams.MATCH_PARENT,dp(1)));
    }
    private Button button(String value,Runnable action){
        Button button=new Button(getContext());button.setText(value);
        button.setOnClickListener(v->{if(action!=null)action.run();});return button;
    }
    private EditText edit(String value,String hint,boolean single){
        EditText input=new EditText(getContext());input.setText(value);input.setHint(hint);
        input.setTextColor(textColor);input.setHintTextColor(mutedColor);input.setTextSize(15);
        input.setSingleLine(single);return input;
    }
    private LinearLayout vertical(){LinearLayout v=new LinearLayout(getContext());v.setOrientation(VERTICAL);return v;}
    private TextView text(String value,int color,int size,boolean bold){
        TextView v=new TextView(getContext());v.setText(value==null?"":value);v.setTextColor(color);v.setTextSize(size);
        if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;
    }
    private GradientDrawable round(int color,int radius){
        android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();
        d.setColor(color);d.setCornerRadius(dp(radius));return d;
    }
    private int withAlpha(int color,float alpha){return (color&0x00FFFFFF)|(Math.max(0,Math.min(255,Math.round(alpha*255)))<<24);}
    private String pageTitle(){
        switch(page){
            case "cover":return "Обложка Specialgram";case "appearance":return "Внешний вид";
            case "account":return "Аккаунт";case "notifications":return "Уведомления";
            case "privacy":return "Конфиденциальность";case "storage":return "Данные и память";
            case "diagnostics":return "Диагностика";default:return "О приложении";
        }
    }
    private void goBack(){
        if("cover".equals(page))page="appearance";else page="main";
        renderKey="";render();backCallback.setEnabled(!"main".equals(page));
    }
    private String formatValue(float value,float min,float max,String suffix){
        if("%".equals(suffix))return Math.round(value*100)+"%";
        if(" dp".equals(suffix))return Math.round(value)+suffix;
        return String.format(java.util.Locale.getDefault(),"%.2f",value);
    }
    private String first(String value){return value==null||value.isEmpty()?"?":value.substring(0,1).toUpperCase();}
    private boolean empty(String value){return value==null||value.trim().isEmpty();}
    private void toast(String value){Toast.makeText(getContext(),value,Toast.LENGTH_SHORT).show();}
    private LayoutParams match(){return new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private static Activity activity(Context context){
        Context current=context;
        while(current instanceof ContextWrapper){
            if(current instanceof Activity)return (Activity)current;
            current=((ContextWrapper)current).getBaseContext();
        }
        return current instanceof Activity?(Activity)current:null;
    }
}
