package com.devai;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Owns the Gemma model file and its download. Shared by the chat screen and the
 * Settings screen so the download keeps running while the user switches screens.
 * All listener callbacks are delivered on the main thread.
 */
final class ModelManager {

    enum State { MISSING, DOWNLOADING, INSTALLED, FAILED }

    interface Listener {
        void onModelStateChanged();
    }

    static final String MODEL_URL =
            "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm?download=true";
    static final long MIN_MODEL_BYTES = 2_000_000_000L;
    static final long REQUIRED_FREE_BYTES = 3_200_000_000L;

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final CopyOnWriteArrayList<Listener> LISTENERS = new CopyOnWriteArrayList<>();

    private static volatile State state = State.MISSING;
    private static volatile long downloadedBytes = 0L;
    private static volatile long totalBytes = 0L;
    private static volatile String error = "";
    private static volatile boolean cancelRequested = false;

    private ModelManager() {}

    static void addListener(Listener listener) {
        if (!LISTENERS.contains(listener)) LISTENERS.add(listener);
    }

    static void removeListener(Listener listener) {
        LISTENERS.remove(listener);
    }

    static File modelFile(Context context) {
        File directory = new File(context.getApplicationContext().getFilesDir(), "models");
        if (!directory.exists() && !directory.mkdirs()) return null;
        return new File(directory, "gemma-4-E2B-it.litertlm");
    }

    static boolean isInstalled(Context context) {
        File model = modelFile(context);
        return model != null && model.isFile() && model.length() >= MIN_MODEL_BYTES;
    }

    /** Current state, reconciled with what is actually on disk when nothing is downloading. */
    static State state(Context context) {
        if (state == State.DOWNLOADING) return State.DOWNLOADING;
        if (isInstalled(context)) {
            state = State.INSTALLED;
        } else if (state == State.INSTALLED) {
            state = State.MISSING;
        }
        return state;
    }

    static long downloadedBytes() { return downloadedBytes; }

    static long totalBytes() { return totalBytes; }

    static String error() { return error; }

    /** 0-100, or -1 when the server did not report a size. */
    static int percent() {
        long total = totalBytes;
        if (total <= 0) return -1;
        return (int) Math.min(100L, downloadedBytes * 100L / total);
    }

    /** Returns an error message, or null if the download was started. */
    static String startDownload(Context context) {
        final Context app = context.getApplicationContext();
        if (state == State.DOWNLOADING) return null;
        final File model = modelFile(app);
        if (model == null) return "Could not create the private model folder.";
        if (model.getParentFile().getUsableSpace() < REQUIRED_FREE_BYTES) {
            return "Not enough free storage. Please free at least 3.2 GB and retry.";
        }
        cancelRequested = false;
        downloadedBytes = 0L;
        totalBytes = 0L;
        error = "";
        state = State.DOWNLOADING;
        notifyChanged();

        new Thread(() -> runDownload(model), "devorus-model-download").start();
        return null;
    }

    static void cancelDownload() {
        if (state == State.DOWNLOADING) cancelRequested = true;
    }

    /** Deletes the installed model. Returns false if the file could not be removed. */
    static boolean deleteModel(Context context) {
        if (state == State.DOWNLOADING) return false;
        File model = modelFile(context);
        boolean ok = model == null || !model.exists() || model.delete();
        state = State.MISSING;
        downloadedBytes = 0L;
        totalBytes = 0L;
        notifyChanged();
        return ok;
    }

    private static void runDownload(File model) {
        File partial = new File(model.getParentFile(), model.getName() + ".part");
        HttpURLConnection connection = null;
        try {
            URL url = new URL(MODEL_URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(120000);
            connection.setRequestProperty("User-Agent", "DevAI-DevOrus/1.1 Android");
            connection.connect();

            int response = connection.getResponseCode();
            if (response < 200 || response >= 300) {
                throw new IllegalStateException("Model host returned HTTP " + response);
            }

            long total = connection.getContentLengthLong();
            totalBytes = Math.max(total, 0L);
            long downloaded = 0L;
            long lastUpdate = 0L;
            try (InputStream input = new BufferedInputStream(connection.getInputStream());
                 BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(partial))) {
                byte[] buffer = new byte[1024 * 256];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    if (cancelRequested) throw new CancelledException();
                    output.write(buffer, 0, count);
                    downloaded += count;
                    downloadedBytes = downloaded;
                    long now = System.currentTimeMillis();
                    if (now - lastUpdate > 500) {
                        lastUpdate = now;
                        notifyChanged();
                    }
                }
            }

            if (downloaded < MIN_MODEL_BYTES) {
                throw new IllegalStateException("Downloaded file is smaller than the expected Gemma model.");
            }
            if (total > 0 && downloaded != total) {
                throw new IllegalStateException("Download size did not match the server's content length.");
            }
            if (model.exists() && !model.delete()) {
                throw new IllegalStateException("Could not replace the previous model file.");
            }
            if (!partial.renameTo(model)) {
                throw new IllegalStateException("Could not finalize the downloaded model.");
            }
            state = State.INSTALLED;
            error = "";
        } catch (CancelledException cancelled) {
            partial.delete();
            downloadedBytes = 0L;
            totalBytes = 0L;
            state = State.MISSING;
        } catch (Exception e) {
            partial.delete();
            String message = e.getMessage();
            error = message == null || message.isEmpty() ? e.getClass().getSimpleName() : message;
            state = State.FAILED;
        } finally {
            if (connection != null) connection.disconnect();
            cancelRequested = false;
            notifyChanged();
        }
    }

    private static void notifyChanged() {
        MAIN.post(() -> {
            for (Listener listener : LISTENERS) listener.onModelStateChanged();
        });
    }

    static String formatMegabytes(long bytes) {
        return (bytes / (1024 * 1024)) + " MB";
    }

    private static final class CancelledException extends RuntimeException {
        CancelledException() { super("cancelled"); }
    }
}
