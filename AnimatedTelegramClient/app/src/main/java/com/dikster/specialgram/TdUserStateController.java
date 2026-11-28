package com.dikster.specialgram;

import org.drinkless.tdlib.TdApi;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Applies TDLib user, contact and presence updates to immutable UI state. */
public final class TdUserStateController {
    public static final class State {
        public final List<TgChat> chats;
        public final List<TgContact> contacts;
        public final TgProfile profile;
        State(List<TgChat> chats, List<TgContact> contacts, TgProfile profile) {
            this.chats = chats;
            this.contacts = contacts;
            this.profile = profile;
        }
    }

    private TdUserStateController() { }

    public static State refreshStatus(List<TgChat> chats, List<TgContact> contacts,
                                      TgProfile profile, Map<Long, Long> userToChat,
                                      long userId, TdApi.UserStatus tdStatus) {
        String status = TdUserStatusFormatter.format(tdStatus);
        boolean online = tdStatus instanceof TdApi.UserStatusOnline;
        List<TgContact> updatedContacts = new ArrayList<>(contacts.size());
        for (TgContact contact : contacts) updatedContacts.add(contact.getId() == userId
                ? new TgContact(contact.getId(), contact.getName(), contact.getUsername(),
                    contact.getPhone(), status, online, contact.isPremium())
                : contact);

        Long chatId = userToChat.get(userId);
        List<TgChat> updatedChats = chats;
        TgProfile updatedProfile = profile;
        if (chatId != null) {
            updatedChats = new ArrayList<>(chats.size());
            for (TgChat chat : chats) updatedChats.add(chat.getId() == chatId
                    ? copyChatPresence(chat, status, online, chat.isPremium()) : chat);
            if (profile != null && profile.getChatId() == chatId) {
                updatedProfile = new TgProfile(profile.getChatId(), profile.getName(), profile.getKind(),
                        profile.getPhone(), profile.getBio(), profile.getAvatarPath(), profile.getUsername(),
                        status, online, profile.isPremium(), profile.isVerified(), profile.getCommonGroups(),
                        profile.getBirthdate(), profile.getGiftCount(), profile.canBeCalled(),
                        profile.supportsVideoCalls(), profile.getGifts(), profile.getUserId());
            }
        }
        return new State(updatedChats, updatedContacts, updatedProfile);
    }

    public static List<TgContact> updateContact(List<TgContact> source, TdApi.User user) {
        TgContact item = TdUserMapper.toContact(user);
        List<TgContact> result = new ArrayList<>(source.size() + 1);
        for (TgContact contact : source) if (contact.getId() != user.id) result.add(contact);
        result.add(item);
        result.sort(Comparator.comparing(contact -> contact.getName().toLowerCase(Locale.getDefault())));
        return result;
    }

    public static List<TgChat> updateChatUser(List<TgChat> source, Map<Long, Long> userToChat,
                                              TdApi.User user) {
        Long chatId = userToChat.get(user.id);
        if (chatId == null) return source;
        String status = TdUserStatusFormatter.format(user.status);
        boolean online = user.status instanceof TdApi.UserStatusOnline;
        List<TgChat> result = new ArrayList<>(source.size());
        for (TgChat chat : source) result.add(chat.getId() == chatId
                ? copyChatPresence(chat, status, online, user.isPremium) : chat);
        return result;
    }

    private static TgChat copyChatPresence(TgChat chat, String status, boolean online, boolean premium) {
        return new TgChat(chat.getId(), chat.getTitle(), chat.getPreview(), chat.getLastDate(),
                chat.getUnreadCount(), chat.getAvatarPath(), chat.getKind(), chat.getArchived(),
                status, online, premium, chat.getLastReadOutboxMessageId(), chat.getHasStory(),
                chat.getHasUnviewedStory(), chat.isPinned(), chat.isMarkedAsUnread(), chat.isMuted(), chat.getPinnedOrder());
    }
}
