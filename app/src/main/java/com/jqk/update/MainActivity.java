package com.jqk.update;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.jqk.updatelibrary.UpdateService;

public class MainActivity extends AppCompatActivity {

    private Button start;

    private boolean isStart = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        start = (Button) findViewById(R.id.start);

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (isStart) {
                    Intent intent = new Intent(MainActivity.this, UpdateService.class);
                    intent.putExtra("url", "http://msoftdl.360.cn/mobile/shouji360/360safesis/360MobileSafe_7.7.4.1039.apk");
                    startService(intent);

                    isStart = false;
                }
            }
        });
    }
}
