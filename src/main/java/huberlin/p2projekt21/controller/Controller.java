package huberlin.p2projekt21.controller;

import huberlin.p2projekt21.kademlia.KademliaInstance;
import huberlin.p2projekt21.networking.Receiver;
import huberlin.p2projekt21.networking.Sender;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.ConcurrentLinkedDeque;

public class Controller {

    private int port;
    private DatagramChannel socket;
    private Sender sender;
    private Receiver receiver;
    private KademliaInstance kademlia;

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

        InetAddress address = null;     // TODO get bootstrapping ip
        int port = -1;                  // TODO get bootstrapping port
        this.kademlia = new KademliaInstance(receiverChannel, senderChannel, address, port);

        this.kademlia.start();
    }

    public void terminate() throws IOException {
        this.sender.terminate();
        this.receiver.terminate();
        this.kademlia.stop();
        this.socket.close();
    }
}
