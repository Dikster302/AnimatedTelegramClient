package com.dikster.specialgram;

import org.drinkless.tdlib.TdApi;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Coordinates generation-safe profile loading and profile model updates. */
public final class TdProfileController {
    public interface Listener {
        TgChat findChat(long chatId);
        TgProfile profile();
        void setProfile(TgProfile profile);
        void onPrivateChatUser(long userId, long chatId);
        void onProfileUser(TdApi.User user, long chatId);
    }

    private final TdUserActionsController userActions;
    private final TdProfileSessionController session;
    private final Listener listener;

    public TdProfileController(TdUserActionsController userActions,
                               TdProfileSessionController session,
                               Listener listener) {
        this.userActions = userActions;
        this.session = session;
        this.listener = listener;
    }

    public void openProfile(long chatId) {
        final int generation = session.open();
        userActions.loadProfile(chatId, new TdUserActionsController.ProfileListener() {
            @Override public void onChat(TdApi.Chat chat) {
                if (!session.isCurrent(generation)) return;
                TgChat currentChat = listener.findChat(chatId);
                String avatar = currentChat == null ? "" : currentChat.getAvatarPath();
                listener.setProfile(new TgProfile(chatId, chat.title, TdChatMapper.profileKind(chat.type),
                        "", "", avatar, "", "Статус неизвестен", false, false, false, 0));
                if (chat.type instanceof TdApi.ChatTypePrivate) {
                    long userId = ((TdApi.ChatTypePrivate) chat.type).userId;
                    session.setActiveUser(userId);
                    listener.onPrivateChatUser(userId, chatId);
                }
            }

            @Override public void onUser(TdApi.User user) {
                if (!session.isCurrent(generation) || !session.isActiveUser(user.id)) return;
                listener.setProfile(TdUserMapper.updateProfile(user, listener.profile()));
                listener.onProfileUser(user, chatId);
            }

            @Override public void onFullInfo(long userId, TdApi.UserFullInfo info) {
                if (!session.isCurrent(generation) || !session.isActiveUser(userId)) return;
                TgProfile current = listener.profile();
                if (current == null) return;
                String birthday = "";
                if (info.birthdate != null && info.birthdate.month > 0 && info.birthdate.day > 0) {
                    birthday = info.birthdate.year > 0
                            ? String.format(Locale.ROOT,"%02d.%02d.%04d",info.birthdate.day,info.birthdate.month,info.birthdate.year)
                            : String.format(Locale.ROOT,"%02d.%02d",info.birthdate.day,info.birthdate.month);
                }
                listener.setProfile(new TgProfile(current.getChatId(), current.getName(), current.getKind(),
                        current.getPhone(), info.bio == null ? "" : info.bio.text,
                        current.getAvatarPath(), current.getUsername(), current.getStatus(),
                        current.isOnline(), current.isPremium(), current.isVerified(),
                        info.groupInCommonCount,birthday,info.giftCount,info.canBeCalled,
                        info.supportsVideoCalls,current.getGifts()));
                userActions.loadProfileGifts(userId,new TdUserActionsController.GiftsListener(){
                    @Override public void onGifts(TdApi.ReceivedGifts received){
                        if(!session.isCurrent(generation)||!session.isActiveUser(userId))return;
                        List<String> items=new ArrayList<>();
                        if(received.gifts!=null)for(TdApi.ReceivedGift gift:received.gifts){
                            if(gift==null)continue;
                            String date=new SimpleDateFormat("dd.MM.yyyy",Locale.getDefault())
                                    .format(new Date(gift.date*1000L));
                            String line=gift.isPrivate?"Личный подарок · "+date:"Подарок · "+date;
                            if(!gift.isPrivate&&gift.text!=null&&gift.text.text!=null&&!gift.text.text.trim().isEmpty())
                                line+="\n"+gift.text.text.trim();
                            items.add(line);
                        }
                        TgProfile latest=listener.profile();
                        if(latest!=null&&latest.getChatId()==chatId)listener.setProfile(latest.withGifts(items));
                    }
                    @Override public void onError(TdApi.Error error){ }
                });
            }
        });
    }

    public void updateUser(TdApi.User user) {
        if (session.isActiveUser(user.id))
            listener.setProfile(TdUserMapper.updateProfile(user, listener.profile()));
    }

    public void closeProfile() {
        session.close();
        listener.setProfile(null);
    }
}
