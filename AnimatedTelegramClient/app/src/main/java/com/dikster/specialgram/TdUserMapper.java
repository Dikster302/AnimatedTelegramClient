package com.dikster.specialgram;

import org.drinkless.tdlib.TdApi;

/** Maps TDLib user snapshots to account, contact and profile presentation models. */
public final class TdUserMapper {
    private TdUserMapper() { }

    public static String statusText(TdApi.UserStatus status) {
        return TdUserStatusFormatter.format(status);
    }

    public static TgContact toContact(TdApi.User user) {
        String name = fullName(user);
        String username = username(user);
        return new TgContact(user.id, name, username, safe(user.phoneNumber),
                statusText(user.status), user.status instanceof TdApi.UserStatusOnline,
                user.isPremium);
    }

    public static TgAccount toAccount(TdApi.User user, TgAccount previous) {
        String name = fullName(user);
        String username = username(user);
        return new TgAccount(name.isEmpty() ? "Мой аккаунт" : name, username,
                safe(user.phoneNumber), user.isPremium,
                user.verificationStatus != null && user.verificationStatus.isVerified,
                previous == null ? "" : safe(previous.getBio()),
                previous == null ? "" : safe(previous.getAvatarPath()),
                safe(user.firstName), safe(user.lastName));
    }

    public static TgProfile updateProfile(TdApi.User user, TgProfile previous) {
        if (previous == null) return null;
        String name = fullName(user);
        String username = username(user);
        return new TgProfile(previous.getChatId(),
                name.isEmpty() ? previous.getName() : name,
                previous.getKind(), safe(user.phoneNumber), previous.getBio(),
                previous.getAvatarPath(), username, statusText(user.status),
                user.status instanceof TdApi.UserStatusOnline, user.isPremium,
                user.verificationStatus != null && user.verificationStatus.isVerified,
                previous.getCommonGroups(),previous.getBirthdate(),previous.getGiftCount(),
                previous.canBeCalled(),previous.supportsVideoCalls(),previous.getGifts(),previous.getUserId(),previous.getPhotoPaths());
    }

    private static String username(TdApi.User user) {
        if (user.usernames == null || user.usernames.activeUsernames == null
                || user.usernames.activeUsernames.length == 0) return "";
        return safe(user.usernames.activeUsernames[0]);
    }

    private static String fullName(TdApi.User user) {
        String first = safe(user.firstName).trim();
        String last = safe(user.lastName).trim();
        if (first.isEmpty()) return last;
        if (last.isEmpty()) return first;
        return first + " " + last;
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
