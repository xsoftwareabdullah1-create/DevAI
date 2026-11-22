package com.devai;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public final class MainActivity extends Activity {
    static { System.loadLibrary("devai"); }

    private native String nativeTrainCorpus(String corpus);
    private native String nativeAsk(String question);

    private LinearLayout messages;
    private ScrollView chatScroll;
    private EditText composer;
    private TextView status;
    private Button sendButton;
    private volatile boolean ready = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        messages = findViewById(R.id.messages);
        chatScroll = findViewById(R.id.chatScroll);
        composer = findViewById(R.id.composer);
        status = findViewById(R.id.status);
        sendButton = findViewById(R.id.sendButton);

        addMessage("DevAI", "Hi! I'm Dev Orus 1.1. I'm loading my own offline knowledge. I can answer from examples I've learned, but this first version retrieves known answers rather than writing completely new ones.", false);
        sendButton.setEnabled(false);
        composer.setEnabled(false);
        sendButton.setOnClickListener(v -> sendMessage());
        composer.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
        loadAndTrainOffline();
    }

    private void loadAndTrainOffline() {
        status.setText("Preparing Dev Orus 1.1 · Offline training…");
        new Thread(() -> {
            String result;
            try (InputStream in = getAssets().open("devai_corpus.tsv");
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
                String corpus = new String(out.toByteArray(), "UTF-8");
                result = nativeTrainCorpus(corpus);
                ready = result.startsWith("Offline retrieval training complete.");
            } catch (Exception e) {
                result = "Offline training could not start: " + e.getMessage();
            }
            final String trainingResult = result;
            runOnUiThread(() -> {
                status.setText(ready ? "OFFLINE · " + "210 starter records loaded" : "OFFLINE · Knowledge unavailable");
                sendButton.setEnabled(ready);
                composer.setEnabled(ready);
                if (ready) addMessage("DevAI", "I'm ready. Ask me something from my current knowledge.", false);
                else addMessage("DevAI", trainingResult, false);
            });
        }).start();
    }

    private void sendMessage() {
        final String question = composer.getText().toString().trim();
        if (question.isEmpty() || !ready) return;
        composer.setText("");
        addMessage("You", question, true);
        sendButton.setEnabled(false);
        status.setText("Dev Orus is thinking…");
        new Thread(() -> {
            final String answer = nativeAsk(question);
            runOnUiThread(() -> {
                addMessage("Dev Orus 1.1", answer, false);
                sendButton.setEnabled(ready);
                status.setText("OFFLINE · Local model · No API");
            });
        }).start();
    }

    private void addMessage(String sender, String body, boolean user) {
        TextView bubble = new TextView(this);
        bubble.setText(sender + "\n" + body);
        bubble.setTextColor(0xFFEAF2FF);
        bubble.setTextSize(15);
        bubble.setLineSpacing(3f, 1.0f);
        bubble.setPadding(16, 12, 16, 12);
        bubble.setBackgroundColor(user ? 0xFF245B65 : 0xFF172238);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                user ? (int)(getResources().getDisplayMetrics().widthPixels * 0.82f) : (int)(getResources().getDisplayMetrics().widthPixels * 0.90f),
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = user ? Gravity.END : Gravity.START;
        params.setMargins(0, 6, 0, 6);
        messages.addView(bubble, params);
        chatScroll.post(() -> chatScroll.fullScroll(ScrollView.FOCUS_DOWN));
    }
}
