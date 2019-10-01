package client;

import model.Message;
import org.apache.commons.lang3.SerializationUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class Client {

    private InetSocketAddress hostAddress;

    private SocketChannel client;

    private Selector selector;

    private String name;

    public Client(String host, Integer port) throws IOException {
        this.hostAddress = new InetSocketAddress(host, port);
        this.selector = Selector.open();
    }

    public void run() throws IOException, InterruptedException {
        this.client = SocketChannel.open(this.hostAddress);
        System.out.println("Client... started");
        registrate();

        Runnable read = () -> {
            try {
                read();
            } catch (IOException e) {
                e.printStackTrace();
            }
        };

        Runnable write = () -> {
            try {
                write();
            } catch (IOException e) {
                e.printStackTrace();
            }
        };

        new Thread(read).start();
        new Thread(write).start();

    }

    private void write() throws IOException {
        while (true) {
            Message message = new Message();
            message.setFrom(name);
            System.out.println("-----------------------------------------------------");
            Scanner in = new Scanner(System.in);
            System.out.print("To: ");
            message.setTo(in.nextLine().trim());
            System.out.print("Text: ");
            message.setMessage(in.nextLine().trim());
            System.out.println("-----------------------------------------------------");
            byte[] bytes = convert(message);
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            client.write(buffer);
            buffer.clear();
        }
    }

    private void read() throws IOException {
        while (true) {
            ObjectInputStream ois = new ObjectInputStream(client.socket().getInputStream());
            try {
                Object object = ois.readObject();
                Message message = (Message) object;
                if (!name.equals(message.getFrom())) {
                    System.out.println("-----------------------------------------------------");
                    System.out.println("From: " + message.getFrom());
                    System.out.println("To: " + message.getTo());
                    System.out.println("Text: " + message.getMessage());
                    System.out.println("-----------------------------------------------------");
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
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
        Message message = convert(data);
        System.out.println("-----------------------------------------------------");
        System.out.println("From: " + message.getFrom());
        System.out.println("To: " + message.getTo());
        System.out.println("Text: " + message.getMessage());
        System.out.println("-----------------------------------------------------");
    }

    private Message convert(byte[] data) {
        return SerializationUtils.deserialize(data);
    }

    private byte[] convert(Message data) {
        return SerializationUtils.serialize(data);
    }

    private void registrate() throws IOException {
        this.client = SocketChannel.open(this.hostAddress);
        System.out.println("Your name: ");
        Scanner in = new Scanner(System.in);
        String name = in.nextLine().trim();
        this.name = name;
        byte[] bytes = name.getBytes();
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        client.write(buffer);
        buffer.clear();
    }
}
