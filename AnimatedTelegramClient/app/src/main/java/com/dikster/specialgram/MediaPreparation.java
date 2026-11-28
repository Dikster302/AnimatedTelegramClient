package com.dikster.specialgram;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import java.io.File;
import java.util.Locale;
import kotlin.Pair;

/** File preparation and media validation shared by Specialgram UI flows. */
public final class MediaPreparation {
    private MediaPreparation() { }

    public static Pair<File, Double> prepareStoryVideo(Context context, Uri uri) throws Exception {
        File file = new File(context.getCacheDir(),
                "specialgram_story_video_" + System.currentTimeMillis() + ".mp4");
        try (java.io.InputStream input = context.getContentResolver().openInputStream(uri)) {
            if (input == null) throw new IllegalStateException("Не удалось открыть видео");
            try (java.io.OutputStream output = new java.io.FileOutputStream(file)) {
                byte[] buffer = new byte[64 * 1024];
                int count;
                while ((count = input.read(buffer)) >= 0) output.write(buffer, 0, count);
            }
        }
        require(file.length() >= 1 && file.length() <= 30L * 1024L * 1024L,
                "Размер видео должен быть не более 30 МБ");
        MediaExtractor extractor = new MediaExtractor();
        try {
            extractor.setDataSource(file.getAbsolutePath());
            int track = -1;
            MediaFormat format = null;
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat candidate = extractor.getTrackFormat(i);
                String mime = candidate.getString(MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith("video/")) {
                    track = i;
                    format = candidate;
                    break;
                }
            }
            require(track >= 0 && format != null, "В файле не найдена видеодорожка");
            String mime = format.getString(MediaFormat.KEY_MIME);
            require("video/hevc".equals(mime),
                    "Нужен MP4 с кодеком H.265 (HEVC). Обычный H.264 пока не поддерживается.");
            int width = format.getInteger(MediaFormat.KEY_WIDTH);
            int height = format.getInteger(MediaFormat.KEY_HEIGHT);
            require(width == 720 && height == 1280, "Разрешение видео должно быть 720×1280");
            long durationUs = format.getLong(MediaFormat.KEY_DURATION);
            double duration = durationUs / 1_000_000.0;
            require(duration >= 0.1 && duration <= 60.0, "Длительность должна быть не более 60 секунд");
            extractor.selectTrack(track);
            long lastSyncUs = -1L, firstSyncUs = -1L, lastSampleUs = 0L;
            while (true) {
                long sampleTime = extractor.getSampleTime();
                if (sampleTime < 0) break;
                lastSampleUs = sampleTime;
                if ((extractor.getSampleFlags() & MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                    if (firstSyncUs < 0) firstSyncUs = sampleTime;
                    if (lastSyncUs >= 0)
                        require(sampleTime - lastSyncUs <= 1_500_000L,
                                "В видео должны быть ключевые кадры примерно каждую секунду");
                    lastSyncUs = sampleTime;
                }
                if (!extractor.advance()) break;
            }
            require(firstSyncUs >= 0 && firstSyncUs <= 100_000L && lastSyncUs >= 0
                            && durationUs - lastSyncUs <= 1_500_000L,
                    "Видео должно начинаться с ключевого кадра и содержать ключевые кадры примерно каждую секунду");
            require(lastSampleUs <= durationUs + 100_000L, "Некорректная длительность видео");
            return new Pair<>(file, duration);
        } finally {
            extractor.release();
        }
    }

    public static File prepareProfilePhoto(Context context, Uri uri) throws Exception {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (java.io.InputStream input = context.getContentResolver().openInputStream(uri)) {
            if (input == null) throw new IllegalStateException("Не удалось открыть фотографию");
            BitmapFactory.decodeStream(input, null, bounds);
        }
        require(bounds.outWidth > 0 && bounds.outHeight > 0, "Некорректное изображение");
        int sample = 1;
        while (bounds.outWidth / sample > 2048 || bounds.outHeight / sample > 2048) sample *= 2;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        Bitmap decoded;
        try (java.io.InputStream input = context.getContentResolver().openInputStream(uri)) {
            decoded = input == null ? null : BitmapFactory.decodeStream(input, null, options);
        }
        if (decoded == null) throw new IllegalStateException("Не удалось прочитать фотографию");
        Bitmap cropped = null, scaled = null;
        try {
            int side = Math.min(decoded.getWidth(), decoded.getHeight());
            cropped = Bitmap.createBitmap(decoded, (decoded.getWidth() - side) / 2,
                    (decoded.getHeight() - side) / 2, side, side);
            scaled = Bitmap.createScaledBitmap(cropped, 640, 640, true);
            File target = new File(context.getCacheDir(),
                    "specialgram_profile_" + System.currentTimeMillis() + ".jpg");
            try (java.io.OutputStream output = new java.io.FileOutputStream(target)) {
                if (!scaled.compress(Bitmap.CompressFormat.JPEG, 88, output))
                    throw new IllegalStateException("Не удалось сохранить фотографию");
            }
            require(target.length() <= 10L * 1024L * 1024L, "Фото профиля превышает 10 МБ");
            return target;
        } finally {
            decoded.recycle();
            if (cropped != null && cropped != decoded) cropped.recycle();
            if (scaled != null && scaled != cropped) scaled.recycle();
        }
    }

