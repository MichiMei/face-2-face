package huberlin.p2projekt21.networking;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.ConcurrentLinkedDeque;

public class Receiver implements Runnable{

    private final DatagramChannel SOCKET;
    private final ConcurrentLinkedDeque<DatagramPacket> CHANNEL;
    private final Thread THREAD;
    private volatile boolean running;


    public Receiver(DatagramChannel socket, ConcurrentLinkedDeque<DatagramPacket> channel) {
        this.SOCKET = socket;
        this.CHANNEL = channel;
        this.THREAD = new Thread(this);
        this.running = false;
    }

    public void start(){
        this.running = true;
        this.THREAD.start();
    }

    public void terminate(){
        this.running = false;
    }

    public void receiveMessage() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(65527);
        SocketAddress senderAddress = this.SOCKET.receive(buffer);
        if(senderAddress != null){
            byte[] payload = buffer.array();
            DatagramPacket datagram = new DatagramPacket(payload, payload.length, senderAddress);
            CHANNEL.add(datagram);
        }
    }

    @Override
    public void run() {
        while (this.running){
            try {
                this.receiveMessage();
                Thread.sleep(10);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
