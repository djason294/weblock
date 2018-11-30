package org.elastos.carrier.demo;

import android.app.AlertDialog;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        TextView txtMsg = (TextView) findViewById(R.id.textView);
        txtMsg.setMovementMethod(new ScrollingMovementMethod());
        Logger.init(txtMsg);


        Button btnPost = (Button) findViewById(R.id.button_post);
        btnPost.setOnClickListener((view) -> {

            EditText content = new EditText(this);
            content.setHint("what you want to post");
            content.setText("hello world!");

            LinearLayout root = new LinearLayout(this);
            root.setOrientation(LinearLayout.VERTICAL);
            root.addView(content);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Post something");
            builder.setView(root);
            builder.setNegativeButton("Cancel", (dialog, which) -> {
                dialog.dismiss();
            });
            builder.setPositiveButton("OK", (dialog, which) -> {
                String p = content.getText().toString();
                CarrierHelper.post(CarrierHelper.getAddress(), p);
            });
            builder.create().show();
        });

    }
}
