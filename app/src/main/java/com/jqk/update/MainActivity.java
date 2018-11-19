package com.jqk.update;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.jqk.updatelibrary.UpdateService;

public class MainActivity extends AppCompatActivity {
private static final int INSTALL_PACKAGES_REQUESTCODE = 1;
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

        checkInstallPackages();
    }

    private boolean checkInstallPackages() {
        if (Build.VERSION.SDK_INT >= 26) {
            boolean b = getPackageManager().canRequestPackageInstalls();
            if (b) {
               return true;
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.REQUEST_INSTALL_PACKAGES}, INSTALL_PACKAGES_REQUESTCODE);
            }
        } else {
            return true;
        }

        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == INSTALL_PACKAGES_REQUESTCODE) {

        }
    }
}
