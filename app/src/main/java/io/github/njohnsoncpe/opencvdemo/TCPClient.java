package io.github.njohnsoncpe.opencvdemo;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.spec.ECField;

public class TCPClient {
    private static final String SERVER_ADDR = "131.128.51.147";
    private static final int SERVER_PORT = 10000;
    private String serverIP;
    private int serverPort;
    private long startTime = 0l;
    private Socket socket;
    private static String TAG = "TCPClient";
    private SendRunnable sendRunnable;
    private Thread sendThread;
    byte[] dataToSend;
    private ReceieveRunnable receieveRunnable;
    private Thread receieveThread;
    boolean receiveThreadRunning = false;

    public MessageReceivedListener msgListener;

    public TCPClient(MessageReceivedListener l){
        msgListener = l;
    }

    public void Connect(String ip, int port){
        serverIP = ip;
        serverPort = port;
        new Thread(new ConnectRunnable()).start();
    }

    public boolean isConnected(){
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public void Disconnect(){
        stopThreads();
        try {
            socket.close();
        }catch (Exception e){}
    }

    private void startSending(){
        sendRunnable = new SendRunnable(socket);
        sendThread = new Thread(sendRunnable);
        sendThread.start();
    }

    private void startReceiving(){
        receieveRunnable = new ReceieveRunnable(socket);
        receieveThread = new Thread(receieveRunnable);
        receieveThread.start();

    }

    private void stopThreads(){
        if(receieveThread != null)
            receieveThread.interrupt();
        if(sendThread != null)
            sendThread.interrupt();
    }

    public void WriteData(byte[] data){
        if(isConnected()){
            startSending();
            sendRunnable.Send(data);
        }
    }

    public void WriteCMD(String cmd){
        if(isConnected()){
            startSending();
            sendRunnable.SendCMD(cmd.getBytes());
        }
    }

    public class ReceieveRunnable implements Runnable{
        private Socket sock;
        private InputStream input;

        public ReceieveRunnable(Socket server){
            sock = server;
            try {
                input = sock.getInputStream();
            }catch (Exception e){}
        }

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted() && isConnected()){
                if(!receiveThreadRunning)
                    receiveThreadRunning = true;
                try {
                    byte[] data = new byte[4];
                    input.read(data, 0, data.length);
                    int length = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getInt();
                    input.read(data,0,data.length);
                    int type = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getInt();

                    int read = 0;
                    int downloaded = 0;

                    if(type == TCPCommands.TYPE_CMD){
                        data = new byte[length];
                        StringBuilder stringBuilder = new StringBuilder();
                        InputStream inputStream = new BufferedInputStream(input);
                        while ((read = inputStream.read(data)) != -1){
                            downloaded += read;
                            stringBuilder.append(new String(data, 0, read, "UTF-8"));
                            if(downloaded == length)
                                break;
                        }
                        if(msgListener != null)
                            msgListener.OnMessageReceived(stringBuilder.toString());
                    }else if(type == TCPCommands.TYPE_FILE_CONTENT){
                        byte[] inputData = new byte[2048];
                        InputStream inputStream = new BufferedInputStream(input);
                        while ((read = inputStream.read(inputData)) != -1){
                            downloaded += read;
                            if(downloaded == length)
                                break;
                        }

                    }
                }catch (Exception e){Disconnect();}
            }
            receiveThreadRunning = false;
        }
    }
    public class SendRunnable implements Runnable {

        byte[] data;
        private OutputStream out;
        private boolean hasMessage = false;
        int dataType = 1;

        public SendRunnable(Socket server){
            try {
                this.out = server.getOutputStream();
            }catch (IOException e){

            }
        }

        public void Send(byte[] bytes){
            this.data = bytes;
            dataType = TCPCommands.TYPE_FILE_CONTENT;
            this.hasMessage = true;
        }

        public void SendCMD(byte[] bytes){
            this.data = bytes;
            dataType = TCPCommands.TYPE_CMD;
            this.hasMessage = true;
        }
        @Override
        public void run() {
            Log.d(TAG, "Sending Started");
            while(!Thread.currentThread().isInterrupted() && isConnected()){
                if(this.hasMessage){
                    try {
                        this.out.write(ByteBuffer.allocate(4).putInt(data.length).array());
                        this.out.write(ByteBuffer.allocate(4).putInt(dataType).array());
                        this.out.write(data, 0, data.length);
                        this.out.flush();
                    }catch (IOException e){}
                    this.hasMessage = false;
                    this.data = null;
                    Log.d(TAG, "Command has been sent!");

                    if(!receiveThreadRunning)
                        startReceiving();
                }
            }
            Log.d(TAG, "Sending stopped");
        }
    }
    public class ConnectRunnable implements Runnable{

        @Override
        public void run() {
            try {
                Log.d(TAG, "C: Connecting...");

                InetAddress serverAddr = InetAddress.getByName(serverIP);
                socket = new Socket();
                socket.connect(new InetSocketAddress(serverAddr, serverPort),5000);

                if(msgListener != null)
                    msgListener.OnConnectSuccess();

            }catch (Exception e){
                if(msgListener != null)
                    msgListener.OnConnectionerror();
            }
        }
    }


}
