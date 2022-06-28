package jp.co.unisys.authlocker.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.databinding.BaseObservable;

/**
 * BluetoothデバイスModel
 * <p>
 * Created by dean on 2017/10/17.
 */
public class BluetoothDeviceModel extends BaseObservable implements Parcelable {

    // デバイス名（Bluetooth）
    private String name;
    // 結合状態
    private int bondState;
    // 物理アドレス
    private String address;

    // Bluetoothデバイスオブジェクト
    private BluetoothDevice bluetoothDevice;

    public BluetoothDeviceModel() {
    }

    public BluetoothDeviceModel(BluetoothDevice bluetoothDevice) {
        if (bluetoothDevice != null) {
            this.name = bluetoothDevice.getName();
            if (TextUtils.isEmpty(this.name))
                this.name = "名前のないデバイス";

            this.bondState = bluetoothDevice.getBondState();
            this.address = bluetoothDevice.getAddress();
        }

        this.bluetoothDevice = bluetoothDevice;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getBondState() {
        return bondState;
    }

    public void setBondState(int bondState) {
        this.bondState = bondState;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    public void setBluetoothDevice(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
    }

    protected BluetoothDeviceModel(Parcel in) {
        name = in.readString();
        bondState = in.readInt();
        address = in.readString();
        bluetoothDevice = in.readParcelable(BluetoothDevice.class.getClassLoader());
    }

    public static final Creator<BluetoothDeviceModel> CREATOR = new Creator<BluetoothDeviceModel>() {
        @Override
        public BluetoothDeviceModel createFromParcel(Parcel in) {
            return new BluetoothDeviceModel(in);
        }

        @Override
        public BluetoothDeviceModel[] newArray(int size) {
            return new BluetoothDeviceModel[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeInt(bondState);
        dest.writeString(address);
        dest.writeParcelable(bluetoothDevice, flags);
    }
}
