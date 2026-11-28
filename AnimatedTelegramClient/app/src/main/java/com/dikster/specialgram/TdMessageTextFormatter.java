package com.dikster.specialgram;

import org.drinkless.tdlib.TdApi;

/** Consistent Russian labels and previews for TDLib message content. */
public final class TdMessageTextFormatter {
    private TdMessageTextFormatter() { }

    public static String display(TdApi.MessageContent content) {
        if (content instanceof TdApi.MessageText) return ((TdApi.MessageText) content).text.text;
        if (content instanceof TdApi.MessagePhoto) return "📷 Фото";
        if (content instanceof TdApi.MessageVideo) return "🎥 Видео";
        if (content instanceof TdApi.MessageVideoNote) return "🎥 Видеокружок";
        if (content instanceof TdApi.MessageDocument) {
            String name = ((TdApi.MessageDocument) content).document.fileName;
            return "📎 " + (name == null || name.trim().isEmpty() ? "Файл" : name);
        }
        if (content instanceof TdApi.MessageSticker) {
            return "Стикер " + ((TdApi.MessageSticker) content).sticker.emoji;
        }
        if (content instanceof TdApi.MessageAnimation) {
            String caption = ((TdApi.MessageAnimation) content).caption == null ? null
                    : ((TdApi.MessageAnimation) content).caption.text;
            return caption == null || caption.trim().isEmpty() ? "GIF" : caption;
        }
        if (content instanceof TdApi.MessageAudio) {
            String title = ((TdApi.MessageAudio) content).audio.title;
            return "🎵 " + (title == null || title.trim().isEmpty() ? "Аудио" : title);
        }
        if (content instanceof TdApi.MessageVoiceNote) return "🎤 Голосовое сообщение";
        return "Сообщение";
    }

    public static String preview(TdApi.MessageContent content) {
        if (content instanceof TdApi.MessageText) return ((TdApi.MessageText) content).text.text;
        if (content instanceof TdApi.MessagePhoto) return "📷 Фото";
        if (content instanceof TdApi.MessageVideo) return "🎥 Видео";
        if (content instanceof TdApi.MessageVideoNote) return "🎥 Видеокружок";
        if (content instanceof TdApi.MessageDocument) return "📎 Файл";
        if (content instanceof TdApi.MessageSticker) return "Стикер";
        if (content instanceof TdApi.MessageVoiceNote) return "🎤 Голосовое сообщение";
        if (content instanceof TdApi.MessageAnimation) return "GIF";
        if (content instanceof TdApi.MessageAudio) return "🎵 Аудио";
        return "Сообщение";
    }

    public static String videoShapeTitle(String shape) {
        if ("square".equals(shape)) return "Квадрат";
        if ("rounded".equals(shape)) return "Скруглённый квадрат";
        if ("heart".equals(shape)) return "Сердце";
        if ("star".equals(shape)) return "Звезда";
        if ("hexagon".equals(shape)) return "Шестиугольник";
        return "Круг";
    }
}
