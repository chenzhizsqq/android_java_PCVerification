package jp.co.unisys.authlocker.application;

import android.util.Log;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import static jp.co.unisys.authlocker.config.Config.AppForgroundFlag;

public class LifecycleHandler implements LifecycleObserver {
    // I use four separate variables here. You can, of course, just use two and
    // increment/decrement them instead of using four and incrementing them all.


    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    public void created() {
        //Log.d("SampleLifeCycle", "ON_CREATE");
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void started() {
        Log.d("SampleLifeCycle", "ON_START");
        AppForgroundFlag = true;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void resumed() {
        //Log.d("SampleLifeCycle", "ON_RESUME");
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void paused() {
        // Log.d("SampleLifeCycle", "ON_PAUSE");
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void stopped() {
         Log.d("SampleLifeCycle", "ON_STOP");
        AppForgroundFlag = false;
    }
}
