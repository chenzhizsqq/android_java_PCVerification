package jp.co.unisys.authlocker.bluetooth;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import jp.co.unisys.authlocker.activity.HomeActivity;
import jp.co.unisys.authlocker.application.PCVApplication;
import jp.co.unisys.authlocker.service.BluetoothService;

public class BluetoothStateChangedBroadcast extends BroadcastReceiver {

    public BluetoothStateChangedBroadcast() {
        super();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                BluetoothAdapter.ERROR);
        switch (state) {
            case BluetoothAdapter.STATE_OFF:
                Log.d("BluetoothStateChanged", "STATE_OFF スマホのBluetoothがオフになっている");
                Intent bluetoothServiceIntent = new Intent(context, BluetoothService.class);
                context.stopService(bluetoothServiceIntent);
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                Log.d("BluetoothStateChanged", "STATE_TURNING_OFF スマホのBluetoothをオフにする準備ができました");
                break;
            case BluetoothAdapter.STATE_ON:
                Log.d("BluetoothStateChanged", "STATE_ON スマホのBluetoothがオンになっている");
                try {
                    bluetoothServiceIntent = new Intent(context, BluetoothService.class);
                    context.stopService(bluetoothServiceIntent);
                    context.startService(bluetoothServiceIntent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case BluetoothAdapter.STATE_TURNING_ON:
                Log.d("BluetoothStateChanged", "STATE_TURNING_ON スマホのBluetoothをオンにする準備ができました");
                break;
        }
        //}
        final PendingResult pendingResult = goAsync();
        Task asyncTask = new Task(pendingResult, intent);
        asyncTask.execute();
    }

    private static class Task extends AsyncTask<String, Integer, String> {

        private final PendingResult pendingResult;
        private final Intent intent;

        private Task(PendingResult pendingResult, Intent intent) {
            this.pendingResult = pendingResult;
            this.intent = intent;
        }

        @Override
        protected String doInBackground(String... strings) {
            StringBuilder sb = new StringBuilder();
            sb.append("Action: " + intent.getAction() + "\n");
            sb.append("URI: " + intent.toUri(Intent.URI_INTENT_SCHEME).toString() + "\n");
            String log = sb.toString();
            return log;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            // Must call finish() so the BroadcastReceiver can be recycled.
            pendingResult.finish();
        }
    }


}
