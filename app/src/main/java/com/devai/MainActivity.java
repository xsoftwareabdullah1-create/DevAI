package com.devai;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public final class MainActivity extends Activity {

    private static final String MODEL_URL =
            "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm?download=true";
    private static final long MIN_MODEL_BYTES = 2_000_000_000L;
    private static final long REQUIRED_FREE_BYTES = 3_200_000_000L;

    private native String nativeInitGemma(String modelPath);
    private native String nativeGenerate(String prompt);
    private native void nativeShutdownGemma();

    private LinearLayout messages;
    private ScrollView chatScroll;
    private EditText composer;
    private TextView status;
    private Button sendButton;
    private Button modelButton;
    private volatile boolean ready = false;
    private volatile boolean loading = false;
    private volatile boolean nativeLoaded = false;
    // Separate from `loading`: downloading and inference also use that flag.
    // This prevents a second refresh/tap from creating another LiteRT-LM engine.
    private volatile boolean initializing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        CrashDiagnostics.install(getApplicationContext());
        CrashDiagnostics.record(getApplicationContext(), "MainActivity onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        messages = findViewById(R.id.messages);
        chatScroll = findViewById(R.id.chatScroll);
        composer = findViewById(R.id.composer);
        status = findViewById(R.id.status);
        sendButton = findViewById(R.id.sendButton);
        modelButton = findViewById(R.id.modelButton);
        Button newChatButton = findViewById(R.id.newChatButton);
        newChatButton.setOnClickListener(v -> {
            messages.removeAllViews();
            addMessage("Dev Orus 1.1", "New chat started. How can I help you?", false);
        });
        Button diagnosticsButton = findViewById(R.id.crashReportButton);
        diagnosticsButton.setOnClickListener(v -> CrashDiagnostics.show(this));
        if (CrashDiagnostics.hasReport(this)) diagnosticsButton.setVisibility(android.view.View.VISIBLE);

        addMessage("Dev Orus 1.1",
                "Welcome. This build uses the Gemma 4 E2B model locally through LiteRT-LM. "
                + "The model is downloaded once; after setup, chat inference runs on this device.",
                false);
        sendButton.setEnabled(false);
        composer.setEnabled(false);
        modelButton.setOnClickListener(v -> onModelButtonClicked());
        sendButton.setOnClickListener(v -> sendMessage());
        composer.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
        CrashDiagnostics.record(this, "UI ready; checking model");
        checkExistingModel();
    }

    private boolean ensureNativeLoaded() {
        if (nativeLoaded) return true;
        try {
            CrashDiagnostics.record(this, "Loading libdevai.so");
            System.loadLibrary("devai");
            nativeLoaded = true;
            CrashDiagnostics.record(this, "Native library loaded");
            return true;
        } catch (Throwable error) {
            CrashDiagnostics.record(this, "Native library load failed: " + error);
            status.setText("Native runtime error · Open Crash report");
            return false;
        }
    }

    private File modelFile() {
        File directory = new File(getFilesDir(), "models");
        if (!directory.exists() && !directory.mkdirs()) return null;
        return new File(directory, "gemma-4-E2B-it.litertlm");
    }

    private void checkExistingModel() {
        File model = modelFile();
        if (model != null && model.isFile() && model.length() >= MIN_MODEL_BYTES) {
            initializeModel(model);
        } else {
            if (model != null && model.exists()) model.delete();
            status.setText("MODEL NOT INSTALLED · First-time download required");
            modelButton.setText("Download Gemma model · about 2.6 GB");
            modelButton.setEnabled(true);
            // Show the first-run download popup as soon as the app opens.
            modelButton.post(this::confirmModelDownload);
        }
    }

    private void onModelButtonClicked() {
        File model = modelFile();
        if (model != null && model.isFile() && model.length() >= MIN_MODEL_BYTES) {
            initializeModel(model);
        } else {
            confirmModelDownload();
        }
    }

    private void confirmModelDownload() {
        new AlertDialog.Builder(this)
                .setTitle("Download Gemma 4 E2B?")
                .setMessage("This is a one-time download of about 2.6 GB. Use Wi-Fi and make sure at least 3.2 GB of free storage is available. After download, model responses run offline.")
                .setNegativeButton("Not now", null)
                .setPositiveButton("Download", (dialog, which) -> downloadModel())
                .show();
    }

    private void downloadModel() {
        if (loading) return;
        File model = modelFile();
        if (model == null) {
            status.setText("Could not create the private model folder.");
            return;
        }
        if (model.getParentFile().getUsableSpace() < REQUIRED_FREE_BYTES) {
            status.setText("Not enough free storage. Please free at least 3.2 GB and retry.");
            return;
        }

        loading = true;
        modelButton.setEnabled(false);
        modelButton.setText("Downloading…");
        status.setText("Connecting to model host…");

        new Thread(() -> {
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
                long downloaded = 0L;
                long lastUpdate = 0L;
                try (InputStream input = new BufferedInputStream(connection.getInputStream());
                     BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(partial))) {
                    byte[] buffer = new byte[1024 * 256];
                    int count;
                    while ((count = input.read(buffer)) != -1) {
                        output.write(buffer, 0, count);
                        downloaded += count;
                        long now = System.currentTimeMillis();
                        if (now - lastUpdate > 500) {
                            final long current = downloaded;
                            final long expected = total;
                            runOnUiThread(() -> {
                                if (expected > 0) {
                                    int percent = (int) Math.min(100, current * 100L / expected);
                                    status.setText("Downloading Gemma 4 E2B · " + percent + "% · "
                                            + (current / (1024 * 1024)) + " MB");
                                    modelButton.setText(percent + "%");
                                } else {
                                    status.setText("Downloading Gemma 4 E2B · "
                                            + (current / (1024 * 1024)) + " MB");
                                }
                            });
                            lastUpdate = now;
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
                runOnUiThread(() -> initializeModel(model));
            } catch (Exception e) {
                partial.delete();
                runOnUiThread(() -> {
                    loading = false;
                    modelButton.setEnabled(true);
                    modelButton.setText("Retry model download");
                    status.setText("Download failed: " + e.getMessage());
                    addMessage("Dev Orus 1.1",
                            "The model was not installed. Check your connection and free storage, then retry.",
                            false);
                });
            } finally {
                if (connection != null) connection.disconnect();
            }
        }, "devorus-model-download").start();
    }

    private void initializeModel(File model) {
        if (ready || initializing) {
            CrashDiagnostics.record(this, "Ignoring duplicate model initialization request; ready="
                    + ready + ", initializing=" + initializing);
            return;
        }
        initializing = true;
        loading = true;
        modelButton.setEnabled(false);
        modelButton.setText("Loading model…");
        status.setText("Loading Gemma 4 E2B · This can take a little time…");
        new Thread(() -> {
            CrashDiagnostics.record(this, "Gemma initialization started; model bytes=" + model.length());
            String result = ensureNativeLoaded() ? nativeInitGemma(model.getAbsolutePath()) : "Native library load failed; open Crash report.";
            CrashDiagnostics.record(this, "Gemma initialization finished: " + result);
            boolean success = "READY".equals(result);
            runOnUiThread(() -> {
                initializing = false;
                loading = false;
                if (isFinishing() || isDestroyed()) {
                    if (success) nativeShutdownGemma();
                    ready = false;
                    CrashDiagnostics.record(getApplicationContext(),
                            "Gemma initialization completed after Activity destruction; engine closed");
                    return;
                }
                ready = success;
                sendButton.setEnabled(success);
                composer.setEnabled(success);
                modelButton.setEnabled(!success);
                modelButton.setText(success ? "Model ready · Offline" : "Retry model loading");
                status.setText(success
                        ? "DEV ORUS 1.1 · GEMMA 4 E2B · OFFLINE"
                        : "Model initialization failed");
                addMessage("Dev Orus 1.1", success
                        ? "I'm ready. The local Gemma model is loaded and chat works without an internet connection."
                        : "I couldn't load the model: " + result, false);
            });
        }, "devorus-model-init").start();
    }

    private void sendMessage() {
        final String question = composer.getText().toString().trim();
        if (question.isEmpty() || !ready || loading) return;
        composer.setText("");
        addMessage("You", question, true);
        sendButton.setEnabled(false);
        status.setText("Dev Orus 1.1 is thinking…");
        loading = true;

        new Thread(() -> {
            CrashDiagnostics.record(this, "Inference started; prompt chars=" + question.length());
            String raw = ensureNativeLoaded() ? nativeGenerate(question) : "{\"error\":\"Native library unavailable. Open Crash report.\"}";
            CrashDiagnostics.record(this, "Inference returned");
            String answer = extractAnswer(raw);
            runOnUiThread(() -> {
                addMessage("Dev Orus 1.1", answer, false);
                loading = false;
                sendButton.setEnabled(ready);
                status.setText("DEV ORUS 1.1 · GEMMA 4 E2B · OFFLINE");
            });
        }, "devorus-inference").start();
    }

    private String extractAnswer(String raw) {
        try {
            JSONObject response = new JSONObject(raw);
            if (response.has("error")) return response.optString("error");
            Object content = response.opt("content");
            if (content instanceof String) return (String) content;
            if (content instanceof JSONObject) {
                JSONObject object = (JSONObject) content;
                return object.optString("text", object.toString());
            }
            if (content instanceof JSONArray) {
                JSONArray parts = (JSONArray) content;
                StringBuilder text = new StringBuilder();
                for (int i = 0; i < parts.length(); i++) {
                    JSONObject part = parts.optJSONObject(i);
                    if (part != null) text.append(part.optString("text", ""));
                }
                if (text.length() > 0) return text.toString().trim();
            }
            return response.optString("text", raw);
        } catch (Exception ignored) {
            return raw;
        }
    }

    private void addMessage(String sender, String body, boolean user) {
        LinearLayout message = new LinearLayout(this);
        message.setOrientation(LinearLayout.VERTICAL);
        message.setPadding(user ? 15 : 2, 4, user ? 15 : 2, 4);

        TextView author = new TextView(this);
        author.setText(user ? "You" : "Dev Orus");
        author.setTextColor(user ? 0xFF626B7A : 0xFF202A44);
        author.setTextSize(12);
        author.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        author.setPadding(2, 0, 2, 6);
        message.addView(author);

        TextView content = new TextView(this);
        content.setText(body);
        content.setTextColor(0xFF252936);
        content.setTextSize(15);
        content.setLineSpacing(4f, 1.0f);
        content.setPadding(user ? 14 : 2, user ? 11 : 2, user ? 14 : 2, user ? 11 : 2);
        if (user) {
            GradientDrawable bubble = new GradientDrawable();
            bubble.setColor(0xFFF0F1F4);
            bubble.setCornerRadius(22f);
            content.setBackground(bubble);
        }
        message.addView(content);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                user ? LinearLayout.LayoutParams.WRAP_CONTENT : LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = user ? Gravity.END : Gravity.START;
        params.setMargins(0, 7, 0, 15);
        messages.addView(message, params);
        chatScroll.post(() -> chatScroll.fullScroll(ScrollView.FOCUS_DOWN));
    }

    @Override
    protected void onDestroy() {
        CrashDiagnostics.record(this, "Activity onDestroy");
        if (nativeLoaded && (ready || initializing)) {
            // JNI serializes shutdown against initialization/inference. Closing here also
            // prevents the large model engine remaining resident after Activity teardown.
            nativeShutdownGemma();
            ready = false;
            initializing = false;
        }
        super.onDestroy();
    }
}
