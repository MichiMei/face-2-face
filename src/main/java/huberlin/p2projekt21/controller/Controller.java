package huberlin.p2projekt21.controller;

import huberlin.p2projekt21.networking.Receiver;
import huberlin.p2projekt21.networking.Sender;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.ConcurrentLinkedDeque;

public class Controller {

    private int port;
    private DatagramChannel socket;
    private Sender sender;
    private Receiver receiver;

    Controller(int port){
        this.port = port;
    }

    public static void main(String[] args) {
        System.exit(0);
    }

    public void init() throws IOException {
        this.socket = DatagramChannel.open().bind(new InetSocketAddress(this.port));

        ConcurrentLinkedDeque<DatagramPacket> senderChannel = new ConcurrentLinkedDeque<>();
        ConcurrentLinkedDeque<DatagramPacket> receiverChannel = new ConcurrentLinkedDeque<>();

        this.sender = new Sender(socket, senderChannel);
        this.receiver = new Receiver(socket, receiverChannel);

        this.sender.start();
        this.receiver.start();
    }

    public void terminate() throws IOException {
        this.sender.terminate();
        this.receiver.terminate();
        this.socket.close();
    }
}
