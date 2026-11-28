package com.dikster.specialgram.td;

import android.content.Context;

import com.dikster.specialgram.TdLibClient;

/**
 * Единственная точка владения TDLib-клиентом на процесс приложения.
 *
 * UI получает фасад TdLibClient через client() и не создаёт TDLib Client
 * самостоятельно. Экземпляр создаётся в Application, а не в Activity,
 * поэтому пересоздание экрана не создаёт второе TDLib-соединение.
 */
public final class TgClient {
    private static volatile TgClient instance;

    private final TdLibClient client;

    private TgClient(Context context) {
        client = new TdLibClient(context.getApplicationContext());
    }

    public static TgClient initialize(Context context) {
        if (context == null) throw new IllegalArgumentException("Context не должен быть null");
        TgClient current = instance;
        if (current == null) {
            synchronized (TgClient.class) {
                current = instance;
                if (current == null) {
                    current = new TgClient(context);
                    instance = current;
                }
            }
        }
        return current;
    }

    public static TgClient getInstance() {
        TgClient current = instance;
        if (current == null) {
            throw new IllegalStateException("TgClient ещё не инициализирован в Application");
        }
        return current;
    }

    public TdLibClient client() {
        return client;
    }
}
