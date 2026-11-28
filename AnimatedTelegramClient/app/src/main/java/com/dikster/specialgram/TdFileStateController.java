package com.dikster.specialgram;

import kotlin.Pair;
import org.drinkless.tdlib.TdApi;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Applies completed TDLib downloads to chat, message, story and account state. */
public final class TdFileStateController {
    public interface Listener {
        Long activeChatId();
        List<TgChat> chats();
        void setChats(List<TgChat> chats);
        TgProfile profile();
        void setProfile(TgProfile profile);
        List<TgMessage> messages();
        void setMessages(List<TgMessage> messages);
        List<TgStory> stories();
        void setStories(List<TgStory> stories);
        TgAccount account();
        void setAccount(TgAccount account);
        void markDownloadCompleted(int fileId);
        boolean isMyPhotoFile(int fileId);
    }

    private final Listener listener;
    private final Map<Integer, Long> fileToChat;
    private final Map<Integer, Pair<Long, Long>> videoFileToMessage;
    private final Map<Integer, Pair<Long, Long>> mediaFileToMessage;
    private final Map<Integer, Pair<Long, Integer>> storyFileToKey;

    public TdFileStateController(Listener listener,
                                 Map<Integer, Long> fileToChat,
                                 Map<Integer, Pair<Long, Long>> videoFileToMessage,
                                 Map<Integer, Pair<Long, Long>> mediaFileToMessage,
                                 Map<Integer, Pair<Long, Integer>> storyFileToKey) {
        this.listener = listener;
        this.fileToChat = fileToChat;
        this.videoFileToMessage = videoFileToMessage;
        this.mediaFileToMessage = mediaFileToMessage;
        this.storyFileToKey = storyFileToKey;
    }

    public boolean handle(TdApi.Object update) {
        if (!(update instanceof TdApi.UpdateFile)) return false;
        TdApi.File file = ((TdApi.UpdateFile) update).file;
        if (!file.local.isDownloadingCompleted) return true;

        listener.markDownloadCompleted(file.id);
        Long chatId = fileToChat.remove(file.id);
        if (chatId != null) {
            List<TgChat> chats = new ArrayList<>(listener.chats().size());
            for (TgChat chat : listener.chats()) chats.add(chat.getId() == chatId
                    ? copyAvatar(chat, file.local.path) : chat);
            listener.setChats(chats);
            TgProfile profile = listener.profile();
            if (profile != null && profile.getChatId() == chatId)
                listener.setProfile(copyAvatar(profile, file.local.path));
        }

        Pair<Long, Long> videoMessage = videoFileToMessage.remove(file.id);
        if (videoMessage != null && isActive(videoMessage.getFirst())) {
            updateMessage(videoMessage.getSecond(), file, false);
        }

        Pair<Long, Long> mediaMessage = mediaFileToMessage.remove(file.id);
        if (mediaMessage != null && isActive(mediaMessage.getFirst())) {
            updateMessage(mediaMessage.getSecond(), file, true);
        }

        Pair<Long, Integer> storyKey = storyFileToKey.remove(file.id);
        if (storyKey != null) {
            List<TgStory> stories = new ArrayList<>(listener.stories().size());
            for (TgStory story : listener.stories()) stories.add(
                    story.getChatId() == storyKey.getFirst() && story.getStoryId() == storyKey.getSecond()
                            ? copyPath(story, file.local.path) : story);
            listener.setStories(stories);
        }

        if (listener.isMyPhotoFile(file.id)) {
            TgAccount account = listener.account();
            if (account != null) listener.setAccount(copyAvatar(account, file.local.path));
        }
        return true;
    }

    private boolean isActive(long chatId) {
        Long active = listener.activeChatId();
        return active != null && active == chatId;
    }

    private void updateMessage(long messageId, TdApi.File file, boolean media) {
        List<TgMessage> messages = new ArrayList<>(listener.messages().size());
        for (TgMessage message : listener.messages()) {
            if (message.getId() != messageId) {
                messages.add(message);
                continue;
            }
            String mediaPath = media ? file.local.path : message.getMediaPath();
            String photoPath = media && "photo".equals(message.getMediaKind())
                    ? file.local.path : message.getPhotoPath();
            long mediaSize = media ? file.size : message.getMediaSize();
            String videoPath = media ? message.getVideoPath() : file.local.path;
            messages.add(new TgMessage(message.getId(), message.getText(), message.getOutgoing(),
                    message.getDate(), message.isRead(), message.getVideoShape(), videoPath,
                    message.getReplyToMessageId(), message.getReplyToText(), message.getReactions(),
                    photoPath, message.getMediaKind(), mediaPath, message.getMediaName(),
                    message.getMediaMimeType(), message.getMediaDuration(), mediaSize, message.isPinned()));
        }
        listener.setMessages(messages);
    }

    private static TgChat copyAvatar(TgChat chat, String path) {
        return new TgChat(chat.getId(), chat.getTitle(), chat.getPreview(), chat.getLastDate(),
                chat.getUnreadCount(), path, chat.getKind(), chat.getArchived(), chat.getStatus(),
                chat.isOnline(), chat.isPremium(), chat.getLastReadOutboxMessageId(),
                chat.getHasStory(), chat.getHasUnviewedStory(), chat.isPinned(),
                chat.isMarkedAsUnread(), chat.isMuted());
    }

    private static TgProfile copyAvatar(TgProfile profile, String path) {
        return new TgProfile(profile.getChatId(), profile.getName(), profile.getKind(), profile.getPhone(),
                profile.getBio(), path, profile.getUsername(), profile.getStatus(), profile.isOnline(),
                profile.isPremium(), profile.isVerified(), profile.getCommonGroups());
    }

    private static TgAccount copyAvatar(TgAccount account, String path) {
        return new TgAccount(account.getName(), account.getUsername(), account.getPhone(),
                account.isPremium(), account.isVerified(), account.getBio(), path,
                account.getFirstName(), account.getLastName());
    }

    private static TgStory copyPath(TgStory story, String path) {
        return new TgStory(story.getChatId(), story.getStoryId(), story.getOwnerName(), story.getDate(),
                story.getCaption(), story.isVideo(), path, story.isViewed());
    }
}
