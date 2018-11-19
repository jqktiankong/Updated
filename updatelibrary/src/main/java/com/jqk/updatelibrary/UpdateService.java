package com.jqk.updatelibrary;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Administrator on 2017/10/27 0027.
 */

public class UpdateService extends Service {
    private int jindu;
    private File dlFile;
    private DownloadTask downloadTask;
    private long alreadySize;
    private Timer timer;
    private File apkFile;
    private String apkName = "123.apk";

    private String url;

    private NotificationManager manager;
    public static final String id = "com.jqk.updatelibrary.id";
    public static final String name = "com.jqk.updatelibrary.name";


    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1000:
//                    L.d("更新数据");
                    RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.noti_update);
                    remoteViews.setProgressBar(R.id.progressBar, 100, jindu, false);
                    remoteViews.setTextViewText(R.id.textView, jindu + "%");

                    startForeground(remoteViews);
                    break;
                case 1001:
                    T.showShort(UpdateService.this, getString(R.string.download_fail));
                    if (timer != null) {
                        timer.cancel();
                    }
                    manager.cancel(1);
                    stopSelf();
                    break;
                case 1002:
                    if (timer != null) {
                        timer.cancel();
                    }

                    AutoInstall.setFile(apkFile);
                    AutoInstall.install(UpdateService.this);
                    manager.cancel(1);
                    stopSelf();
                    break;
            }
        }
    };

    public class DLBinder extends Binder {
        public UpdateService getService() {
            return UpdateService.this;
        }
    }

    @Override
    public void onCreate() {

        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        getTemporaryStorageDir(getApplicationContext(), "apk");
        url = intent.getStringExtra("url");

        L.d("url = " + url);

        if (manager == null) {
            manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH);
            channel.setSound(null, Notification.AUDIO_ATTRIBUTES_DEFAULT);
            channel.enableVibration(false);
            manager.createNotificationChannel(channel);
        }

        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.noti_update);
        remoteViews.setProgressBar(R.id.progressBar, 100, jindu, false);
        remoteViews.setTextViewText(R.id.textView, jindu + "%");

        startForeground(remoteViews);

        startDL(1, url);

        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        L.d("销毁服务");

        if (timer != null) {
            timer.cancel();
        }
        stopForeground(true);
        handler.removeMessages(1000);
    }

    // 开始下载
    public void startDL(int tID, String url) {
        if (downloadTask == null) {
            downloadTask = new DownloadTask();
            downloadTask.execute(url);
            if (timer != null) {
                timer.cancel();
            }
            timer = new Timer();
            timer.schedule(new TimerTask() {
                public void run() {
                    Message msg = new Message();
                    msg.what = 1000;
                    handler.sendMessage(msg);
                }
            }, 0, 500);
        }
    }

    public File getTemporaryStorageDir(Context context, String albumName) {
        // 获取临时文件夹
        dlFile = new File(context.getExternalFilesDir(
                Environment.DIRECTORY_DOWNLOADS), albumName);
        if (dlFile.mkdirs() || dlFile.isDirectory()) {
            Log.d("dl", "文件夹已存在");
        } else {
            Log.d("dl", "文件夹创建失败");
        }
        return dlFile;
    }

    // 获取下载进度
    public int getJindu(int tID) {
        Log.d("dl", "jindu = " + jindu);
        return jindu;
    }

    public class DownloadTask extends AsyncTask<String, Integer, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... strings) {
            Looper.prepare();
            try {
                //创建URL对象
                URL url = new URL(strings[0]);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                //已经下载的字节数
                alreadySize = 0;
                //将文件写到指定目录中
                File file = new File(dlFile.getAbsolutePath(), apkName);
                apkFile = file;
                file.delete();
                file.exists();

                conn.addRequestProperty("range", "bytes=" + alreadySize + "-");
                conn.connect();
                // 获得返回结果
                L.d("开始下载");
                int code = conn.getResponseCode();
                L.d("code = " + code);
                // 响应成功返回206
                if (code == 206) {
                    // 获取未下载的文件的大小
                    long unfinishedSize = conn.getContentLength();
                    // 文件的大小
                    long size = alreadySize + unfinishedSize;
                    Log.d("dl", "size = " + size);
                    // 获取输入流
                    InputStream in = conn.getInputStream();
                    // 获取输出对象,在原文件后追加
                    OutputStream out = new BufferedOutputStream(new FileOutputStream(file, true));

                    // 开始下载
                    byte[] buff = new byte[2048];
                    int len;
                    StringBuilder sb = new StringBuilder();
                    while ((len = in.read(buff)) != -1) {
                        out.write(buff, 0, len);
                        // 累加已下载的大小
                        alreadySize += len;
                        // 更新进度
                        publishProgress((int) (alreadySize * 1.0 / size * 100));
                    }
                    out.close();
                    // 下载完成
                    L.d("下载完成");
                    handler.sendEmptyMessageDelayed(1002, 1000);
                } else {
                    L.d("下载失败1");
                    handler.sendEmptyMessage(1001);
                }
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                L.d("下载失败");
                handler.sendEmptyMessage(1001);
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            jindu = (values[0]);
        }
    }

    public static class AutoInstall {
        private static File mFile;
        private static Context mContext;

        /**
         * 外部传进来的file以便定位需要安装的APK
         *
         * @param file
         */
        public static void setFile(File file) {
            mFile = file;
        }

        /**
         * 安装
         *
         * @param context 接收外部传进来的context
         */
        public static void install(Context context) {
            mContext = context;
//            // 核心是下面几句代码
//            Intent intent = new Intent(Intent.ACTION_VIEW);
//            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            intent.setDataAndType(Uri.fromFile(new File(mUrl)),
//                    "application/vnd.android.package-archive");
//            mContext.startActivity(intent);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            // 由于没有在Activity环境下启动Activity,设置下面的标签
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (Build.VERSION.SDK_INT >= 24) { //判读版本是否在7.0以上
                //参数1 上下文, 参数2 Provider主机地址 和配置文件中保持一致   参数3  共享的文件
                Uri apkUri = FileProvider.getUriForFile(mContext, "com.jqk.updatelibrary.fileprovider", mFile);
                //添加这一句表示对目标应用临时授权该Uri所代表的文件
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            } else {
                intent.setDataAndType(Uri.fromFile(mFile), "application/vnd.android.package-archive");
            }
            mContext.startActivity(intent);
        }
    }

    @TargetApi(26)
    public void startForeground(RemoteViews remoteViews) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                    .setContent(remoteViews)
                    .setOngoing(true)//设置他为一个正在进行的通知。他们通常是用来表示一个后台任务,用户积极参与(如播放音乐) 或以某种方式正在等待,因此占用设备(如一个文件下载,同步操作,主动网络连接)
                    .setSmallIcon(R.drawable.ic_launcher)//设置通知小ICON
                    .setChannelId(id);
            manager.notify(1, builder.build());
        } else {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                    .setContent(remoteViews)
                    .setOngoing(true)//设置他为一个正在进行的通知。他们通常是用来表示一个后台任务,用户积极参与(如播放音乐) 或以某种方式正在等待,因此占用设备(如一个文件下载,同步操作,主动网络连接)
                    .setSmallIcon(R.drawable.ic_launcher)//设置通知小ICON
                    .setSound(null)
                    .setVibrate(new long[]{0l});
            manager.notify(1, builder.build());
        }
    }
}
