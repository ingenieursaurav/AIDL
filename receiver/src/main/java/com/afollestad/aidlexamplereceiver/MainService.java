package com.afollestad.aidlexamplereceiver;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.afollestad.aidlexample.IMainService;
import com.afollestad.aidlexample.MainObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Aidan Follestad (afollestad)
 */
public class MainService extends Service {

    private String keycode = "su";
    final int PORT = 8080;
    final String COM = "input keyevent ";
    ServerSocket serverSocket;
    String send;

    private void log(String message) {
        Log.v("MainService", message);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        log("Received start command.");
        WorkerThread workerThread = new WorkerThread();
        workerThread.start();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        log("Received binding.");
        return mBinder;
    }

    private final IMainService.Stub mBinder = new IMainService.Stub() {
        @Override
        public MainObject[] listFiles(String path) throws RemoteException {
            log("Received list command for: " + path);
            List<MainObject> toSend = new ArrayList<>();
            // Generates a list of 1000 objects that aren't sent back to the binding Activity
            for (int i = 0; i < 1000; i++)
                toSend.add(new MainObject("saurav" + (i + 1)));

            return toSend.toArray(new MainObject[toSend.size()]);
        }

        @Override
        public void exit() throws RemoteException {
            log("Received exit command.");
            stopSelf();
        }
    };

    private class WorkerThread extends Thread {

        @Override
        public void run() {
            Socket socket = null;

            try {
                serverSocket = new ServerSocket(PORT);

                while (true) {
                    socket = serverSocket.accept();

                    TextClient client = new TextClient();
                    Communication communicationThread = new Communication(client, socket);
                    communicationThread.start();
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }

    }

    class TextClient {
        Socket socket;
        Communication chatThread;

    }


    private class Communication extends Thread {

        Socket socket;
        TextClient connectClient;
        String textToSend = "";

        Communication(TextClient client, Socket socket) {
            connectClient = client;
            this.socket = socket;
            client.socket = socket;
            client.chatThread = this;
        }

        @Override
        public void run() {
            DataInputStream dataInputStream = null;
            DataOutputStream dataOutputStream = null;

            try {
                dataInputStream = new DataInputStream(socket.getInputStream());
                dataOutputStream = new DataOutputStream(socket.getOutputStream());
                dataOutputStream.flush();
                while (true) {
                    if (dataInputStream.available() > 0) {
                        String msg = dataInputStream.readUTF();
                        keycode = msg;
                        // Process p = Runtime.getRuntime().exec("su");
                       // Process p = Runtime.getRuntime().exec(COM + "su");
                        Runtime.getRuntime().exec(COM + keycode);
                        moveTheSelector(keycode);
                    }

                    if (!textToSend.equals("")) {
                        dataOutputStream.writeUTF(textToSend);
                        dataOutputStream.flush();
                        textToSend = "";
                    }

                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (dataInputStream != null) {
                    try {
                        dataInputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (dataOutputStream != null) {
                    try {
                        dataOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }

        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void moveTheSelector(String textmessage) {
        Log.e("TCP_MESSAGE", "run: " + textmessage);
        send = textmessage;
    }
}
