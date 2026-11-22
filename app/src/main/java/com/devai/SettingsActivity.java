package com.devai;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/** Settings screen: Gemma model download / cancel / delete, about, diagnostics. */
public final class SettingsActivity extends Activity implements ModelManager.Listener {

    private TextView modelStatus;
    private ProgressBar downloadProgress;
    private Button downloadButton;
    private Button deleteButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        modelStatus = findViewById(R.id.modelStatus);
        downloadProgress = findViewById(R.id.downloadProgress);
        downloadButton = findViewById(R.id.downloadButton);
        deleteButton = findViewById(R.id.deleteButton);

        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        downloadButton.setOnClickListener(v -> onDownloadButtonClicked());
        deleteButton.setOnClickListener(v -> confirmDelete());

        ((TextView) findViewById(R.id.aboutText)).setText(
                "Chat is powered by the Google Gemma 4 E2B instruction-tuned model, "
                + "run locally with Google's LiteRT-LM runtime. Dev Orus 1.1 is the assistant "
                + "identity of this app; the model weights are not trained by DevAI.\n\n"
                + "Model: huggingface.co/litert-community/gemma-4-E2B-it-litert-lm");

        if (CrashDiagnostics.hasReport(this)) {
            findViewById(R.id.diagnosticsSection).setVisibility(View.VISIBLE);
            Button crash = findViewById(R.id.crashReportButton);
            crash.setVisibility(View.VISIBLE);
            crash.setOnClickListener(v -> CrashDiagnostics.show(this));
        }
        render();
    }

    @Override
    protected void onStart() {
        super.onStart();
        ModelManager.addListener(this);
        render();
    }

    @Override
    protected void onStop() {
        ModelManager.removeListener(this);
        super.onStop();
    }

    @Override
    public void onModelStateChanged() {
        render();
    }

    private void onDownloadButtonClicked() {
        ModelManager.State state = ModelManager.state(this);
        if (state == ModelManager.State.DOWNLOADING) {
            ModelManager.cancelDownload();
        } else if (state != ModelManager.State.INSTALLED) {
            confirmDownload();
        }
    }

    private void confirmDownload() {
        new AlertDialog.Builder(this)
                .setTitle("Download Gemma 4 E2B?")
                .setMessage("This is a one-time download of about 2.6 GB. Use Wi-Fi and make sure at "
                        + "least 3.2 GB of free storage is available. After download, model responses "
                        + "run offline.")
                .setNegativeButton("Not now", null)
                .setPositiveButton("Download", (dialog, which) -> {
                    String problem = ModelManager.startDownload(this);
                    if (problem != null) Toast.makeText(this, problem, Toast.LENGTH_LONG).show();
                    render();
                })
                .show();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete the Gemma model?")
                .setMessage("The model file will be removed from this device (about 2.6 GB). "
                        + "You will need to download it again to keep chatting.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (!ModelManager.deleteModel(this)) {
                        Toast.makeText(this, "Could not delete the model file.", Toast.LENGTH_LONG).show();
                    }
                    render();
                })
                .show();
    }

    private void render() {
        ModelManager.State state = ModelManager.state(this);
        switch (state) {
            case DOWNLOADING: {
                int percent = ModelManager.percent();
                String size = ModelManager.formatMegabytes(ModelManager.downloadedBytes());
                downloadProgress.setVisibility(View.VISIBLE);
                downloadProgress.setIndeterminate(percent < 0);
                if (percent >= 0) downloadProgress.setProgress(percent);
                modelStatus.setText(percent >= 0
                        ? "Downloading · " + percent + "% · " + size
                        : "Downloading · " + size);
                downloadButton.setText("Cancel download");
                downloadButton.setEnabled(true);
                deleteButton.setVisibility(View.GONE);
                break;
            }
            case INSTALLED:
                downloadProgress.setVisibility(View.GONE);
                modelStatus.setText("Installed · ready to use offline");
                downloadButton.setText("Model installed");
                downloadButton.setEnabled(false);
                deleteButton.setVisibility(View.VISIBLE);
                break;
            case FAILED:
                downloadProgress.setVisibility(View.GONE);
                modelStatus.setText("Download failed: " + ModelManager.error());
                downloadButton.setText("Retry download");
                downloadButton.setEnabled(true);
                deleteButton.setVisibility(View.GONE);
                break;
            case MISSING:
            default:
                downloadProgress.setVisibility(View.GONE);
                modelStatus.setText("Not installed");
                downloadButton.setText("Download Gemma model · about 2.6 GB");
                downloadButton.setEnabled(true);
                deleteButton.setVisibility(View.GONE);
                break;
        }
    }
}
