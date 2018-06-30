import net.sf.json.JSONObject;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.*;

public class SocketServer {
    private Charset cs = Charset.forName("UTF-8");
    private ByteBuffer sBuffer = ByteBuffer.allocate(1024);
    private ByteBuffer rBuffer = ByteBuffer.allocate(1024);
    public Map<String, Integer> lockMap = new HashMap<>();
    public List<SocketChannel> socketChannels = new ArrayList<>();
    private SocketClient masterSocketClient = null;

    public void startClientService(int port){
        new Thread(() -> {
            System.out.println("startClientService at port: " + port);
            startSocketServer(port, true);
        }).start();
    }

    public void startFollowerService(int port){
        new Thread(() -> {
            System.out.println("startFollowerService at port: " + port);
            startSocketServer(port, false);
        }).start();
    }

    public boolean connectToMaster(String address, Integer port) {
        new Thread(() -> {
            try {
                masterSocketClient = new SocketClient(address, port);
                while (true){
                    String request = masterSocketClient.getResponse();
                    System.out.println("master request: " + request);
                    String[] args = request.split(":");
                    handleRequest(Integer.parseInt(args[0]), args[1]);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
        return true;
    }

    public void startSocketServer(int port, boolean forClient) {
        try {
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            ServerSocket serverSocket = serverSocketChannel.socket();
            serverSocket.bind(new InetSocketAddress(port));
            Selector selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            while (true) {
                selector.select();
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                for (SelectionKey key : selectionKeys) {
                    handle(key, forClient, selector);
                }
                selectionKeys.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handle(SelectionKey selectionKey, boolean forClient, Selector selector) throws IOException {
        ServerSocketChannel serverSocketChannel = null;
        SocketChannel socketChannel = null;
        String request = "";
        int count = 0;
        try {
            if (selectionKey.isAcceptable()) {
                serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
                socketChannel = serverSocketChannel.accept();
                socketChannel.configureBlocking(false);
                socketChannel.register(selector, SelectionKey.OP_READ);
                if (forClient){
                    System.out.println("client " + socketChannel.hashCode() + " connect");
                }else {
                    socketChannels.add(socketChannel);
                    System.out.println("followServer " + socketChannel.hashCode() + " connect");
                }
                response(socketChannel, String.valueOf(socketChannel.hashCode()));
            } else if (selectionKey.isReadable()) {
                socketChannel = (SocketChannel) selectionKey.channel();
                rBuffer.clear();
                count = socketChannel.read(rBuffer);
                if (count > 0) {
                    rBuffer.flip();
                    request = String.valueOf(cs.decode(rBuffer).array());
                }
                if (forClient){
                    System.out.println("client " + socketChannel.hashCode() + " request: " + request);
                    response(socketChannel, handleRequest(socketChannel.hashCode(), request));
                    if (masterSocketClient != null){
                        masterSocketClient.request(socketChannel.hashCode() + ":" + request);
                    }
                }else {
                    System.out.println("followServer " + socketChannel.hashCode() + " request: " + request);
                    String[] args = request.split(":");
                    handleRequest(Integer.parseInt(args[0]), args[1]);
                }
                for (SocketChannel channel : socketChannels) {
                    if (!channel.equals(socketChannel)){
                        response(channel, socketChannel.hashCode() + ":" + request);
                    }
                }
            }
        } catch (IOException e) {
            if (forClient){
                System.out.println("client " + socketChannel.hashCode() + " close connect");
                for (Map.Entry<String, Integer> entry : lockMap.entrySet()) {
                    if (entry.getValue().equals(socketChannel.hashCode())){
                        lockMap.remove(entry.getKey());
                    }
                }
            }else {
                System.out.println("followServer " + socketChannel.hashCode() + " close connect");
                for (SocketChannel channel : socketChannels) {
                    if (channel.equals(socketChannel)){
                        socketChannels.remove(channel);
                        break;
                    }
                }
            }
            socketChannel.close();
        }
    }

    private String handleRequest(Integer clientId, String request){
        String[] args = request.split(" ");
        String op = args[0];
        if (op.equalsIgnoreCase("tryLock")){
            String lock = args[1];
            if (lockMap.containsKey(lock)){
                return String.valueOf(false);
            }else {
                lockMap.put(lock, clientId);
                return String.valueOf(true);
            }
        }else if (op.equalsIgnoreCase("tryUnLock")){
            String lock = args[1];
            if (lockMap.containsKey(lock) && lockMap.get(lock).equals(clientId)){
                lockMap.remove(lock);
                return String.valueOf(true);
            }else {
                return String.valueOf(false);
            }
        }else if (op.equalsIgnoreCase("ownLock")) {
            String lock = args[1];
            return String.valueOf(lockMap.containsKey(lock));
        }else if (op.equalsIgnoreCase("getLockMap")){
            return JSONObject.fromObject(lockMap).toString();
        }else {
            return "unsupported operation";
        }
    }

    private void response(SocketChannel socketChannel, String responseMsg) throws IOException {
        sBuffer = ByteBuffer.allocate(responseMsg.getBytes("UTF-8").length);
        sBuffer.put(responseMsg.getBytes("UTF-8"));
        sBuffer.flip();
        socketChannel.write(sBuffer);
    }
}

