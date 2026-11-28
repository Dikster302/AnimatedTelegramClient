package com.dikster.specialgram;

import org.drinkless.tdlib.TdApi;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Owns story list state, viewed flags and story-media download handoff. */
public final class TdStoryStateController {
    public interface Listener {
        List<TgChat> chats();
        List<TgStory> stories();
        void setChats(List<TgChat> chats);
        void setStories(List<TgStory> stories);
        void enqueueStoryContent(long chatId, int storyId, boolean viewed);
        void onStoryMediaFile(TdApi.File file, TgStory story);
    }

    private final Listener listener;

    public TdStoryStateController(Listener listener) {
        this.listener = listener;
    }

    public void updateChatStories(TdApi.ChatActiveStories update) {
        boolean hasStory = !update.stories.isEmpty();
        boolean hasUnviewed = false;
        for (TdApi.StoryInfo story : update.stories) {
            if (story.storyId > update.maxReadStoryId) hasUnviewed = true;
        }
        List<TgChat> chats = new ArrayList<>();
        for (TgChat chat : listener.chats()) {
            chats.add(chat.getId() == update.chatId
                    ? withStoryFlags(chat, hasStory, hasUnviewed) : chat);
        }
        listener.setChats(chats);

        if (!hasStory) {
            List<TgStory> stories = new ArrayList<>();
            for (TgStory story : listener.stories()) {
                if (story.getChatId() != update.chatId) stories.add(story);
            }
            listener.setStories(stories);
        } else {
            for (TdApi.StoryInfo story : update.stories) {
                listener.enqueueStoryContent(update.chatId, story.storyId,
                        story.storyId <= update.maxReadStoryId);
            }
        }
    }

    public void updateStory(TdApi.Story source, Boolean viewed) {
        String owner = "История";
        for (TgChat chat : listener.chats()) {
            if (chat.getId() == source.posterChatId) {
                owner = chat.getTitle();
                break;
            }
        }
        boolean isViewed = viewed != null && viewed;
        if (viewed == null) {
            for (TgStory story : listener.stories()) {
                if (story.getChatId() == source.posterChatId && story.getStoryId() == source.id) {
                    isViewed = story.isViewed();
                    break;
                }
            }
        }

        TdStoryMapper.Result mapped = TdStoryMapper.map(source, owner, isViewed);
        TgStory item = mapped.story;
        List<TgStory> stories = new ArrayList<>();
        for (TgStory story : listener.stories()) {
            if (story.getChatId() != item.getChatId() || story.getStoryId() != item.getStoryId()) {
                stories.add(story);
            }
        }
        stories.add(item);
        stories.sort(Comparator.comparingInt(TgStory::getDate));
        listener.setStories(stories);

        TdApi.File file = mapped.mediaFile;
        if (file != null && !file.local.isDownloadingCompleted) {
            listener.onStoryMediaFile(file, item);
        }
    }

    public void deleteStory(long chatId, int storyId) {
        List<TgStory> stories = new ArrayList<>();
        for (TgStory story : listener.stories()) {
            if (story.getChatId() != chatId || story.getStoryId() != storyId) stories.add(story);
        }
        listener.setStories(stories);
        updateChatStoryFlags(chatId, stories);
    }

    public void markViewed(long chatId, int storyId) {
        List<TgStory> stories = new ArrayList<>();
        for (TgStory story : listener.stories()) {
            if (story.getChatId() == chatId && story.getStoryId() == storyId) {
                stories.add(new TgStory(story.getChatId(), story.getStoryId(), story.getOwnerName(),
                        story.getDate(), story.getCaption(), story.isVideo(), story.getPath(), true));
            } else {
                stories.add(story);
            }
        }
        listener.setStories(stories);
        updateChatUnviewedFlag(chatId, stories);
    }

    private void updateChatUnviewedFlag(long chatId, List<TgStory> stories) {
        boolean hasUnviewed = false;
        for (TgStory story : stories) {
            if (story.getChatId() == chatId && !story.isViewed()) hasUnviewed = true;
        }
        List<TgChat> chats = new ArrayList<>();
        for (TgChat chat : listener.chats()) {
            chats.add(chat.getId() == chatId
                    ? withStoryFlags(chat, chat.getHasStory(), hasUnviewed) : chat);
        }
        listener.setChats(chats);
    }

    private void updateChatStoryFlags(long chatId, List<TgStory> stories) {
        boolean hasStory = false;
        boolean hasUnviewed = false;
        for (TgStory story : stories) {
            if (story.getChatId() == chatId) {
                hasStory = true;
                if (!story.isViewed()) hasUnviewed = true;
            }
        }
        List<TgChat> chats = new ArrayList<>();
        for (TgChat chat : listener.chats()) {
            chats.add(chat.getId() == chatId
                    ? withStoryFlags(chat, hasStory, hasUnviewed) : chat);
        }
        listener.setChats(chats);
    }

    private static TgChat withStoryFlags(TgChat c, boolean hasStory, boolean hasUnviewed) {
        return new TgChat(c.getId(), c.getTitle(), c.getPreview(), c.getLastDate(),
                c.getUnreadCount(), c.getAvatarPath(), c.getKind(), c.getArchived(),
                c.getStatus(), c.isOnline(), c.isPremium(), c.getLastReadOutboxMessageId(),
                hasStory, hasUnviewed, c.isPinned(), c.isMarkedAsUnread(), c.isMuted());
    }
}
