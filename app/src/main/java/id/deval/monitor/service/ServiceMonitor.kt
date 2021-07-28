package id.deval.monitor.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log

class ServiceMonitor : Service() {

    companion object {
        private val TAG = ServiceMonitor::class.java.simpleName
    }
    private var mBinder = MyBinder()
    private val startTime = System.currentTimeMillis()

    internal inner class MyBinder : Binder(){
        val getService: ServiceMonitor = this@ServiceMonitor
    }

    private var mTimer: CountDownTimer = object : CountDownTimer(100000, 1000) {
        override fun onTick(l: Long) {
            val elapsedTime = System.currentTimeMillis() - startTime
            Log.d(TAG, "onTick: $elapsedTime")
        }
        override fun onFinish() {
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: ")
    }
    override fun onBind(intent: Intent): IBinder? {
        Log.d(TAG, "onBind: ")
        mTimer.start()
        return mBinder
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        return START_STICKY
    }
}