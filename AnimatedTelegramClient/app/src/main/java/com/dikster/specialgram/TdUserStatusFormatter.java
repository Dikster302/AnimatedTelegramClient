package com.dikster.specialgram;

import org.drinkless.tdlib.TdApi;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Formats TDLib presence states for chat and profile presentation. */
public final class TdUserStatusFormatter {
    private TdUserStatusFormatter() { }

    public static String format(TdApi.UserStatus status) {
        if (status instanceof TdApi.UserStatusOnline) return "в сети";
        if (status instanceof TdApi.UserStatusOffline) {
            int timestamp = ((TdApi.UserStatusOffline) status).wasOnline;
            return "был(а) в сети " + new SimpleDateFormat(
                    "dd.MM.yyyy в HH:mm", Locale.getDefault())
                    .format(new Date(timestamp * 1000L));
        }
        if (status instanceof TdApi.UserStatusRecently) return "был(а) недавно";
        if (status instanceof TdApi.UserStatusLastWeek) return "был(а) на этой неделе";
        if (status instanceof TdApi.UserStatusLastMonth) return "был(а) в этом месяце";
        return "давно не был(а) в сети";
    }
}
