package io.github.njohnsoncpe.opencvdemo;

public interface MessageReceivedListener {

    public void OnMessageReceived(String msg);

    public void OnFileIncoming(int length);

    public void OnFileDataReceived(byte[] data,int read, int length, int downloaded);

    public void OnFileComplete();

    public void OnConnectionerror();

    public void OnConnectSuccess();
}
