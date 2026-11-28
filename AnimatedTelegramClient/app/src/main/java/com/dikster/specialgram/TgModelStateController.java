package com.dikster.specialgram;

/** Small immutable-model transformations shared by Java state controllers. */
public final class TgModelStateController {
    private TgModelStateController() { }

    public static TgAccount withBio(TgAccount account, String bio) {
        if (account == null) return null;
        return new TgAccount(account.getName(), account.getUsername(), account.getPhone(),
                account.isPremium(), account.isVerified(), bio, account.getAvatarPath(),
                account.getFirstName(), account.getLastName(),account.getPhotoPaths());
    }

    public static TgChat withStoryFlags(TgChat chat, boolean hasStory, boolean hasUnviewedStory) {
        return new TgChat(chat.getId(), chat.getTitle(), chat.getPreview(), chat.getLastDate(),
                chat.getUnreadCount(), chat.getAvatarPath(), chat.getKind(), chat.getArchived(),
                chat.getStatus(), chat.isOnline(), chat.isPremium(), chat.getLastReadOutboxMessageId(),
                hasStory, hasUnviewedStory, chat.isPinned(), chat.isMarkedAsUnread(), chat.isMuted(), chat.getPinnedOrder());
    }

    public static TgChat clearHistoryPreview(TgChat chat) {
        return new TgChat(chat.getId(), chat.getTitle(), "", 0, 0, chat.getAvatarPath(),
                chat.getKind(), chat.getArchived(), chat.getStatus(), chat.isOnline(),
                chat.isPremium(), chat.getLastReadOutboxMessageId(), chat.getHasStory(),
                chat.getHasUnviewedStory(), chat.isPinned(), chat.isMarkedAsUnread(), chat.isMuted(), chat.getPinnedOrder());
    }
}
