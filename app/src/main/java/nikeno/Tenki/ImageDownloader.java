package nikeno.Tenki;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.util.ArrayList;

public class ImageDownloader {


    static final int MSG_CHECK_TASK = 1;
    static final int MSG_TASK_COMPLETE = 2;

    static ImageDownloader sInstance;

    public static synchronized  ImageDownloader getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ImageDownloader(context);
        }
        return sInstance;
    }


    final Downloader mDownloader;

    Thread mCurrentTask;
    ArrayList<Task> mTasks = new ArrayList<>();

    Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case MSG_CHECK_TASK:
                    checkTask();
                    break;
                case MSG_TASK_COMPLETE:
                    mCurrentTask = null;
                    onPostExecute((Task) msg.obj);
                    checkTask();
                    break;
            }
        }
    };

    public ImageDownloader(Context context) {
        mDownloader = Downloader.getInstance(context);
    }

    public void setImage(String url, Downloader.ImageHandler view) {
        if (setImageNow(url, view)) {
            return;
        }

        Task task = new Task(url, view);
        mTasks.add(task);

        mHandler.sendEmptyMessage(MSG_CHECK_TASK);
    }

    boolean setImageNow(String url, Downloader.ImageHandler view) {
        Bitmap bmp = mDownloader.getImageFromMemCache(url);
        if (bmp == null) {
            return false;
        } else {
            view.setBitmap(bmp);
            return true;
        }
    }

    void checkTask() {
        if (mCurrentTask == null) {
            while (mTasks.size() > 0) {
                final Task task = mTasks.remove(0);
                if (setImageNow(task.url, task.handler)) {
                    continue;
                }

                mCurrentTask = new Thread() {
                    @Override
                    public void run() {
                        super.run();
                        execute(task);
                    }
                };
                mCurrentTask.start();
                break;
            }
        }
    }

    void execute(Task task) {
        try {
            task.bmp = mDownloader.downloadImage(task.url, TenkiApp.IMAGE_SIZE_MAX, 0);
        } catch (Exception e) {
            e.printStackTrace();
            task.error = e;
        }

        Message msg = mHandler.obtainMessage();
        msg.what = MSG_TASK_COMPLETE;
        msg.obj = task;
        mHandler.sendMessage(msg);
    }

    void onPostExecute(Task task) {
        if (task.bmp != null) {
            task.handler.setBitmap(task.bmp);
        }
    }

    static class Task {
        public String                  url;
        public Downloader.ImageHandler handler;
        public Bitmap                  bmp;
        public Throwable               error;
        public Task(String url, Downloader.ImageHandler handler) {
            this.url = url;
            this.handler = handler;
        }
    }
}
