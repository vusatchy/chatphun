package server;

import model.Message;
import org.apache.commons.lang3.SerializationUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

    private Map<SocketChannel, String> users = new ConcurrentHashMap<>();

    private Selector selector;

    private InetSocketAddress inetAddress;


    private InetSocketAddress listenAddress;

    public Server(String host, Integer port) {
        inetAddress = new InetSocketAddress(port);
    }

    private void startUp() throws IOException {
        Selector selector = null;
        ServerSocket serverSocket = null;
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocket = serverSocketChannel.socket();
        serverSocket.bind(inetAddress);
        selector = Selector.open();
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        this.selector = selector;
        System.out.println("Server started");
    }

    private void process() throws IOException {
        while (true) {
            int num = selector.select();
            if (num == 0) {
                continue;
            }

            Iterator keys = this.selector.selectedKeys().iterator();
            while (keys.hasNext()) {
                SelectionKey key = (SelectionKey) keys.next();
                keys.remove();

                if (!key.isValid()) {
                    continue;
                }

                if (key.isAcceptable()) {
                    this.accept(key);
                } else if (key.isReadable()) {
                    this.read(key);
                }
            }
        }

    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int numRead = -1;
        numRead = channel.read(buffer);

        if (numRead == -1) {
            Socket socket = channel.socket();
            SocketAddress remoteAddr = socket.getRemoteSocketAddress();
            System.out.println("Connection closed by client: " + remoteAddr);
            channel.close();
            key.cancel();
            return;
        }

        byte[] data = new byte[numRead];
        System.arraycopy(buffer.array(), 0, data, 0, numRead);
        if (!users.containsKey(channel)) {
            String name = new String(data);
            users.put(channel, name);
            System.out.println("Registered user " + name);
        } else {
            Message message = convert(data);
            System.out.println("Sending message " + message);
            if (message.getTo().equals("all")) {
                sendTo(message, users.keySet());
            } else {
                SocketChannel to = findChanel(message.getTo());
                sendTo(message, to);
            }
        }
    }

    private void sendTo(Message message, SocketChannel channel) {
        List<SocketChannel> socketChannels = new ArrayList<>();
        socketChannels.add(channel);
        sendTo(message, socketChannels);
    }

    private void sendTo(Message message, Collection<SocketChannel> channels) {
        channels.forEach(channel -> {
            byte[] bytes = convert(message);
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            try {
                channel.write(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
            buffer.clear();
        });
    }

    private SocketChannel findChanel(String to) {
        final SocketChannel[] socketChannel = {null};
        users.forEach((SocketChannel k, String v) -> {
            if (v.equals(to)) {
                socketChannel[0] = k;
            }
        });
        return socketChannel[0];
    }

    private Message convert(byte[] data) {
        return SerializationUtils.deserialize(data);
    }

    private byte[] convert(Message data) {
        return SerializationUtils.serialize(data);
    }

    private byte[] read(SocketChannel channel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int numRead = -1;
        numRead = channel.read(buffer);

        if (numRead == -1) {
            return new byte[]{};
        }

        byte[] data = new byte[numRead];
        System.arraycopy(buffer.array(), 0, data, 0, numRead);
        return data;
    }

    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel channel = serverChannel.accept();
        channel.configureBlocking(false);
        channel.register(this.selector, SelectionKey.OP_READ);
    }

    public void run() throws IOException {
        startUp();
        process();
    }

}
