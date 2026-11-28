package com.dikster.specialgram;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Stable chronological ordering for story navigation. */
public final class TdStoryListFilter {
    private TdStoryListFilter() { }
    public static List<TgStory> ordered(List<TgStory> stories) {
        List<TgStory> result = stories == null ? new ArrayList<>() : new ArrayList<>(stories);
        result.sort(Comparator.comparingInt(TgStory::getDate)
                .thenComparingLong(TgStory::getChatId)
                .thenComparingInt(TgStory::getStoryId));
        return result;
    }

    public static List<TgStory> forChat(List<TgStory> stories, long chatId) {
        List<TgStory> result = new ArrayList<>();
        if (stories != null) for (TgStory story : stories)
            if (story.getChatId() == chatId) result.add(story);
        return result;
    }

    public static TgStory firstToView(List<TgStory> stories, long chatId) {
        List<TgStory> group = forChat(stories, chatId);
        for (TgStory story : group) if (!story.isViewed()) return story;
        return group.isEmpty() ? null : group.get(0);
    }
}
