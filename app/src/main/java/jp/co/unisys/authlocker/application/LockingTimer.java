package jp.co.unisys.authlocker.application;

import android.util.Log;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import static jp.co.unisys.authlocker.config.Config.TimeOutFlag;

public class LockingTimer {
     public static Timer timer = null;

    public void setTimer(){
        if (timer == null) {
            //==== タイマー作成 & スタート ====//
            timer = new Timer();
            Log.d("timer", String.valueOf(PCVApplication.getInstance().getUnlockTime())+"ms");
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    long timeNow = new Date().getTime();
                    PCVApplication.getInstance().getHomeActivity().runOnUiThread(()-> {
                        //スマホアプリの状態変更（スマホロック画面に遷移）
                        if(timeNow-PCVApplication.getInstance().getUnlockTime()>180*1000) {
                            Log.d("timer", String.valueOf(timeNow)+"ms");
                            PCVApplication.getInstance().getHomeActivity().refreshStatus(1);
                            TimeOutFlag=true;
                            //タイマー停止
                        	stopTimer();
                        }
                    });
                }
            }, 0,5000);
        }
    }
    //タイマー停止
    public void stopTimer(){
        if(timer!=null){
            //Log.d("timer","Stop");
            timer.cancel();
            timer.purge();
            timer=null;
        }
    }
}
