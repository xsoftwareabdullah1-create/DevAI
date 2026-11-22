package com.devai;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** ChatGPT-style chat screen. Model download lives in {@link SettingsActivity}. */
public final class MainActivity extends Activity implements ModelManager.Listener {

    private static final String STATUS_READY = "GEMMA 4 E2B · OFFLINE";
    private static final long DRAWER_ANIMATION_MS = 220L;
    private static final int REQUEST_PICK_FILES = 4101;
    // The on-device model has a limited context window, so attached text is capped.
    private static final int MAX_ATTACHMENT_CHARS = 8000;
    private static final int MAX_BYTES_PER_FILE = 64 * 1024;
    private static final int MAX_ZIP_ENTRIES = 100;

    private static final class Attachment {
        final Uri uri;
        final String name;
        final long size;
        Attachment(Uri uri, String name, long size) {
            this.uri = uri;
            this.name = name;
            this.size = size;
        }
    }

    private native String nativeInitGemma(String modelPath);
    private native String nativeGenerate(String prompt);
    private native void nativeShutdownGemma();

    private LinearLayout messages;
    private ScrollView chatScroll;
    private View emptyState;
    private View chips;
    private EditText composer;
    private TextView status;
    private ImageButton sendButton;
    private View modelBanner;
    private TextView bannerText;
    private Button bannerButton;
    private View drawerRoot;
    private View drawerPanel;
    private View drawerScrim;
    private HorizontalScrollView attachmentScroll;
    private LinearLayout attachmentBar;
    private final List<Attachment> attachments = new ArrayList<>();