    public static File prepareStoryPhoto(Context context, Uri uri) throws Exception {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (java.io.InputStream input = context.getContentResolver().openInputStream(uri)) {
            if (input == null) throw new IllegalStateException("Не удалось открыть изображение");
            BitmapFactory.decodeStream(input, null, bounds);
        }
        require(bounds.outWidth > 0 && bounds.outHeight > 0, "Некорректное изображение");
        int sample = 1;
        while (bounds.outWidth / sample > 2160 || bounds.outHeight / sample > 3840) sample *= 2;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        Bitmap decoded;
        try (java.io.InputStream input = context.getContentResolver().openInputStream(uri)) {
            decoded = input == null ? null : BitmapFactory.decodeStream(input, null, options);
        }
        if (decoded == null) throw new IllegalStateException("Не удалось прочитать изображение");
        Bitmap cropped = null, outputBitmap = null;
        try {
            float targetRatio = 9f / 16f;
            float sourceRatio = (float) decoded.getWidth() / decoded.getHeight();
            int cropWidth, cropHeight;
            if (sourceRatio > targetRatio) {
                cropHeight = decoded.getHeight();
                cropWidth = Math.max(1, (int) (cropHeight * targetRatio));
            } else {
                cropWidth = decoded.getWidth();
                cropHeight = Math.max(1, (int) (cropWidth / targetRatio));
            }
            cropped = Bitmap.createBitmap(decoded, (decoded.getWidth() - cropWidth) / 2,
                    (decoded.getHeight() - cropHeight) / 2, cropWidth, cropHeight);
            outputBitmap = Bitmap.createScaledBitmap(cropped, 1080, 1920, true);
            File target = new File(context.getCacheDir(),
                    "specialgram_story_" + System.currentTimeMillis() + ".jpg");
            int quality = 90;
            do {
                try (java.io.OutputStream output = new java.io.FileOutputStream(target)) {
                    if (!outputBitmap.compress(Bitmap.CompressFormat.JPEG, quality, output))
                        throw new IllegalStateException("Не удалось сжать изображение");
                }
                quality -= 5;
            } while (target.length() > 10L * 1024L * 1024L && quality >= 70);
            require(target.length() <= 10L * 1024L * 1024L, "Фото превышает лимит Telegram 10 МБ");
            return target;
        } finally {
            decoded.recycle();
            if (cropped != null && cropped != decoded) cropped.recycle();
            if (outputBitmap != null && outputBitmap != cropped) outputBitmap.recycle();
        }
    }

    public static File prepareOutgoingMedia(Context context, Uri uri, String prefix, long maxBytes)
            throws Exception {
        String name = null;
        try (android.database.Cursor cursor = context.getContentResolver().query(
                uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) name = cursor.getString(0);
        }
        if (name != null) {
            name = name.substring(name.lastIndexOf('/') + 1);
            if (name.length() > 100) name = name.substring(0, 100);
        }
        String extension = "";
        if (name != null && name.contains(".")) {
            String candidate = name.substring(name.lastIndexOf('.') + 1);
            if (candidate.matches("[A-Za-z0-9]{1,8}")) extension = "." + candidate;
        }
        File target = new File(context.getCacheDir(),
                prefix + "_" + System.currentTimeMillis() + extension);
        try (java.io.InputStream input = context.getContentResolver().openInputStream(uri)) {
            if (input == null) throw new IllegalStateException("Не удалось открыть выбранный файл");
            try (java.io.OutputStream output = new java.io.FileOutputStream(target)) {
                byte[] buffer = new byte[64 * 1024];
                long total = 0L;
                int count;
                while ((count = input.read(buffer)) >= 0) {
                    total += count;
                    require(total <= maxBytes, "Файл превышает лимит "
                            + (maxBytes / (1024L * 1024L)) + " МБ");
                    output.write(buffer, 0, count);
                }
            }
        }
        require(target.length() > 0L, "Выбран пустой файл");
        return target;
    }

    public static void openDownloadedMedia(Context context, String path, String mimeType) {
        if (path == null || path.isEmpty()) {
            Toast.makeText(context, "Файл ещё загружается", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            File file = new File(path);
            require(file.exists(), "Файл ещё не загружен");
            Uri uri = FileProvider.getUriForFile(context,
                    context.getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, mimeType == null || mimeType.isEmpty() ? "*/*" : mimeType)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(intent);
        } catch (Exception error) {
            String message = error.getMessage() == null
                    ? "нет подходящего приложения" : error.getMessage();
            Toast.makeText(context, "Не удалось открыть файл: " + message, Toast.LENGTH_LONG).show();
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }
}
