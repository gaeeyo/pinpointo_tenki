package nikeno.Tenki;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.ImageView;

import java.util.ArrayList;

public class ImageDownloader {


    static final int MSG_CHECK_TASK = 1;
    static final int MSG_TASK_COMPLETE = 2;

    static ImageDownloader sInstance;

    public static synchronized  ImageDownloader getInstance() {
        if (sInstance == null) {
            sInstance = new ImageDownloader();
        }
        return sInstance;
    }


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

    Thread mCurrentTask;
    ArrayList<Task> mTasks = new ArrayList<Task>();

    public void setImage(String url, ImageView view) {
        if (setImageNow(url, view)) {
            return;
        }

        Task task = new Task(url, view);
        mTasks.add(task);

        mHandler.sendEmptyMessage(MSG_CHECK_TASK);
    }

    boolean setImageNow(String url, ImageView view) {
        Bitmap bmp = Downloader.getImageFromMemCache(url);
        if (bmp == null) {
            return false;
        } else {
            view.setImageBitmap(bmp);
            return true;
        }
    }

    void checkTask() {
        if (mCurrentTask == null) {
            while (mTasks.size() > 0) {
                final Task task = mTasks.remove(0);
                if (setImageNow(task.url, task.view)) {
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
            task.bmp = Downloader.downloadImage(task.url, Const.IMAGE_SIZE_MAX, 0);
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
            task.view.setImageBitmap(task.bmp);
        }
    }

    static class Task {
        public String url;
        public ImageView view;
        public Bitmap bmp;
        public Throwable error;
        public Task(String url, ImageView view) {
            this.url = url;
            this.view = view;
        }
    }
}
