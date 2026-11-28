package com.dikster.specialgram;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;
import org.drinkless.tdlib.TdApi;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.ViewCompat;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Native Android application shell. Chat list, folders, stories, settings and
 * conversation controls are rendered by the Java Android Views coordinators.
 */
public final class MainActivity extends Activity {
    private static final int PICK_PROFILE=11, PICK_HEADER=12, PICK_CHAT_PHOTO=13,
            PICK_DOCUMENT=14, PICK_STORY_PHOTO=15, PICK_STORY_VIDEO=16, CAPTURE_VIDEO=17;
    private static final int BG=0xFF0E1621, PANEL=0xFF17212B, PANEL_LIGHT=0xFF202B36,
            TEXT=0xFFEAF2F8, MUTED=0xFF8A9AA9, ACCENT=0xFF2AABEE,
            INCOMING=0xFF182533, OUTGOING=0xFF2B5278;

    private final Handler handler=new Handler(Looper.getMainLooper());
    private SpecialgramApp app;
    private TdLibClient td;
    private FrameLayout root;
    private NativeChatsScreenView chatsView;
    private NativeConversationScreenView conversationView;
    private TgChat selectedChat;
    private String phone="",code="",password="",query="",contactsQuery="";
    private String themeName="Telegram",accentName="По теме",wallpaperName="По теме",
            headerWallpaper=null,videoNoteShape="circle";
    private float glass=.35f,fontScale=1f,bubbleRadius=18f;
    private boolean compactChats=false,sortNewest=true;
    private Object[] lastSnapshot;
    private Uri pendingVideoUri;

