package com.dikster.specialgram;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Search, folder filtering and ordering for the Telegram-style chat list. */
public final class TdChatListFilter {
    private TdChatListFilter() { }

    public static List<TgChat> filter(List<TgChat> chats, String query,
                                     boolean newestFirst, boolean unreadOnly, String folder) {
        String needle = query == null ? "" : query.toLowerCase(Locale.ROOT);
        String selected = folder == null ? "Все" : folder;
        List<TgChat> result = new ArrayList<>();
        if (chats == null) return result;
        for (TgChat chat : chats) {
            if (chat == null) continue;
            String title = chat.getTitle() == null ? "" : chat.getTitle();
            String preview = chat.getPreview() == null ? "" : chat.getPreview();
            if (!title.toLowerCase(Locale.ROOT).contains(needle)
                    && !preview.toLowerCase(Locale.ROOT).contains(needle)) continue;
            if (unreadOnly && chat.getUnreadCount() <= 0) continue;
            boolean archived = chat.getArchived();
            String kind = chat.getKind() == null ? "" : chat.getKind();
            boolean matches;
            switch (selected) {
                case "Архив": matches = archived; break;
                case "Личные": matches = !archived &&
                        ("Личный чат".equals(kind) || "Секретный чат".equals(kind)); break;
                case "Группы": matches = !archived && "Группа".equals(kind); break;
                case "Каналы": matches = !archived && "Канал".equals(kind); break;
                default: matches = !archived; break;
            }
            if (matches) result.add(chat);
        }
        Comparator<TgChat> comparator = newestFirst
                ? Comparator.comparing(TgChat::isPinned).reversed()
                    .thenComparing(Comparator.comparingInt(TgChat::getLastDate).reversed())
                : Comparator.comparing(TgChat::isPinned).reversed()
                    .thenComparing(chat -> chat.getTitle().toLowerCase(Locale.getDefault()));
        result.sort(comparator);
        return result;
    }
}
