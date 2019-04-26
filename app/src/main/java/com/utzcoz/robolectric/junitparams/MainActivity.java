package com.utzcoz.robolectric.junitparams;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final EditText etContentInput = findViewById(R.id.et_content_input);
        final TextView tvContentPresenter = findViewById(R.id.tv_content_presenter);
        findViewById(R.id.btn_present_input).setOnClickListener(
                v -> tvContentPresenter.setText(etContentInput.getText().toString())
        );
    }
}