    private final Runnable refreshTask=new Runnable(){
        @Override public void run(){
            if(isFinishing()||td==null)return;
            Object[] snapshot=snapshot();
            if(!sameSnapshot(snapshot,lastSnapshot)){
                lastSnapshot=snapshot;
                render(snapshot);
            }
            handler.postDelayed(this,250L);
        }
    };

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        app=(SpecialgramApp)getApplication();
        td=app.getTd();
        Window window=getWindow();
        WindowCompat.setDecorFitsSystemWindows(window,false);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(BG);
        WindowCompat.getInsetsController(window,window.getDecorView()).setAppearanceLightStatusBars(false);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        loadPreferences();
        root=new FrameLayout(this);
        root.setBackgroundColor(BG);
        ViewCompat.setOnApplyWindowInsetsListener(root,(view,insets)->{
            int top=insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int bottom=insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            view.setPadding(0,top,0,Math.max(0,bottom/3));
            return insets;
        });
        setContentView(root);
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                !=android.content.pm.PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},1001);
        handler.post(refreshTask);
    }

    private void loadPreferences(){
        android.content.SharedPreferences p=getSharedPreferences("appearance",MODE_PRIVATE);
        themeName=p.getString("theme","Telegram");glass=p.getFloat("glass",.35f);
        fontScale=p.getFloat("fontScale",1f);accentName=p.getString("accent","По теме");
        bubbleRadius=p.getFloat("bubbleRadius",18f);wallpaperName=p.getString("wallpaper","По теме");
        compactChats=p.getBoolean("compactChats",false);headerWallpaper=p.getString("headerWallpaper",null);
        videoNoteShape=p.getString("videoNoteShape","circle");
    }
    private void savePreferences(){
        getSharedPreferences("appearance",MODE_PRIVATE).edit().putString("theme",themeName)
                .putFloat("glass",glass).putFloat("fontScale",fontScale).putString("accent",accentName)
                .putFloat("bubbleRadius",bubbleRadius).putString("wallpaper",wallpaperName)
                .putBoolean("compactChats",compactChats).putString("headerWallpaper",headerWallpaper)
                .putString("videoNoteShape",videoNoteShape).apply();
    }

    private Object[] snapshot(){
        return new Object[]{td.getAuth().getValue(),td.getChats().getValue(),td.getContacts().getValue(),
                td.getStories().getValue(),td.getMessages().getValue(),td.getSearchResults().getValue(),
                td.getProfile().getValue(),td.getAccount().getValue(),td.getPrivacyLastSeen().getValue(),
                td.getPrivacyPhone().getValue(),td.getPrivacyPhoto().getValue(),td.getOperationError().getValue()};
    }
    private boolean sameSnapshot(Object[] a,Object[] b){
        if(a==null||b==null)return false;
        for(int i=0;i<a.length;i++)if(a[i]!=b[i])return false;
        return true;
    }

    @SuppressWarnings("unchecked")
    private void render(Object[] s){
        Auth auth=(Auth)s[0];
        if(auth==Auth.Loading){showLoading("Подключаемся к Telegram…","Пожалуйста, подождите");return;}
        if(auth==Auth.Phone){showAuth("Ваш номер телефона","Введите номер в международном формате",phone,"Продолжить",false,()->{td.clearOperationError();td.phone(phone);},v->phone=v,(String)s[11]);return;}
        if(auth==Auth.Code){showAuth("Код подтверждения","Мы отправили код в Telegram или SMS",code,"Подтвердить",false,()->{td.clearOperationError();td.code(code);},v->code=v,(String)s[11]);return;}
        if(auth==Auth.Password){showAuth("Облачный пароль","Введите пароль двухэтапной проверки",password,"Войти",true,()->{td.clearOperationError();td.password(password);},v->password=v,(String)s[11]);return;}
        if(auth instanceof Auth.Error){showLoading("Не удалось подключиться",((Auth.Error)auth).getText());return;}
        if(auth!=Auth.Ready)return;

        List<TgChat> chats=(List<TgChat>)s[1];
        List<TgContact> contacts=(List<TgContact>)s[2];
        List<TgStory> stories=(List<TgStory>)s[3];
        List<TgMessage> messages=(List<TgMessage>)s[4];
        List<TgMessage> search=(List<TgMessage>)s[5];
        TgProfile profile=(TgProfile)s[6];
        TgAccount account=(TgAccount)s[7];
        String privacyLast=(String)s[8],privacyPhone=(String)s[9],privacyPhoto=(String)s[10];
        String error=(String)s[11];
        if(selectedChat==null){
            conversationView=null;
            int screenBg=screenBackground(),screenPanel=screenPanel(),screenPanelLight=screenPanelLight();
            root.setBackgroundColor(screenBg);
            getWindow().setNavigationBarColor(screenBg);
            androidx.core.view.WindowInsetsControllerCompat bars=
                    WindowCompat.getInsetsController(getWindow(),getWindow().getDecorView());
            bars.setAppearanceLightStatusBars(isLightTheme());
            bars.setAppearanceLightNavigationBars(isLightTheme());
            NativeChatsScreenView.State state=new NativeChatsScreenView.State(chats,contacts,stories,
                    query,contactsQuery,sortNewest,compactChats,account,error,privacyLast,privacyPhone,
                    privacyPhoto,themeName,glass,fontScale,accentName,bubbleRadius,wallpaperName,
                    headerWallpaper,screenBg,screenPanel,screenText(),screenMuted(),ACCENT,screenPanelLight);
            if(chatsView==null){
                chatsView=new NativeChatsScreenView(this,state,chatListener());
                setScreenWithTransition(chatsView, false);
            }else chatsView.update(state,chatListener());
        }else{
            chatsView=null;
            TgChat current=findChat(chats,selectedChat.getId());
            if(current!=null)selectedChat=current;
            int basePanel=screenPanel(),basePanelLight=screenPanelLight();
            int panel=(basePanel&0x00FFFFFF)|(((int)(255*(1f-glass*.30f)))<<24);
            int panelLight=(basePanelLight&0x00FFFFFF)|(((int)(255*(1f-glass*.38f)))<<24);
            if(conversationView==null){
                conversationView=new NativeConversationScreenView(this,selectedChat,chats,messages,search,
                        profile,videoNoteShape,error,screenBackground(),chatBackground(),panel,panelLight,screenText(),screenMuted(),
                        accentColor(),INCOMING,OUTGOING,bubbleRadius,conversationListener());
                setScreenWithTransition(conversationView, true);
            }else conversationView.update(selectedChat,chats,messages,search,profile,videoNoteShape,error,
                    screenBackground(),chatBackground(),panel,panelLight,screenText(),screenMuted(),accentColor(),INCOMING,OUTGOING,
                    bubbleRadius,conversationListener());
        }
    }

    /**
     * Consistent lightweight screen transition. The incoming screen is faded and
     * translated a few dp; animations are hardware-friendly and short enough not
     * to delay navigation or interfere with chat scrolling.
     */
    private void setScreenWithTransition(View screen, boolean fromRight){
        if(screen==null)return;
        screen.setAlpha(0f);
        screen.setTranslationX((fromRight?18f:-12f)*getResources().getDisplayMetrics().density);
        root.removeAllViews();
        root.addView(screen,new FrameLayout.LayoutParams(-1,-1));
        screen.animate().cancel();
        screen.animate().alpha(1f).translationX(0f).setDuration(220L)
                .setInterpolator(new android.view.animation.DecelerateInterpolator(1.6f))
                .start();
    }

    private boolean isLightTheme(){return "Светлая".equalsIgnoreCase(themeName);}
    private int screenBackground(){
        if("Midnight".equalsIgnoreCase(themeName))return 0xFF000000;
        return isLightTheme()?0xFFF1F3F5:BG;
    }
    private int screenPanel(){
        if("Midnight".equalsIgnoreCase(themeName))return 0xFF090909;
        return isLightTheme()?0xFFFFFFFF:PANEL;
    }
    private int screenPanelLight(){
        if("Midnight".equalsIgnoreCase(themeName))return 0xFF151515;
        return isLightTheme()?0xFFE7EBEF:PANEL_LIGHT;
    }
    private int screenText(){return isLightTheme()?0xFF17212B:TEXT;}
    private int screenMuted(){return isLightTheme()?0xFF6B7785:MUTED;}

    private int accentColor(){
        switch(accentName){case "Фиолетовый":return 0xFF9B82FF;case "Мятный":return 0xFF42D6B5;
            case "Розовый":return 0xFFFF78B7;default:return ACCENT;}
    }
    private int chatBackground(){
        switch(wallpaperName){case "Глубокая ночь":return 0xFF080F1A;case "Океан":return 0xFF0A2028;
            case "Сумерки":return 0xFF211629;default:return screenBackground();}
    }
    private TgChat findChat(List<TgChat> chats,long id){
        for(TgChat c:chats)if(c.getId()==id)return c;return null;
    }

    private void showLoading(String title,String subtitle){
        chatsView=null;conversationView=null;
        setScreenWithTransition(NativeEntryPanel.loading(this,title,subtitle,BG,TEXT,MUTED,ACCENT), false);
    }
    private void showAuth(String title,String subtitle,String value,String button,boolean secret,
                          Runnable submit,java.util.function.Consumer<String> changed,String error){
        chatsView=null;conversationView=null;
        View old=root.getChildCount()>0?root.getChildAt(0):null;
        if(old instanceof NativeEntryPanel){
            ((NativeEntryPanel)old).update(title,subtitle,value,button,secret,BG,TEXT,MUTED,ACCENT,
                    changed,submit,error);return;
        }
        setScreenWithTransition(new NativeEntryPanel(this,title,subtitle,value,button,secret,BG,TEXT,MUTED,
                ACCENT,changed,submit,error), false);
    }

    private NativeChatsScreenView.Listener chatListener(){
        return new NativeChatsScreenView.Listener(){
            @Override public void onOpenStory(TgStory story){td.openStory(story.getChatId(),story.getStoryId());}
            @Override public void onCloseStory(TgStory story){td.closeStory(story.getChatId(),story.getStoryId());}
            @Override public void onReactToStory(TgStory story){td.reactToStory(story.getChatId(),story.getStoryId(),"❤️");}
            @Override public void onPostStory(){pick(PICK_STORY_PHOTO,"image/*");}
            @Override public void onPostVideoStory(){pick(PICK_STORY_VIDEO,"video/*");}
            @Override public void onReplyToStory(TgStory story,String text){td.replyToStory(story.getChatId(),story.getStoryId(),text);}
            @Override public void onQuery(String value){query=value;refreshNow();}
            @Override public void onContactsQuery(String value){contactsQuery=value;refreshNow();}
            @Override public void onSort(boolean value){sortNewest=value;refreshNow();}
            @Override public void onCompactChats(boolean value){compactChats=value;savePreferences();refreshNow();}
            @Override public void onSaveMyProfile(String first,String last,String bio){td.updateMyProfile(first,last,bio);}
            @Override public void onPickProfilePhoto(){pick(PICK_PROFILE,"image/*");}
            @Override public void onDismissOperationError(){td.clearOperationError();}
            @Override public void onPrivacyLastSeen(String value){td.setLastSeenPrivacy(value);}
            @Override public void onPrivacyPhone(String value){td.setPhonePrivacy(value);}
            @Override public void onPrivacyPhoto(String value){td.setPhotoPrivacy(value);}
            @Override public void onTheme(String value){themeName=value;savePreferences();refreshNow();}
            @Override public void onGlass(float value){glass=value;savePreferences();refreshNow();}
            @Override public void onFontScale(float value){fontScale=value;savePreferences();refreshNow();}
            @Override public void onAccent(String value){accentName=value;savePreferences();refreshNow();}
            @Override public void onBubbleRadius(float value){bubbleRadius=value;savePreferences();refreshNow();}
            @Override public void onWallpaper(String value){wallpaperName=value;savePreferences();refreshNow();}
            @Override public void onPickHeaderWallpaper(){pick(PICK_HEADER,"image/*");}
            @Override public void onClearHeaderWallpaper(){headerWallpaper=null;savePreferences();refreshNow();}
            @Override public void onSelectCosmicCover(){headerWallpaper="preset:cosmic";savePreferences();refreshNow();}
            @Override public void onOpen(TgChat chat){selectedChat=chat;td.openChat(chat.getId());refreshNow();}
            @Override public void onOpenContact(TgContact contact){
                td.openContact(contact.getId(),new TdContactReadyListener(){
                    @Override public void onReady(TgChat chat){selectedChat=chat;td.openChat(chat.getId());refreshNow();}
                });
            }
            @Override public void onSavedMessages(){
                td.openSavedMessages(chat->{selectedChat=chat;td.openChat(chat.getId());refreshNow();});
            }
            @Override public void onCreateGroup(String title,long[] userIds){
                td.createBasicGroup(title,userIds,chat->{selectedChat=chat;td.openChat(chat.getId());refreshNow();});
            }
            @Override public void onArchive(TgChat chat){td.archiveChat(chat.getId(),!chat.getArchived());}
            @Override public void onTogglePinned(TgChat chat){td.toggleChatPinned(chat.getId(),!chat.isPinned(),chat.getArchived());}
            @Override public void onToggleUnread(TgChat chat){td.toggleChatUnread(chat.getId(),!chat.isMarkedAsUnread());}
            @Override public void onToggleMute(TgChat chat){td.toggleChatMuted(chat.getId(),!chat.isMuted());}
        };
    }

    private NativeConversationScreenView.Listener conversationListener(){
        return new NativeConversationScreenView.Listener(){
            private long chatId(){return selectedChat==null?0L:selectedChat.getId();}
            @Override public void onSearchMessages(String value){td.searchChatMessages(chatId(),value);}
            @Override public void onOpenProfile(){td.openProfile(chatId());}
            @Override public void onCloseProfile(){td.closeProfile();}
            @Override public void onToggleMute(TgChat chat,boolean muted){td.toggleChatMuted(chat.getId(),muted);refreshNow();}
            @Override public void onOpenGiftCatalog(TgProfile profile){
                if(profile==null||profile.getUserId()==0L){
                    Toast.makeText(MainActivity.this,"Подарки доступны в личных профилях",Toast.LENGTH_SHORT).show();return;
                }
                td.loadAvailableGifts(new TdUserActionsController.AvailableGiftsListener(){
                    @Override public void onGifts(List<TgGiftOption> gifts){
                        NativeGiftCatalogDialog.show(MainActivity.this,gifts,screenText(),screenMuted(),accentColor(),gift->
                                td.sendGift(profile.getUserId(),gift.getId(),new TdUserActionsController.Callback(){
                                    @Override public void onResult(TdApi.Object result){
                                        Toast.makeText(MainActivity.this,"Подарок отправлен",Toast.LENGTH_SHORT).show();
                                    }
                                    @Override public void onError(TdApi.Error error){
                                        Toast.makeText(MainActivity.this,error==null?"Не удалось отправить подарок":error.message,Toast.LENGTH_LONG).show();
                                    }
                                }));
                    }
                    @Override public void onError(TdApi.Error error){
                        td.reportOperationError(error==null?"Не удалось загрузить каталог подарков":error.message);
                    }
                });
            }
            @Override public void onLoadProfileSection(TgChat chat,String tab,NativeProfileDialogHost.SectionCallback callback){
                if(chat==null){if(callback!=null)callback.onResult(java.util.Collections.emptyList());return;}
                td.searchProfileSection(chat.getId(),tab,callback::onResult);
            }
            @Override public void onBack(){closeConversation();}
            @Override public void onSend(String value,boolean animated,Long replyId){
                if(animated)td.sendAnimated(chatId(),value,replyId);else td.sendPlain(chatId(),value,replyId);
            }
            @Override public void onDeleteMessage(TgMessage message,boolean everyone){td.deleteMessage(chatId(),message.getId(),everyone);}
            @Override public void onClearHistory(boolean everyone){td.clearHistory(chatId(),everyone);}
            @Override public void onEditMessage(TgMessage message,String value){td.editMessage(chatId(),message.getId(),value);}
            @Override public void onReactToMessage(TgMessage message,String emoji){td.reactToMessage(chatId(),message.getId(),emoji);}
            @Override public void onForwardMessage(TgMessage message,TgChat destination){td.forwardMessage(chatId(),destination.getId(),message.getId());}
            @Override public void onPinMessage(TgMessage message){td.pinMessage(chatId(),message.getId(),!message.isPinned());}
            @Override public void onVideoNoteShape(String shape){videoNoteShape=shape;savePreferences();}
            @Override public void onRecordVideo(){
                try{Intent intent=new Intent(MediaStore.ACTION_VIDEO_CAPTURE);intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT,60);startActivityForResult(intent,CAPTURE_VIDEO);}
                catch(Exception e){td.reportOperationError("Не удалось открыть камеру: "+safeMessage(e));}
            }
            @Override public void onDismissOperationError(){td.clearOperationError();}
            @Override public void onLoadOlderMessages(){td.loadOlderMessages(chatId());}
            @Override public void onSendPhoto(){pick(PICK_CHAT_PHOTO,"image/*");}
            @Override public void onSendDocument(){pick(PICK_DOCUMENT,"*/*");}
        };
    }

    private void closeConversation(){
        td.closeProfile();td.closeChat();selectedChat=null;conversationView=null;lastSnapshot=null;refreshNow();
    }
    private void pick(int request,String mime){
        Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mime);intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try{startActivityForResult(intent,request);}catch(Exception e){td.reportOperationError("Не удалось открыть выбор файла: "+safeMessage(e));}
    }

    @Override protected void onActivityResult(int request,int resultCode,Intent data){
        super.onActivityResult(request,resultCode,data);
        if(resultCode!=RESULT_OK||data==null||data.getData()==null)return;
        Uri uri=data.getData();
        try{getContentResolver().takePersistableUriPermission(uri,data.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}
        try{
            switch(request){
                case PICK_PROFILE:td.setMyProfilePhoto(MediaPreparation.prepareProfilePhoto(this,uri).getAbsolutePath());break;
                case PICK_HEADER:headerWallpaper=uri.toString();savePreferences();lastSnapshot=null;refreshNow();break;
                case PICK_CHAT_PHOTO:
                    if(selectedChat!=null)td.sendPhoto(selectedChat.getId(),MediaPreparation.prepareOutgoingMedia(this,uri,"specialgram_photo",10L*1024L*1024L).getAbsolutePath(),"");break;
                case PICK_DOCUMENT:
                    if(selectedChat!=null)td.sendDocument(selectedChat.getId(),MediaPreparation.prepareOutgoingMedia(this,uri,"specialgram_document",50L*1024L*1024L).getAbsolutePath(),"");break;
                case PICK_STORY_PHOTO:td.postPhotoStory(MediaPreparation.prepareStoryPhoto(this,uri).getAbsolutePath(),"");break;
                case PICK_STORY_VIDEO:
                    TdPair<File,Double> story=MediaPreparation.prepareStoryVideo(this,uri);
                    td.postVideoStory(story.getFirst().getAbsolutePath(),story.getSecond());break;
                case CAPTURE_VIDEO:
                    if(selectedChat!=null)sendCapturedVideo(uri);break;
            }
        }catch(Exception e){td.reportOperationError("Не удалось обработать файл: "+safeMessage(e));}
        lastSnapshot=null;refreshNow();
    }
    private void sendCapturedVideo(Uri uri)throws Exception{
        File target=new File(getCacheDir(),"specialgram_video_"+System.currentTimeMillis()+".mp4");
        try(InputStream input=getContentResolver().openInputStream(uri);
            OutputStream output=new java.io.FileOutputStream(target)){
            if(input==null)throw new IllegalStateException("Камера не вернула видеофайл");
            byte[] buffer=new byte[65536];int n;while((n=input.read(buffer))!=-1)output.write(buffer,0,n);
        }
        td.sendVideo(selectedChat.getId(),target.getAbsolutePath(),videoNoteShape);
    }
    private String safeMessage(Exception e){return e.getMessage()==null?"ошибка файла":e.getMessage();}
    private void refreshNow(){lastSnapshot=null;handler.removeCallbacks(refreshTask);handler.post(refreshTask);}

    @Override public void onBackPressed(){
        if(selectedChat!=null){closeConversation();return;}
        super.onBackPressed();
    }
    @Override protected void onDestroy(){
        handler.removeCallbacks(refreshTask);
        super.onDestroy();
    }
}
