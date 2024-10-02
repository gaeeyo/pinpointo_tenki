package nikeno.Tenki

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.os.Message
import nikeno.Tenki.TenkiApp.Companion.from

class ImageDownloader(context: Context?) {
    val mDownloader: Downloader = from(context!!).downloader

    var mCurrentTask: Thread? = null
    var mTasks: ArrayList<Task> = ArrayList()

    var mHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            when (msg.what) {
                MSG_CHECK_TASK -> checkTask()
                MSG_TASK_COMPLETE -> {
                    mCurrentTask = null
                    onPostExecute(msg.obj as Task)
                    checkTask()
                }
            }
        }
    }

    fun setImage(url: String?, view: (Bitmap) -> Unit) {
        if (setImageNow(url, view)) {
            return
        }

        val task = Task(url, view)
        mTasks.add(task)

        mHandler.sendEmptyMessage(MSG_CHECK_TASK)
    }

    fun setImageNow(url: String?, view: (Bitmap) -> Unit): Boolean {
        val bmp = mDownloader.getImageFromMemCache(url!!)
        if (bmp == null) {
            return false
        } else {
            view(bmp)
            return true
        }
    }

    fun checkTask() {
        if (mCurrentTask == null) {
            while (mTasks.size > 0) {
                val task = mTasks.removeAt(0)
                if (setImageNow(task.url, task.handler)) {
                    continue
                }

                mCurrentTask = object : Thread() {
                    override fun run() {
                        super.run()
                        execute(task)
                    }
                }
                mCurrentTask!!.start()
                break
            }
        }
    }

    fun execute(task: Task) {
        try {
            task.bmp = mDownloader.downloadImage(task.url!!, TenkiApp.IMAGE_SIZE_MAX, 0)
        } catch (e: Exception) {
            e.printStackTrace()
            task.error = e
        }

        val msg = mHandler.obtainMessage()
        msg.what = MSG_TASK_COMPLETE
        msg.obj = task
        mHandler.sendMessage(msg)
    }

    fun onPostExecute(task: Task) {
        if (task.bmp != null) {
            task.handler(task.bmp!!)
        }
    }

    class Task(var url: String?, var handler: (Bitmap) -> Unit) {
        var bmp: Bitmap? = null
        var error: Throwable? = null
    }

    companion object {
        const val MSG_CHECK_TASK: Int = 1
        const val MSG_TASK_COMPLETE: Int = 2
    }
}
