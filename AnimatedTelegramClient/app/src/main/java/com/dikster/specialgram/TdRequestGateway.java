package com.dikster.specialgram;

import org.drinkless.tdlib.TdApi;

/** Adapts Java TDLib dispatch callbacks to optional result/error consumers and UI reporting. */
public final class TdRequestGateway {
    public interface ResultHandler { void onResult(TdApi.Object result); }
    public interface ErrorHandler { void onError(TdApi.Error error); }
    public interface Listener { void onOperationError(String message); }

    private final TdRequestDispatcher dispatcher;
    private final Listener listener;

    public TdRequestGateway(TdRequestDispatcher dispatcher, Listener listener) {
        this.dispatcher = dispatcher;
        this.listener = listener;
    }

    public void send(TdApi.Function<?> function, ResultHandler resultHandler,
                     ErrorHandler errorHandler, boolean reportErrorToUi) {
        dispatcher.send(function, new TdRequestDispatcher.Listener() {
            @Override public void onResult(TdApi.Object result) {
                if (resultHandler != null) resultHandler.onResult(result);
            }
            @Override public void onError(TdApi.Error error) {
                if (errorHandler != null) errorHandler.onError(error);
                if (reportErrorToUi && listener != null)
                    listener.onOperationError(error == null ? "TDLib error" : error.message);
            }
        });
    }
}