    private TextView pendingReply;
    private Runnable bannerAction;
    private volatile boolean ready = false;
    // True while an answer is being generated or the model is loading.
    private volatile boolean loading = false;
    private volatile boolean nativeLoaded = false;
    // Separate from `loading`: prevents a second refresh from creating another LiteRT-LM engine.
    private volatile boolean initializing = false;
    // Set when loading an installed model failed, so we do not retry in a loop.
    private String initError = null;
    private boolean drawerOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        CrashDiagnostics.install(getApplicationContext());
        CrashDiagnostics.record(getApplicationContext(), "MainActivity onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        messages = findViewById(R.id.messages);
        chatScroll = findViewById(R.id.chatScroll);
        emptyState = findViewById(R.id.emptyState);
        chips = findViewById(R.id.chips);
        composer = findViewById(R.id.composer);
        status = findViewById(R.id.status);
        sendButton = findViewById(R.id.sendButton);
        modelBanner = findViewById(R.id.modelBanner);
        bannerText = findViewById(R.id.bannerText);
        bannerButton = findViewById(R.id.bannerButton);
        drawerRoot = findViewById(R.id.drawerRoot);
        drawerPanel = findViewById(R.id.drawerPanel);
        drawerScrim = findViewById(R.id.drawerScrim);
        attachmentScroll = findViewById(R.id.attachmentScroll);
        attachmentBar = findViewById(R.id.attachmentBar);
        findViewById(R.id.attachButton).setOnClickListener(v -> pickFiles());

        findViewById(R.id.menuButton).setOnClickListener(v -> openDrawer());
        findViewById(R.id.newChatButton).setOnClickListener(v -> newChat());
        findViewById(R.id.drawerNewChat).setOnClickListener(v -> {
            closeDrawer();
            newChat();
        });
        findViewById(R.id.drawerSettings).setOnClickListener(v -> {
            closeDrawer();
            startActivity(new Intent(this, SettingsActivity.class));
        });
        View diagnostics = findViewById(R.id.crashReportButton);
        diagnostics.setOnClickListener(v -> {
            closeDrawer();
            CrashDiagnostics.show(this);
        });
        if (CrashDiagnostics.hasReport(this)) diagnostics.setVisibility(View.VISIBLE);
        drawerScrim.setOnClickListener(v -> closeDrawer());

        bannerButton.setOnClickListener(v -> {
            if (bannerAction != null) bannerAction.run();
        });

        sendButton.setOnClickListener(v -> sendMessage());
        composer.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
        composer.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { updateSendEnabled(); }
        });

        bindChip(R.id.chip1, "Explain a programming concept to me in simple words.");
        bindChip(R.id.chip2, "Write a Python function that checks whether a number is prime.");
        bindChip(R.id.chip3, "Help me debug my code. What details do you need from me?");

        updateEmptyState();
        refreshModelUi();
        CrashDiagnostics.record(this, "UI ready; checking model");
    }

    @Override
    protected void onStart() {
        super.onStart();
        ModelManager.addListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshModelUi();
    }

    @Override
    protected void onStop() {
        ModelManager.removeListener(this);
        super.onStop();
    }

    @Override
    public void onModelStateChanged() {
        refreshModelUi();
    }

    // ---------------------------------------------------------------- model state

    /** Reconciles the UI (and engine) with the model file and download state. */
    private void refreshModelUi() {
        ModelManager.State state = ModelManager.state(this);

        if (state != ModelManager.State.INSTALLED) {
            initError = null;
            // The model was deleted from Settings: unload the engine without blocking the UI.
            if (ready && !loading) {
                ready = false;
                new Thread(() -> {
                    if (nativeLoaded) nativeShutdownGemma();
                }, "devorus-model-unload").start();
            }
        } else if (!ready && !initializing && initError == null) {
            File model = ModelManager.modelFile(this);
            if (model != null) initializeModel(model);
        }

        if (ready) {
            showBanner(null, null, null);
            if (!loading) status.setText(STATUS_READY);
        } else if (initializing) {
            showBanner("Loading Gemma 4 E2B… this can take a little while.", null, null);
            status.setText("Loading model…");
        } else if (state == ModelManager.State.DOWNLOADING) {
            int percent = ModelManager.percent();
            String progress = percent >= 0 ? percent + "%"
                    : ModelManager.formatMegabytes(ModelManager.downloadedBytes());
            showBanner("Downloading Gemma 4 E2B · " + progress, "Settings", this::openSettings);
            status.setText("Downloading model · " + progress);
        } else if (state == ModelManager.State.FAILED) {
            showBanner("Model download failed: " + ModelManager.error(), "Settings", this::openSettings);
            status.setText("Model not installed");
        } else if (state == ModelManager.State.INSTALLED && initError != null) {
            showBanner("Couldn't load the model: " + initError, "Retry", () -> {
                initError = null;
                refreshModelUi();
            });
            status.setText("Model failed to load");
        } else {
            showBanner("Download the Gemma model in Settings to start chatting.", "Settings", this::openSettings);
            status.setText("Model not installed");
        }

        // The composer stays editable so the keyboard always works; sending needs the model.
        composer.setHint(ready ? "Message Dev Orus" : "Message Dev Orus (model not ready yet)");
        chips.setVisibility(ready ? View.VISIBLE : View.GONE);
        updateSendEnabled();
    }

    private void showBanner(String text, String buttonLabel, Runnable action) {
        if (text == null) {
            modelBanner.setVisibility(View.GONE);
            bannerAction = null;
            return;
        }
        modelBanner.setVisibility(View.VISIBLE);
        bannerText.setText(text);
        bannerAction = action;
        if (buttonLabel == null) {
            bannerButton.setVisibility(View.GONE);
        } else {
            bannerButton.setVisibility(View.VISIBLE);
            bannerButton.setText(buttonLabel);
        }
    }

    private void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
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
            return false;
        }
    }

    private void initializeModel(File model) {
        if (ready || initializing) {
            CrashDiagnostics.record(this, "Ignoring duplicate model initialization request; ready="
                    + ready + ", initializing=" + initializing);
            return;
        }
        initializing = true;
        loading = true;
        initError = null;
        new Thread(() -> {
            CrashDiagnostics.record(this, "Gemma initialization started; model bytes=" + model.length());
            String result = ensureNativeLoaded()
                    ? nativeInitGemma(model.getAbsolutePath())
                    : "Native library load failed; open Crash report.";
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
                initError = success ? null : result;
                if (!success) {
                    addMessage("Dev Orus 1.1", "I couldn't load the model: " + result, false);
                }
                refreshModelUi();
            });
        }, "devorus-model-init").start();
        refreshModelUi();
    }

    // ---------------------------------------------------------------- chat

    private void updateSendEnabled() {
        boolean hasContent = composer.getText().toString().trim().length() > 0
                || !attachments.isEmpty();
        sendButton.setEnabled(ready && !loading && hasContent);
    }

    private void bindChip(int id, String prompt) {
        findViewById(id).setOnClickListener(v -> {
            if (!ready || loading) return;
            composer.setText(prompt);
            sendMessage();
        });
    }

    private void newChat() {
        messages.removeAllViews();
        pendingReply = null;
        attachments.clear();
        renderAttachments();
        composer.setText("");
        updateEmptyState();
    }

    private void updateEmptyState() {
        boolean empty = messages.getChildCount() == 0;
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        chatScroll.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void sendMessage() {
        final String question = composer.getText().toString().trim();
        if ((question.isEmpty() && attachments.isEmpty()) || !ready || loading) return;
        final List<Attachment> files = new ArrayList<>(attachments);
        attachments.clear();
        renderAttachments();
        composer.setText("");
        StringBuilder shown = new StringBuilder(question);
        for (Attachment file : files) {
            if (shown.length() > 0) shown.append('\n');
            shown.append("\uD83D\uDCCE ").append(file.name);
        }
        addMessage("You", shown.toString(), true);
        pendingReply = addMessage("Dev Orus 1.1", "Thinking…", false);
        loading = true;
        status.setText("Dev Orus is thinking…");
        updateSendEnabled();

        final TextView target = pendingReply;
        new Thread(() -> {
            CrashDiagnostics.record(this, "Inference started; prompt chars=" + question.length());
            String prompt = files.isEmpty() ? question : buildPrompt(question, files);
            String raw = ensureNativeLoaded()
                    ? nativeGenerate(prompt)
                    : "{\"error\":\"Native library unavailable. Open Crash report.\"}";
            CrashDiagnostics.record(this, "Inference returned");
            String answer = extractAnswer(raw);
            runOnUiThread(() -> {
                target.setText(answer);
                if (pendingReply == target) pendingReply = null;
                loading = false;
                refreshModelUi();
                scrollToBottom();
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

    /** Adds a message row and returns its text view so it can be updated later. */
    private TextView addMessage(String sender, String body, boolean user) {
        TextView content = new TextView(this);
        content.setText(body);
        content.setTextColor(getResources().getColor(R.color.text_primary, getTheme()));
        content.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        content.setLineSpacing(dp(3), 1.0f);
        content.setTextIsSelectable(false);
        content.setOnLongClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText("Dev Orus message", content.getText()));
                Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show();
            }
            return true;
        });

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, dp(8), 0, dp(8));

        if (user) {
            row.setGravity(Gravity.END);
            content.setBackgroundResource(R.drawable.bg_user_bubble);
            content.setPadding(dp(16), dp(10), dp(16), dp(10));
            content.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * 0.78f));
            row.addView(content, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        } else {
            TextView avatar = new TextView(this);
            avatar.setText("D");
            avatar.setGravity(Gravity.CENTER);
            avatar.setTextColor(0xFFFFFFFF);
            avatar.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            avatar.setTypeface(avatar.getTypeface(), android.graphics.Typeface.BOLD);
            avatar.setBackgroundResource(R.drawable.bg_avatar);
            LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dp(28), dp(28));
            avatarParams.setMargins(0, dp(2), dp(12), 0);
            row.addView(avatar, avatarParams);
            row.addView(content, new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        }

        messages.addView(row, rowParams);
        updateEmptyState();
        scrollToBottom();
        return content;
    }

    private void scrollToBottom() {
        chatScroll.post(() -> chatScroll.fullScroll(ScrollView.FOCUS_DOWN));
    }

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics()));
    }

    // ---------------------------------------------------------------- attachments

    private void pickFiles() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*"); // every file type, including zip archives
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        try {
            startActivityForResult(intent, REQUEST_PICK_FILES);
        } catch (Exception e) {
            Toast.makeText(this, "No file picker available on this device.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_PICK_FILES || resultCode != RESULT_OK || data == null) return;
        List<Uri> uris = new ArrayList<>();
        if (data.getClipData() != null) {
            for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                uris.add(data.getClipData().getItemAt(i).getUri());
            }
        } else if (data.getData() != null) {
            uris.add(data.getData());
        }
        for (Uri uri : uris) attachments.add(describe(uri));
        renderAttachments();
        updateSendEnabled();
    }

    private Attachment describe(Uri uri) {
        String name = null;
        long size = -1L;
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (nameIndex >= 0) name = cursor.getString(nameIndex);
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex);
            }
        } catch (Exception ignored) {
            // Fall back to the last path segment below.
        }
        if (name == null || name.isEmpty()) {
            name = uri.getLastPathSegment() == null ? "file" : uri.getLastPathSegment();
        }
        return new Attachment(uri, name, size);
    }

    private void renderAttachments() {
        attachmentBar.removeAllViews();
        attachmentScroll.setVisibility(attachments.isEmpty() ? View.GONE : View.VISIBLE);
        for (Attachment file : new ArrayList<>(attachments)) {
            TextView chip = new TextView(this);
            chip.setText("\uD83D\uDCCE " + file.name + "  \u2715");
            chip.setSingleLine(true);
            chip.setMaxWidth(dp(220));
            chip.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
            chip.setTextColor(getResources().getColor(R.color.text_primary, getTheme()));
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            chip.setBackgroundResource(R.drawable.bg_chip);
            chip.setPadding(dp(12), dp(8), dp(12), dp(8));
            chip.setOnClickListener(v -> {
                attachments.remove(file);
                renderAttachments();
                updateSendEnabled();
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, dp(8), 0);
            attachmentBar.addView(chip, params);
        }
    }

    /** Builds the model prompt: attached file contents (as text where possible) plus the question. */
    private String buildPrompt(String question, List<Attachment> files) {
        StringBuilder out = new StringBuilder();
        int[] budget = {MAX_ATTACHMENT_CHARS};
        for (Attachment file : files) {
            try (InputStream raw = getContentResolver().openInputStream(file.uri)) {
                if (raw == null) throw new IOException("could not open file");
                BufferedInputStream in = new BufferedInputStream(raw);
                in.mark(8);
                byte[] magic = new byte[4];
                int got = in.read(magic);
                in.reset();
                boolean zipMagic = got == 4 && magic[0] == 'P' && magic[1] == 'K'
                        && magic[2] == 3 && magic[3] == 4;
                if (zipMagic && file.name.toLowerCase().endsWith(".zip")) {
                    appendZip(out, file, in, budget);
                } else {
                    appendFile(out, file.name, file.size, readUpTo(in, MAX_BYTES_PER_FILE), budget);
                }
            } catch (Exception e) {
                out.append("File: ").append(file.name).append(" \u2014 could not be read (")
                        .append(e.getMessage()).append(").\n\n");
            }
        }
        if (question.isEmpty()) question = "Please look at the attached file(s) and summarize them.";
        out.append("User request: ").append(question);
        return out.toString();
    }

    private void appendZip(StringBuilder out, Attachment file, InputStream in, int[] budget)
            throws IOException {
        List<String> listing = new ArrayList<>();
        StringBuilder contents = new StringBuilder();
        int count = 0;
        try (ZipInputStream zip = new ZipInputStream(in)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null && count < MAX_ZIP_ENTRIES) {
                if (entry.isDirectory()) continue;
                count++;
                listing.add(entry.getName());
                if (budget[0] > 0) {
                    appendFile(contents, file.name + "/" + entry.getName(), entry.getSize(),
                            readUpTo(zip, MAX_BYTES_PER_FILE), budget);
                }
            }
        }
        out.append("Archive: ").append(file.name).append(" (").append(formatSize(file.size))
                .append(") with ").append(count).append(count >= MAX_ZIP_ENTRIES ? "+" : "")
                .append(" files:\n");
        for (String name : listing) out.append("- ").append(name).append('\n');
        out.append('\n').append(contents);
    }

    private void appendFile(StringBuilder out, String name, long size, byte[] bytes, int[] budget) {
        if (!looksLikeText(bytes)) {
            out.append("File: ").append(name).append(" (").append(formatSize(size))
                    .append(") \u2014 binary or unsupported format; its contents cannot be read by this model.\n\n");
            return;
        }
        if (budget[0] <= 0) {
            out.append("File: ").append(name).append(" \u2014 skipped (attachment size limit reached).\n\n");
            return;
        }
        String text = new String(bytes, StandardCharsets.UTF_8);
        boolean truncated = text.length() > budget[0] || size > bytes.length;
        if (text.length() > budget[0]) text = text.substring(0, budget[0]);
        budget[0] -= text.length();
        out.append("File: ").append(name).append(" (").append(formatSize(size)).append(")\n```\n")
                .append(text).append(truncated ? "\n[truncated]" : "").append("\n```\n\n");
    }

    private static byte[] readUpTo(InputStream in, int max) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while (result.size() < max
                && (n = in.read(buffer, 0, Math.min(buffer.length, max - result.size()))) != -1) {
            result.write(buffer, 0, n);
        }
        return result.toByteArray();
    }

    private static boolean looksLikeText(byte[] data) {
        int length = Math.min(data.length, 4096);
        for (int i = 0; i < length; i++) {
            if (data[i] == 0) return false;
        }
        return true;
    }

    private static String formatSize(long bytes) {
        if (bytes < 0) return "unknown size";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return (bytes / (1024 * 1024)) + " MB";
    }

    // ---------------------------------------------------------------- drawer

    private void openDrawer() {
        if (drawerOpen) return;
        drawerOpen = true;
        hideKeyboard();
        drawerRoot.setVisibility(View.VISIBLE);
        drawerScrim.setAlpha(0f);
        drawerPanel.setTranslationX(-dp(288));
        drawerScrim.animate().alpha(1f).setDuration(DRAWER_ANIMATION_MS).start();
        drawerPanel.animate().translationX(0f).setDuration(DRAWER_ANIMATION_MS).start();
    }

    private void closeDrawer() {
        if (!drawerOpen) return;
        drawerOpen = false;
        drawerScrim.animate().alpha(0f).setDuration(DRAWER_ANIMATION_MS).start();
        drawerPanel.animate().translationX(-dp(288)).setDuration(DRAWER_ANIMATION_MS)
                .withEndAction(() -> {
                    if (!drawerOpen) drawerRoot.setVisibility(View.GONE);
                }).start();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(composer.getWindowToken(), 0);
    }

    @Override
    public void onBackPressed() {
        if (drawerOpen) {
            closeDrawer();
        } else {
            super.onBackPressed();
        }
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
