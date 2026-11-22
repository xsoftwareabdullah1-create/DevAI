package com.devai;

import android.app.Activity;
import android.app.AlertDialog;
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
    static { System.loadLibrary("devai"); }

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        messages = findViewById(R.id.messages);
        chatScroll = findViewById(R.id.chatScroll);
        composer = findViewById(R.id.composer);
        status = findViewById(R.id.status);
        sendButton = findViewById(R.id.sendButton);
        modelButton = findViewById(R.id.modelButton);

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
        checkExistingModel();
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
        if (loading && ready) return;
        loading = true;
        modelButton.setEnabled(false);
        modelButton.setText("Loading model…");
        status.setText("Loading Gemma 4 E2B · This can take a little time…");
        new Thread(() -> {
            String result = nativeInitGemma(model.getAbsolutePath());
            boolean success = "READY".equals(result);
            runOnUiThread(() -> {
                loading = false;
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
            String raw = nativeGenerate(question);
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
        TextView bubble = new TextView(this);
        bubble.setText(sender + "\n" + body);
        bubble.setTextColor(0xFF172238);
        bubble.setTextSize(15);
        bubble.setLineSpacing(3f, 1.0f);
        bubble.setPadding(16, 12, 16, 12);
        bubble.setBackgroundColor(user ? 0xFFD9F3EC : 0xFFF0F4FA);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                user ? (int)(getResources().getDisplayMetrics().widthPixels * 0.82f)
                        : (int)(getResources().getDisplayMetrics().widthPixels * 0.90f),
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = user ? Gravity.END : Gravity.START;
        params.setMargins(0, 6, 0, 6);
        messages.addView(bubble, params);
        chatScroll.post(() -> chatScroll.fullScroll(ScrollView.FOCUS_DOWN));
    }

    @Override
    protected void onDestroy() {
        if (ready) nativeShutdownGemma();
        super.onDestroy();
    }
}
