package com.dikster.specialgram;

import org.drinkless.tdlib.TdApi;

/** Converts TDLib stories and selects their highest-resolution media file. */
public final class TdStoryMapper {
    public static final class Result {
        public final TgStory story;
        public final TdApi.File mediaFile;
        Result(TgStory story, TdApi.File mediaFile) {
            this.story = story;
            this.mediaFile = mediaFile;
        }
    }
    private TdStoryMapper() { }

    public static Result map(TdApi.Story source, String ownerName, boolean viewed) {
        TdApi.File file = null;
        long bestArea = -1L;
        boolean video = false;
        if (source.content instanceof TdApi.StoryContentPhoto) {
            TdApi.Photo photo = ((TdApi.StoryContentPhoto) source.content).photo;
            if (photo.sizes != null) for (TdApi.PhotoSize size : photo.sizes) {
                if (size == null || size.photo == null) continue;
                long area = size.width * (long) size.height;
                if (area > bestArea) { bestArea = area; file = size.photo; }
            }
        } else if (source.content instanceof TdApi.StoryContentVideo) {
            file = ((TdApi.StoryContentVideo) source.content).video.video;
            video = true;
        }
        String path = file == null ? "unsupported" :
                file.local.isDownloadingCompleted ? safe(file.local.path) : "";
        String caption = source.caption == null ? "" : safe(source.caption.text);
        TgStory story = new TgStory(source.posterChatId, source.id,
                ownerName == null || ownerName.isEmpty() ? "История" : ownerName,
                source.date, caption, video, path, viewed);
        return new Result(story, file);
    }
    private static String safe(String value) { return value == null ? "" : value; }
}
