package com.devai;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Arrays;

public final class MainActivity extends Activity {
    static { System.loadLibrary("devai"); }

    private native String nativeTokenizerInfo();
    private native String nativeTrainTokenizer(String corpus, int mergeCount);
    private native int[] nativeEncode(String text);
    private native String nativeDecode(int[] ids);
    private native String nativeRunTransformer(int[] tokenIds);

    private EditText trainingText;
    private EditText inputText;
    private TextView outputText;
    private int[] lastTokenIds = new int[0];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        trainingText = findViewById(R.id.trainingText);
        inputText = findViewById(R.id.inputText);
        outputText = findViewById(R.id.outputText);
        Button train = findViewById(R.id.trainButton);
        Button encode = findViewById(R.id.encodeButton);
        Button decode = findViewById(R.id.decodeButton);
        Button forward = findViewById(R.id.forwardButton);
        outputText.setText(nativeTokenizerInfo());

        train.setOnClickListener(v -> outputText.setText(
                nativeTrainTokenizer(trainingText.getText().toString(), 128)
                        + "\n\n" + nativeTokenizerInfo()));
        encode.setOnClickListener(v -> {
            lastTokenIds = nativeEncode(inputText.getText().toString());
            outputText.setText("Token IDs:\n" + Arrays.toString(lastTokenIds)
                    + "\n\nToken count: " + lastTokenIds.length
                    + "\n\n" + nativeTokenizerInfo());
        });
        decode.setOnClickListener(v -> {
            if (lastTokenIds.length == 0) {
                outputText.setText("Train the tokenizer and encode text first.");
            } else {
                outputText.setText("Decoded text:\n" + nativeDecode(lastTokenIds));
            }
        });
        forward.setOnClickListener(v -> {
            if (lastTokenIds.length == 0) {
                outputText.setText("Encode some text first. The Transformer accepts up to 64 token IDs.");
            } else {
                outputText.setText(nativeRunTransformer(lastTokenIds));
            }
        });
    }
}
