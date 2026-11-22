package com.devai;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public final class MainActivity extends Activity {
    static {
        System.loadLibrary("devai");
    }

    private native String nativeTokenizerInfo();
    private native String nativeEncodeAsHex(String text);
    private native String nativeDecodeHex(String hex);

    private EditText input;
    private TextView output;
    private String lastHex = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        input = findViewById(R.id.inputText);
        output = findViewById(R.id.outputText);
        Button encode = findViewById(R.id.encodeButton);
        Button decode = findViewById(R.id.decodeButton);

        encode.setOnClickListener(view -> {
            lastHex = nativeEncodeAsHex(input.getText().toString());
            output.setText(nativeTokenizerInfo()
                    + "\n\nEncoded bytes (hex):\n" + lastHex
                    + "\n\nPrototype only: this is not learned BPE or an AI model.");
        });

        decode.setOnClickListener(view -> {
            if (lastHex.isEmpty()) {
                output.setText("Enter text and encode it first.");
                return;
            }
            output.setText("Decoded text:\n" + nativeDecodeHex(lastHex));
        });
    }
}
