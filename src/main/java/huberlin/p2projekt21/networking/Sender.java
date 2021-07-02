package huberlin.p2projekt21.networking;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Logger;

public class Sender implements Runnable{

    private final DatagramChannel SOCKET;
    private final ConcurrentLinkedDeque<DatagramPacket> CHANNEL;
    private final Thread THREAD;
    private volatile boolean running;

    public Sender(DatagramChannel socket, ConcurrentLinkedDeque<DatagramPacket> channel) {
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

    public void sendMessage() throws IOException {
        if(!this.CHANNEL.isEmpty()){
            DatagramPacket datagram = this.CHANNEL.remove();
            ByteBuffer buffer = ByteBuffer.wrap(datagram.getData());
            SocketAddress receiverAddress = datagram.getSocketAddress();
            this.SOCKET.send(buffer, receiverAddress);
        }
    }

    @Override
    public void run() {
        while (this.running){
            try {
                this.sendMessage();
                Thread.sleep(10);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
