package com.dikster.specialgram;

import org.drinkless.tdlib.TdApi;

/** Coordinates generation-safe profile loading and profile model updates. */
public final class TdProfileController {
    public interface Listener {
        TgChat findChat(long chatId);
        TgProfile profile();
        void setProfile(TgProfile profile);
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
                listener.setProfile(new TgProfile(current.getChatId(), current.getName(), current.getKind(),
                        current.getPhone(), info.bio == null ? "" : info.bio.text,
                        current.getAvatarPath(), current.getUsername(), current.getStatus(),
                        current.isOnline(), current.isPremium(), current.isVerified(),
                        info.groupInCommonCount));
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
