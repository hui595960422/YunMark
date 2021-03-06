package com.geekymax.server.thread;


import com.geekymax.server.ServerDocumentService;
import com.geekymax.operation.Operation;

import java.io.*;
import java.net.Socket;


/**
 * 用于接受客户端发送的operation的线程
 *
 * @author Max Huang
 */
public class ClientThread implements Runnable {
    private Socket socket;
    private int index;
    private ServerDocumentService serverDocumentService;
    private final Object broadcastLock;

    public ClientThread(Socket socket, int index, Object broadcastLock) {
        this.socket = socket;
        this.index = index;
        serverDocumentService = ServerDocumentService.getInstance();
        this.broadcastLock = broadcastLock;
    }

    /**
     * todo 进程结束,需要能够从线程list中删除,否则会因为一个连接的断开,而导致异常
     */
    @Override
    public void run() {
        try {
            System.out.println("client " + index + " is online!");
            // 建立好连接后，从socket中获取输入流，并建立缓冲区进行读取
            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();
            // 写入操作对象
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
            objectOutputStream.writeObject(serverDocumentService.getText().toString());
            while (true) {
                // 这里出于方便考虑,将objectInputStream初始化放于循环之内,否则会需要解决header问题
                ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
                Operation operation = (Operation) objectInputStream.readObject();
                System.out.println("receive: " + operation);
                if (operation == null) {
                    break;
                }
                operation = serverDocumentService.receiveOperation(operation);
                // 消息广播
                BroadcastThread broadcastThread = BroadcastThread.getInstance();
                synchronized (broadcastLock) {
                    broadcastThread.addOperation(operation);
                    broadcastThread.setExcludeIndex(index);
                    broadcastLock.notify();
                }
            }
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public int getIndex() {
        return index;
    }

    public Socket getSocket() {
        return socket;
    }

}
