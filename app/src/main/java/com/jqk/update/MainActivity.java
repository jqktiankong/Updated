package com.jqk.update;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.jqk.updatelibrary.UpdateService;

public class MainActivity extends AppCompatActivity {
    private static final int INSTALL_PERMISS_CODE = 1;
    private static final int INSTALL_PACKAGES_REQUESTCODE = 2;
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

    // 判断是否安装未知应用权限
    // 两种处理方式：
    // 一种主动打开设置页面，一种系统自动提醒是否允许安装未知应用
    private void checkInstallPackages() {
        if (Build.VERSION.SDK_INT >= 26) {
            if (getPackageManager().canRequestPackageInstalls()) {
                isStart = true;
            } else {
                // 主动跳设置页面
                Uri packageURI = Uri.parse("package:" + getPackageName());
                Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, packageURI);
                startActivityForResult(intent, INSTALL_PERMISS_CODE);

                // 默认申请失败，安装时系统自动调出设置界面
//                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.REQUEST_INSTALL_PACKAGES}, INSTALL_PACKAGES_REQUESTCODE);

                isStart = false;
            }
        } else {
            isStart = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case INSTALL_PACKAGES_REQUESTCODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("jqk", "获取权限成功");
                    isStart = true;
                } else {
                    Log.d("jqk", "获取权限失败");
                    isStart = false;
                }
                break;
            default:
                break;

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == INSTALL_PERMISS_CODE) {
            isStart = true;
        } else {
            isStart = false;
        }
    }
}
