package jp.co.unisys.authlocker.bluetooth;

public class MessageContentModel {

    // メッセージID
    int code;

    // データ
    String data;

    public MessageContentModel(int code, String data) {
        this.code = code;
        this.data = data;
    }

    public byte[] toBytes() {
        byte[] result = new byte[1];
        byte[] dataBytes = new byte[1];

        if (code == 4 || code == 5|| code == 6||code == 7||code == 8) {
            dataBytes = stringToByteArray(data.split(","));
        } else {
            dataBytes = data.getBytes();
        }
        result = new byte[dataBytes.length + 5];
        for (int i=0; i < dataBytes.length ; i++)  result[i+5] = dataBytes[i];
        result[0] = (byte)0xff;
        result[1] = (byte)code;
        result[2] = (byte)0x01;
        result[3] = (byte)(dataBytes.length & 0xff);
        result[4] = (byte)(dataBytes.length >> 8);

        return result;
    }

    private byte[] stringToByteArray(String[] data) {
        if(data == null || data.length == 0) {
            return new byte[1];
        }

        byte[] result = new byte[data.length];
        int i = 0;
        for(String item : data) {
            result[i] =  ((byte)Integer.parseInt(item, 10));
            i++;
        }
        return result;
    }
}
