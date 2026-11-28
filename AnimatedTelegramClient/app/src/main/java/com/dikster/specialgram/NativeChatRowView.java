package com.dikster.specialgram;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import com.dikster.specialgram.anim.SpecialgramAnimations;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Native Java chat list row with story avatar, unread counter and chat actions. */
public final class NativeChatRowView extends LinearLayout {
    private static final int ACTION_PIN = 1;
    private static final int ACTION_UNREAD = 2;
    private static final int ACTION_MUTE = 3;
    private static final int ACTION_ARCHIVE = 4;

    private final NativeAvatarView avatar;
    private final TextView title;
    private final TextView preview;
    private final TextView time;
    private final TextView premium;
    private final TextView pin;
    private final TextView mute;
    private final TextView unread;
    private TgChat chat;
    private Runnable openAction;
    private Runnable archiveAction;
    private Runnable pinAction;
    private Runnable unreadAction;
    private Runnable muteAction;
    private int textColor;
    private int mutedColor;
    private int accentColor;
    private int previousUnreadCount = -1;

    public NativeChatRowView(Context context, TgChat chat, boolean compact,
                             int textColor, int mutedColor, int accentColor, int panelColor,
                             Runnable onOpen, Runnable onArchive, Runnable onPin,
                             Runnable onUnread, Runnable onMute) {
        super(context);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setPadding(dp(16), dp(compact ? 5 : 8), dp(16), dp(compact ? 5 : 8));
        setMinimumHeight(dp(76));
        setBackgroundResource(android.R.drawable.list_selector_background);
        avatar = new NativeAvatarView(context, "", "", 0L, 52, false, false, false, panelColor);
        LayoutParams avatarParams = new LayoutParams(dp(52), dp(52));
        addView(avatar, avatarParams);

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(VERTICAL);
        LayoutParams contentParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        contentParams.leftMargin = dp(12);
        addView(content, contentParams);

        LinearLayout top = new LinearLayout(context);
        top.setGravity(Gravity.CENTER_VERTICAL);
        content.addView(top, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        title = new TextView(context);
        title.setTextSize(16);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        top.addView(title, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

        premium = smallLabel(context, 13);
        top.addView(premium);
        pin = smallLabel(context, 11);
        LayoutParams pinParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        pinParams.leftMargin = dp(3);
        top.addView(pin, pinParams);
        mute = smallLabel(context, 11);
        LayoutParams muteParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        muteParams.leftMargin = dp(2);
        top.addView(mute, muteParams);

        time = smallLabel(context, 12);
        LayoutParams timeParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        timeParams.leftMargin = dp(5);
        top.addView(time, timeParams);

        LinearLayout bottom = new LinearLayout(context);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        LayoutParams bottomParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        bottomParams.topMargin = dp(4);
        content.addView(bottom, bottomParams);

        preview = new TextView(context);
        preview.setTextSize(14);
        preview.setSingleLine(true);
        preview.setEllipsize(TextUtils.TruncateAt.END);
        bottom.addView(preview, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

        unread = new TextView(context);
        unread.setTextColor(Color.WHITE);
        unread.setTextSize(11);
        unread.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        unread.setGravity(Gravity.CENTER);
        unread.setPadding(dp(6), 0, dp(6), 0);
        GradientDrawable badge = new GradientDrawable();
        badge.setColor(Color.rgb(42, 171, 238));
        badge.setCornerRadius(dp(20));
        unread.setBackground(badge);
        LayoutParams unreadParams = new LayoutParams(LayoutParams.WRAP_CONTENT, dp(21));
        unreadParams.leftMargin = dp(8);
        bottom.addView(unread, unreadParams);

        setOnClickListener(view -> { if (openAction != null) openAction.run(); });
        setOnLongClickListener(view -> { showMenu(); return true; });
        update(chat, compact, textColor, mutedColor, accentColor, panelColor,
                onOpen, onArchive, onPin, onUnread, onMute);
    }

    public void update(TgChat value, boolean compact, int textColor, int mutedColor,
                       int accentColor, int panelColor, Runnable onOpen, Runnable onArchive,
                       Runnable onPin, Runnable onUnread, Runnable onMute) {
        this.chat = value;
        this.textColor = textColor;
        this.mutedColor = mutedColor;
        this.accentColor = accentColor;
        this.openAction = onOpen;
        this.archiveAction = onArchive;
        this.pinAction = onPin;
        this.unreadAction = onUnread;
        this.muteAction = onMute;
        setPadding(dp(16), dp(compact ? 5 : 8), dp(16), dp(compact ? 5 : 8));
        avatar.update(value.getAvatarPath(), initials(value.getTitle()), value.getId(), 52,
                value.isOnline(), value.getHasStory(), !value.getHasUnviewedStory(), panelColor);
        title.setText(value.getTitle());
        title.setTextColor(textColor);
        preview.setText(value.getPreview().isEmpty()
                ? (value.getStatus().isEmpty() ? "Нет сообщений" : value.getStatus())
                : value.getPreview());
        preview.setTextColor(mutedColor);
        premium.setText(value.isPremium() ? "✦" : "");
        premium.setTextColor(Color.rgb(181, 140, 255));
        pin.setText(value.isPinned() ? "📌" : "");
        mute.setText(value.isMuted() ? "🔕" : "");
        String timestamp = formatTime(value.getLastDate());
        time.setText(timestamp);
        time.setTextColor(value.getUnreadCount() > 0 || value.isMarkedAsUnread()
                ? accentColor : mutedColor);
        if (value.getUnreadCount() > 0) {
            boolean appeared = previousUnreadCount <= 0;
            unread.setVisibility(VISIBLE);
            unread.setText(value.getUnreadCount() > 99 ? "99+" : String.valueOf(value.getUnreadCount()));
            GradientDrawable badge = new GradientDrawable();
            badge.setColor(value.isMuted()?mutedColor:accentColor);
            badge.setCornerRadius(dp(20));
            unread.setBackground(badge);
            if (appeared) SpecialgramAnimations.animateCounterIn(unread);
        } else {
            unread.setVisibility(GONE);
        }
        previousUnreadCount = value.getUnreadCount();
    }

    private void showMenu() {
        if (chat == null) return;
        PopupMenu menu = new PopupMenu(getContext(), this);
        menu.getMenu().add(0, ACTION_PIN, 0, chat.isPinned() ? "Открепить чат" : "Закрепить чат");
        menu.getMenu().add(0, ACTION_UNREAD, 1,
                chat.isMarkedAsUnread() ? "Отметить прочитанным" : "Отметить непрочитанным");
        menu.getMenu().add(0, ACTION_MUTE, 2,
                chat.isMuted() ? "Включить уведомления" : "Отключить уведомления");
        menu.getMenu().add(0, ACTION_ARCHIVE, 3,
                chat.getArchived() ? "Вернуть из архива" : "В архив");
        menu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case ACTION_PIN: if (pinAction != null) pinAction.run(); return true;
                case ACTION_UNREAD: if (unreadAction != null) unreadAction.run(); return true;
                case ACTION_MUTE: if (muteAction != null) muteAction.run(); return true;
                case ACTION_ARCHIVE: if (archiveAction != null) archiveAction.run(); return true;
                default: return false;
            }
        });
        menu.show();
    }

