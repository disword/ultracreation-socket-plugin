package com.ultracreation;

import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class UltracreationSocket extends CordovaPlugin {

    private static final String TAG = "UltracreationSocket";

    private Map<Integer, SocketData> sockets = new HashMap<Integer, SocketData>();
    private int nextSocket = 1;


    private static final String ERROR_BIND = "Bind error";
    private static final String ERROR_CONNECT = "Connect error";
    private static final String ERROR_CLOSE = "Socket close";
    private static final String ERROR_NOT_CREATE = "Socket not create";
    private static final String ERROR_NOT_SERVER = "Only tcpServer can accept";
    private static final String ERROR_NOT_CLIENT = "Only Client can accept";
    private static final String ERROR_ACCEPT = "Accept error";
    private static final String ERROR_UDP_WRITE = "UDP write error";

    @Override
    public boolean execute(String action, CordovaArgs args, final CallbackContext callbackContext) throws JSONException {
        if ("socket".equals(action)) {
            socket(args, callbackContext);
        } else if ("listen".equals(action)) {
            listen(args, callbackContext);
        } else if ("accept".equals(action)) {
            accept(args, callbackContext);
        } else if ("select".equals(action)) {
            select(args, callbackContext);
        } else if ("send".equals(action)) {
            send(args, callbackContext);
        } else if ("recv".equals(action)) {
            recv(args, callbackContext);
        } else if ("connect".equals(action)) {
            connect(args, callbackContext);
        } else if ("sendTo".equals(action)) {
            sendTo(args, callbackContext);
        } else if ("recvFrom".equals(action)) {
            recvFrom(args, callbackContext);
        }else if ("close".equals(action)) {
            close(args, callbackContext);
        }
// else if ("disconnect".equals(action)) {
//            disconnect(args, callbackContext);
//        } else if ("destroy".equals(action)) {
//            destroy(args, callbackContext);
//        }  else if ("getInfo".equals(action)) {
//            getInfo(args, callbackContext);
//        }

        else {
            return false;
        }
        return true;
    }

    public void onDestroy() {
        destroyAllSockets();
    }

    public void onReset() {
        destroyAllSockets();
    }

    private void destroyAllSockets() {
        System.out.println("destroyAllSockets");
        if (sockets.isEmpty()) return;

        Log.i(TAG, "Destroying all open sockets");

        for (Map.Entry<Integer, SocketData> entry : sockets.entrySet()) {
            SocketData sd = entry.getValue();
            sd.close();
        }
        sockets.clear();
    }
//

//
//
//
//
//
//
//    private void disconnect(CordovaArgs args, final CallbackContext callbackContext) throws JSONException {
//        int socketId = args.getInt(0);
//
//        SocketData sd = sockets.get(Integer.valueOf(socketId));
//        if (sd == null) {
//            Log.e(LOG_TAG, "No tcpSocket with socketId " + socketId);
//            return;
//        }
//
//        sd.disconnect();
//        callbackContext.success();
//    }
//
//    private void destroy(CordovaArgs args, final CallbackContext callbackContext) throws JSONException {
//        int socketId = args.getInt(0);
//
//        SocketData sd = sockets.get(Integer.valueOf(socketId));
//        if (sd == null) {
//            Log.e(LOG_TAG, "No tcpSocket with socketId " + socketId);
//            return;
//        }
//
//        sd.destroy();
//        sockets.remove(Integer.valueOf(socketId));
//        callbackContext.success();
//    }
//
//
//    private void getInfo(CordovaArgs args, final CallbackContext callbackContext) throws JSONException {
//        int socketId = args.getInt(0);
//
//        SocketData sd = sockets.get(Integer.valueOf(socketId));
//        if (sd == null) {
//            Log.e(LOG_TAG, "No tcpSocket with socketId " + socketId);
//            return;
//        }
//
//        JSONObject info = sd.getInfo();
//        callbackContext.success(info);
//    }

    private void close(CordovaArgs args, final CallbackContext context) throws JSONException {
        int socketId = args.getInt(0);

        SocketData sd = sockets.get(Integer.valueOf(socketId));
        if (sd == null) {
            context.error(ERROR_BIND);
            Log.d(TAG, ERROR_BIND);
            return;
        }
        
        sd.close();
        context.success();
    }

    private void socket(CordovaArgs args, final CallbackContext callbackContext) throws JSONException {
        System.out.println("socket");
        String socketType = args.getString(0);
        if (socketType.equals("tcp") || socketType.equals("udp")) {
            SocketData sd = new SocketData(socketType.equals("tcp") ? SocketType.TCP : SocketType.UDP);
            int id = addSocket(sd);
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, id));
        }
        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, -1));
    }

    public void listen(final CordovaArgs args, final CallbackContext context) throws JSONException {
        System.out.println("listen");
        String[] info = args.getString(1).split(":");
        final int socketId = args.getInt(0);
        final String address = info[0];
        final int port = Integer.parseInt(info[1]);
        final int backlog = args.getInt(2);

        final SocketData sd = sockets.get(socketId);
        if (sd == null) {
            context.error(ERROR_BIND);
            Log.d(TAG, ERROR_BIND);
            return;
        }
        cordova.getThreadPool().submit(new Runnable() {
            @Override
            public void run() {
                boolean success = sd.listen(address, port, backlog);
                if (success)
                    context.sendPluginResult(new PluginResult(PluginResult.Status.OK, 1));
                else {
                    context.error(ERROR_BIND);
                    Log.d(TAG, ERROR_BIND);
                }
            }
        });
    }

    public void accept(CordovaArgs args, final CallbackContext context) throws JSONException {
        System.out.println("accept");
        int socketId = args.getInt(0);
        SocketData sd = sockets.get(socketId);
        if (sd == null) {
            context.error(ERROR_ACCEPT);
            Log.d(TAG, ERROR_ACCEPT);
            return;
        }
        sd.accept(context);
    }

    public void select(CordovaArgs args, final CallbackContext context) throws JSONException {
        System.out.println("select");
        final int socketId = args.getInt(0);
        final int timeout = args.getInt(1);

        final SocketData sd = sockets.get(socketId);
        System.out.println("select = " + sd);
        if (sd == null) {
            context.error(ERROR_NOT_CREATE);
            Log.d(TAG, ERROR_NOT_CREATE);
            return;
        }

        cordova.getThreadPool().submit(new Runnable() {
            @Override
            public void run() {
                try {
                    sd.select(timeout, context);
                } catch (Exception e) {
                    e.printStackTrace();
                    context.error(ERROR_CLOSE);
                }
            }
        });
    }

    public void send(CordovaArgs args, final CallbackContext context) throws JSONException {
        System.out.println("send");
        int socketId = args.getInt(0);
        final byte[] data = args.getArrayBuffer(1);

        final SocketData sd = sockets.get(Integer.valueOf(socketId));
        if (sd == null) {
            context.error(ERROR_NOT_CREATE);
            Log.d(TAG, ERROR_NOT_CREATE);
            return;
        }
        cordova.getThreadPool().submit(new Runnable() {
            @Override
            public void run() {
                int result = sd.send(data);
                if (result <= 0) {
                    context.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, result));
                } else {
                    context.sendPluginResult(new PluginResult(PluginResult.Status.OK, result));
                }
            }
        });
    }

    public void recv(CordovaArgs args, final CallbackContext context) throws JSONException {
        System.out.println("recv");
        int socketId = args.getInt(0);
        int bufferSize = args.getInt(1);

        SocketData sd = sockets.get(Integer.valueOf(socketId));
        if (sd == null) {
            context.error(ERROR_NOT_CREATE);
            Log.d(TAG, ERROR_NOT_CREATE);
            return;
        }

        // Will call the callback once it has some data.
        sd.recv(bufferSize, context);
    }

    private void connect(CordovaArgs args, final CallbackContext context) throws JSONException {
        System.out.println("connect");
        String[] info = args.getString(1).split(":");
        final int socketId = args.getInt(0);
        final String address = info[0];
        final int port = Integer.parseInt(info[1]);

        final SocketData sd = sockets.get(Integer.valueOf(socketId));
        if (sd == null) {
            context.error(ERROR_NOT_CREATE);
            Log.d(TAG, ERROR_NOT_CREATE);
            return;
        }

        // The SocketData.connect() method will callback appropriately
        cordova.getThreadPool().submit(new Runnable() {
            @Override
            public void run() {
                try {
                    sd.connect(address, port, context);
                } catch (Exception e) {
                    context.error(ERROR_CONNECT);
                    Log.d(TAG, ERROR_CONNECT);
                    e.printStackTrace();
                }
            }
        });
    }

    private void sendTo(CordovaArgs args, final CallbackContext context) throws JSONException {
        System.out.println("sendTo");
        JSONObject opts = args.getJSONObject(0);
        String[] info = opts.getString("info").split(":");
        final int socketId = opts.getInt("socketId");
        final String address = info[0];
        final int port = Integer.parseInt(info[1]);
        final byte[] data = args.getArrayBuffer(1);

        final SocketData sd = sockets.get(Integer.valueOf(socketId));
        if (sd == null) {
            context.error(ERROR_NOT_CREATE);
            Log.d(TAG, ERROR_NOT_CREATE);
            return;
        }

        cordova.getThreadPool().submit(new Runnable() {
            @Override
            public void run() {
                try {
                    int result = sd.sendTo(address, port, data);
                    if (result <= 0) {
                        context.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, result));
                    } else {
                        context.sendPluginResult(new PluginResult(PluginResult.Status.OK, result));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    context.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, -1));
                }
            }
        });
    }

    private void recvFrom(CordovaArgs args, final CallbackContext context) throws JSONException {
        System.out.println("recvFrom");
        int socketId = args.getInt(0);
        int bufferSize = args.getInt(1);

        SocketData sd = sockets.get(Integer.valueOf(socketId));
        if (sd == null) {
            context.error(ERROR_NOT_CREATE);
            Log.d(TAG, ERROR_NOT_CREATE);
            return;
        }
        sd.recvFrom(bufferSize, context);
    }

    private int addSocket(SocketData sd) {
        sockets.put(Integer.valueOf(nextSocket), sd);
        return nextSocket++;
    }

    private enum SocketType {
        TCP, UDP
    }

    private class SocketData {

        private ServerSocketChannel tcpServer;
        private SocketChannel tcpSocket;
        private Selector selector = null;

        private DatagramChannel udpSocket;
        private Charset charset = Charset.forName("UTF-8");

        private SocketType type;

        // Cached values used by UDP read()/write().
        // These are the REMOTE address and port.
        private InetAddress address;
        private int port;

        private boolean isClose;
        private int localPort;

        private boolean connected = false; // Only applies to UDP, where connect() restricts who the tcpSocket will receive from.
        private boolean bound = false;

        private boolean isServer = false;
        private CallbackContext acceptCallback;
        private CallbackContext recvCallback;
        private CallbackContext recvFromCallback;
        private int readSize = 1024;

        public SocketData(SocketType type) {
            this.type = type;
        }

        public SocketData(SocketChannel incoming) {
            this.type = SocketType.TCP;
            tcpSocket = incoming;
            connected = true;
            address = incoming.socket().getInetAddress();
            port = incoming.socket().getPort();
        }

        public void connect(String address, int port, final CallbackContext context) throws Exception {
            if (isServer) {
                context.error(ERROR_NOT_CLIENT);
                Log.d(TAG, ERROR_NOT_CLIENT);
            }
            init();
            if (type == SocketType.TCP) {
                tcpSocket.connect(new InetSocketAddress(address, port));
            } else {
                udpSocket.connect(new InetSocketAddress(address, port));
            }

            context.success();
            connected = true;
        }

        public boolean listen(String address, int port, int backlog) {
            if (type == SocketType.TCP) {
                try {
                    isServer = true;
                    init();
                    tcpServer.socket().setReuseAddress(true);
                    InetSocketAddress isa = new InetSocketAddress(address, port);
                    tcpServer.socket().bind(isa, backlog);
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    isServer = true;
                    init();
                    udpSocket.socket().bind(new InetSocketAddress(address, port));
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return false;
        }


        private boolean canRunning() {
            if (type == SocketType.TCP) {
                if (isServer)
                    return !isClose;
                else
                    return connected && tcpSocket != null && tcpSocket.isConnected();
            } else {
                return !isClose && udpSocket != null;
            }
        }

        public void close() {
            if (tcpServer != null || tcpSocket != null || udpSocket != null) {
                closeAll();
            }
        }

        private void closeAll() {
            System.out.println("closeAll");
            connected = false;
            isClose = true;
            try {
                if (tcpServer != null)
                    tcpServer.close();

                if (tcpSocket != null)
                    tcpSocket.close();

                if (udpSocket != null)
                    udpSocket.close();

                if(selector != null)
                    selector.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
            tcpSocket = null;
            udpSocket = null;
            tcpServer = null;
            selector = null;
        }

        public void select(int timeout, final CallbackContext context) throws Exception {
            selectImply(timeout, context);
        }

        private void selectImply(int timeout, final CallbackContext context) throws Exception {
            init();
            if(timeout < 0)
                timeout = 0;
            selector = Selector.open();
            if (type == SocketType.TCP) {
                System.out.println("selectImply");
                if (isServer) {
                    tcpServer.configureBlocking(false);
                    tcpServer.register(selector, SelectionKey.OP_ACCEPT);
                } else {
                    tcpSocket.configureBlocking(false);
                    tcpSocket.register(selector, SelectionKey.OP_READ);
                }
            } else {
                System.out.println("udpSelect");
                if (isServer) {
                    udpSocket.configureBlocking(false);
                    udpSocket.register(selector, SelectionKey.OP_READ);
                }
            }

            PluginResult temp = new PluginResult(PluginResult.Status.OK, 0);
            temp.setKeepCallback(true);
            context.sendPluginResult(temp);
            /** 外循环，已经发生了SelectionKey数目 */

            while (canRunning()) {
                int select = selector.select(timeout);
                System.out.println(isServer + " select = " + select);

                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, select);
                pluginResult.setKeepCallback(true);
                context.sendPluginResult(pluginResult);

                if (select > 0) {
                /* 得到已经被捕获了的SelectionKey的集合 */
                    Iterator iterator = selector.selectedKeys().iterator();
                    while (iterator.hasNext()) {
                        SelectionKey key = null;
                        try {
                            key = (SelectionKey) iterator.next();
                            //记住一定要remove这个key，否则之后的新连接将被阻塞无法连接服务器
                            iterator.remove();
                            if (key.isAcceptable()) {
                                selectAccept(key);
                            }

                            if (key.isValid() && key.isReadable()) {
                                selectRead(key);
                            }
//                            if (key.isValid() && key.isWritable()) {
//                                // send(key);
//                            }
                        } catch (Exception e) {
                            context.error(ERROR_CLOSE);
                            e.printStackTrace();
                            try {
                                if (key != null) {
                                    key.cancel();
                                    key.channel().close();
                                }
                            } catch (Exception cex) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
            System.out.println("Select end");
        }

        private void selectAccept(SelectionKey key) throws Exception {
            ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
            //设置为非阻塞
            SocketChannel sc = ssc.accept();
            System.out.println("客户端机子的地址是 "
                    + sc.socket().getRemoteSocketAddress()
                    + "  客户端机机子的端口号是 "
                    + sc.socket().getLocalPort());

            if (acceptCallback != null) {
                SocketData sd = new SocketData(sc);
                int id = UltracreationSocket.this.addSocket(sd);
                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, id);
                pluginResult.setKeepCallback(true);
                acceptCallback.sendPluginResult(pluginResult);
            }
        }

        public int send(byte[] data) {
            int write = 0;
            try {
                write = tcpSocket.write(ByteBuffer.wrap(data));
            } catch (IOException e) {
                e.printStackTrace();
            }

            return write;
        }

        public void recv(int size, CallbackContext context) {
            this.recvCallback = context;
            this.readSize = size;
        }

        public void recvFrom(int size, CallbackContext context) {
            this.recvFromCallback = context;
            this.readSize = size;
        }

        public void selectRead(SelectionKey key) {
            if (key == null)
                return;

            //***用channel.read()获取客户端消息***//
            //：接收时需要考虑字节长度
            SocketChannel sc = null;
            DatagramChannel dc = null;
            if (type == SocketType.TCP) {
                sc = (SocketChannel) key.channel();
                if (sc == null) {
                    return;
                }
                System.out.println("sc2 = " + sc);
            } else {
                dc = (DatagramChannel) key.channel();
                if (dc == null) {
                    return;
                }
                System.out.println("dc = " + dc);
            }


//            String content = "";
            //create buffer with capacity of 48 bytes




            if (this.recvFromCallback != null && type == SocketType.UDP) {
//                        JSONObject obj = new JSONObject();
//                        try {
//                            obj.put("address", sc.hashCode());
//                            obj.put("port", packet.getPort());
//
//                        } catch (JSONException je) {
//                            Log.e(LOG_TAG, "Error constructing JSON object to return from recvFrom()", je);
//                            return;
//                        }
//                        context.success(obj);
                JSONObject obj = new JSONObject();
                try {
                    ByteBuffer byte_buffer = ByteBuffer.allocate(1024);
                    InetSocketAddress remote_address = (InetSocketAddress) ((DatagramChannel) key.channel()).receive(byte_buffer);
                    byte_buffer.flip();
                    byte[] data = new byte[byte_buffer.remaining()];
                    byte_buffer.get(data, 0, data.length);
                    System.out.println("recvFrom 接收：" + new String(data));
                    System.out.println("remote_address = " + remote_address.getHostName());
                    System.out.println("remote_address = " + remote_address.getPort());

                    obj.put("resultCode", data.length);
                    obj.put("data",new String(data));
                    obj.put("address", remote_address.getHostName());
                    obj.put("port", remote_address.getPort());

                } catch (Exception e) {
                    e.printStackTrace();
                }
                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, obj);
                pluginResult.setKeepCallback(true);
                this.recvFromCallback.sendPluginResult(pluginResult);
                return;
            }

            byte[] data = new byte[0];
            ByteBuffer buf = ByteBuffer.allocate(readSize);//java里一个(utf-8)中文3字节,gbk中文占2个字节
            int bytesRead = 0; //read into buffer.

            try {
                if (type == SocketType.TCP) {
                    bytesRead = sc.read(buf);
                    if (bytesRead > 0) {
                        buf.flip();
                        data = new byte[bytesRead];
                        buf.get(data, 0, data.length);
                        key.interestOps(SelectionKey.OP_READ);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("bytesRead = " + bytesRead);

            if (bytesRead < 0) {
                try {
                    sc.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (this.recvCallback != null) {
                    this.recvCallback.error(ERROR_CLOSE);
                }
            } else {
                if (data.length > 0) {
                    System.out.println("接收：" + new String(data));
                    if (this.recvCallback != null) {
                        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, data);
                        pluginResult.setKeepCallback(true);
                        this.recvCallback.sendPluginResult(pluginResult);
                    }
                }
            }
        }

        public int sendTo(String address, int port, byte[] data) throws Exception {
            System.out.println("sendto = " + address +":" + port);
            init();
            ByteBuffer buf = ByteBuffer.allocate(data.length);
            buf.clear();
            buf.put(data);
            buf.flip();
            udpSocket.socket().setBroadcast(true);
            return udpSocket.send(buf, new InetSocketAddress(address, port));
        }

        private synchronized void init() throws Exception {
            if (type == SocketType.TCP) {
                if (isServer && tcpServer == null)
                    tcpServer = ServerSocketChannel.open();
                else if (tcpSocket == null)
                    tcpSocket = SocketChannel.open();
            } else if (type == SocketType.UDP) {
                if (udpSocket == null)
                    udpSocket = DatagramChannel.open();
            }
        }

        public void accept(CallbackContext context) {
            if (!isServer) {
                System.out.println("accept() is not supported on client sockets. Call listen() first.");
                context.error(ERROR_ACCEPT);
                Log.d(TAG, ERROR_ACCEPT);
            }
            this.acceptCallback = context;
        }
    }
}
