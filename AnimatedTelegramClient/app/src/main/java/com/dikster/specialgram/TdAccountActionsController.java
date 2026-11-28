package com.dikster.specialgram;

import org.drinkless.tdlib.TdApi;

/** Account name, bio and profile-photo operations with visible TDLib error reporting. */
public final class TdAccountActionsController {
    public interface Callback {
        void onResult(TdApi.Object result);
        void onError(TdApi.Error error);
    }
    public interface Requester {
        void send(TdApi.Function<?> function, Callback callback);
    }
    public interface Listener {
        void onAccountUser(TdApi.User user);
        void onBioUpdated(String bio);
        void onValidationError(String message);
    }

    private final Requester requester;
    private final Listener listener;

    public TdAccountActionsController(Requester requester, Listener listener) {
        this.requester = requester;
        this.listener = listener;
    }

    public void updateProfile(String firstName, String lastName, String bio, long userId) {
        String first = firstName == null ? "" : firstName.trim();
        String last = lastName == null ? "" : lastName.trim();
        String about = bio == null ? "" : bio.trim().replace('\n', ' ');
        if (first.isEmpty()) {
            listener.onValidationError("Имя не может быть пустым");
            return;
        }
        if (first.length() > 64 || last.length() > 64) {
            listener.onValidationError("Имя и фамилия должны быть не длиннее 64 символов");
            return;
        }
        if (about.length() > 70) {
            listener.onValidationError("Описание профиля должно быть не длиннее 70 символов");
            return;
        }

        requester.send(new TdApi.SetName(first, last), new Callback() {
            @Override public void onResult(TdApi.Object result) {
                if (result instanceof TdApi.Ok) refreshAccount();
                else listener.onValidationError("Telegram не подтвердил изменение имени");
            }
            @Override public void onError(TdApi.Error error) {
                listener.onValidationError(errorText(error, "Не удалось изменить имя"));
            }
        });

        requester.send(new TdApi.SetBio(about), new Callback() {
            @Override public void onResult(TdApi.Object result) {
                if (!(result instanceof TdApi.Ok)) {
                    listener.onValidationError("Telegram не подтвердил изменение описания");
                    return;
                }
                if (userId <= 0) {
                    listener.onBioUpdated(about);
                    return;
                }
                requester.send(new TdApi.GetUserFullInfo(userId), new Callback() {
                    @Override public void onResult(TdApi.Object info) {
                        if (info instanceof TdApi.UserFullInfo) {
                            TdApi.FormattedText value = ((TdApi.UserFullInfo) info).bio;
                            listener.onBioUpdated(value == null ? "" : value.text);
                        } else {
                            listener.onBioUpdated(about);
                        }
                    }
                    @Override public void onError(TdApi.Error error) {
                        // The write succeeded; retain the submitted value even if refresh fails.
                        listener.onBioUpdated(about);
                    }
                });
            }
            @Override public void onError(TdApi.Error error) {
                listener.onValidationError(errorText(error, "Не удалось изменить описание профиля"));
            }
        });
    }

    public void setProfilePhoto(String path, long userId) {
        if (path == null || path.trim().isEmpty()) {
            listener.onValidationError("Не выбран файл фотографии");
            return;
        }
        requester.send(new TdApi.SetProfilePhoto(
                new TdApi.InputChatPhotoStatic(new TdApi.InputFileLocal(path)), false),
                new Callback() {
                    @Override public void onResult(TdApi.Object result) {
                        if (result instanceof TdApi.Ok) {
                            if (userId > 0) refreshAccount();
                        } else {
                            listener.onValidationError("Telegram не подтвердил изменение фотографии");
                        }
                    }
                    @Override public void onError(TdApi.Error error) {
                        listener.onValidationError(errorText(error, "Не удалось изменить фотографию профиля"));
                    }
                });
    }

    private void refreshAccount() {
        requester.send(new TdApi.GetMe(), new Callback() {
            @Override public void onResult(TdApi.Object result) {
                if (result instanceof TdApi.User) listener.onAccountUser((TdApi.User) result);
                else listener.onValidationError("Не удалось обновить данные аккаунта после сохранения");
            }
            @Override public void onError(TdApi.Error error) {
                listener.onValidationError(errorText(error, "Изменения сохранены, но профиль не удалось обновить"));
            }
        });
    }

    private static String errorText(TdApi.Error error, String fallback) {
        return error == null || error.message == null || error.message.trim().isEmpty()
                ? fallback : error.message;
    }
}