    private TextView smallLabel(Context context, int size) {
        TextView view = new TextView(context);
        view.setTextSize(size);
        view.setGravity(Gravity.CENTER);
        return view;
    }

    private String initials(String value) {
        if (value == null || value.trim().isEmpty()) return "T";
        String[] words = value.trim().split("\\s+");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.length && i < 2; i++)
            if (!words[i].isEmpty()) result.append(words[i].substring(0, 1).toUpperCase(Locale.getDefault()));
        return result.length() == 0 ? "T" : result.toString();
    }

    private String formatTime(int timestamp) {
        if (timestamp <= 0) return "";
        try {
            Date date = new Date(timestamp * 1000L);
            Calendar today = Calendar.getInstance();
            Calendar messageDay = Calendar.getInstance();
            messageDay.setTime(date);
            boolean sameDay = today.get(Calendar.YEAR) == messageDay.get(Calendar.YEAR)
                    && today.get(Calendar.DAY_OF_YEAR) == messageDay.get(Calendar.DAY_OF_YEAR);
            int daysAgo = (int) ((today.getTimeInMillis() - messageDay.getTimeInMillis()) / 86_400_000L);
            if (sameDay) return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date);
            if (daysAgo == 1) return "вчера";
            if (daysAgo >= 2 && daysAgo <= 6)
                return new SimpleDateFormat("EEE", Locale.getDefault()).format(date);
            return new SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(date);
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
