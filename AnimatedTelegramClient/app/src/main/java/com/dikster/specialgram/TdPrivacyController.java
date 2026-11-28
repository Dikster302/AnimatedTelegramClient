package com.dikster.specialgram;

import org.drinkless.tdlib.TdApi;

/** Java implementation of TDLib privacy settings loading, formatting and updates. */
public final class TdPrivacyController {
    public static final int STATUS = 0;
    public static final int PHONE = 1;
    public static final int PHOTO = 2;

    public interface Callback {
        void onResult(TdApi.Object result);
        void onError(TdApi.Error error);
    }
    public interface Requester {
        void request(TdApi.Function<?> function, Callback callback);
    }
    public interface Listener {
        void onValue(int type, String value);
    }

    private final Requester requester;
    private final Listener listener;

    public TdPrivacyController(Requester requester, Listener listener) {
        this.requester = requester;
        this.listener = listener;
    }

    public void loadAll() {
        load(STATUS);
        load(PHONE);
        load(PHOTO);
    }

    private void load(int type) {
        requester.request(new TdApi.GetUserPrivacySettingRules(setting(type)), new Callback() {
            @Override public void onResult(TdApi.Object result) {
                if (result instanceof TdApi.UserPrivacySettingRules) {
                    listener.onValue(type, label((TdApi.UserPrivacySettingRules) result));
                }
            }
            @Override public void onError(TdApi.Error error) { }
        });
    }

    public void set(int type, String value) {
        TdApi.UserPrivacySettingRule rule = rule(value);
        if (rule == null) return;
        requester.request(new TdApi.SetUserPrivacySettingRules(
                setting(type), new TdApi.UserPrivacySettingRules(new TdApi.UserPrivacySettingRule[]{rule})),
                new Callback() {
                    @Override public void onResult(TdApi.Object result) {
                        if (result instanceof TdApi.Ok) listener.onValue(type, value);
                    }
                    @Override public void onError(TdApi.Error error) { }
                });
    }

    private static TdApi.UserPrivacySetting setting(int type) {
        switch (type) {
            case STATUS: return new TdApi.UserPrivacySettingShowStatus();
            case PHONE: return new TdApi.UserPrivacySettingShowPhoneNumber();
            case PHOTO: return new TdApi.UserPrivacySettingShowProfilePhoto();
            default: throw new IllegalArgumentException("Unknown privacy setting");
        }
    }

    private static TdApi.UserPrivacySettingRule rule(String value) {
        if ("Все".equals(value)) return new TdApi.UserPrivacySettingRuleAllowAll();
        if ("Мои контакты".equals(value)) return new TdApi.UserPrivacySettingRuleAllowContacts();
        if ("Никто".equals(value)) return new TdApi.UserPrivacySettingRuleRestrictAll();
        return null;
    }

    public static String label(TdApi.UserPrivacySettingRules rules) {
        if (rules == null || rules.rules == null || rules.rules.length != 1) return "Особые настройки";
        TdApi.UserPrivacySettingRule rule = rules.rules[0];
        if (rule instanceof TdApi.UserPrivacySettingRuleAllowAll) return "Все";
        if (rule instanceof TdApi.UserPrivacySettingRuleAllowContacts) return "Мои контакты";
        if (rule instanceof TdApi.UserPrivacySettingRuleRestrictAll) return "Никто";
        return "Особые настройки";
    }
}
