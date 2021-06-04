package huberlin.p2projekt21.networking;

import java.net.DatagramPacket;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.ConcurrentLinkedDeque;

public class Sender implements Runnable{

    private final DatagramChannel SOCKET;
    private final ConcurrentLinkedDeque<DatagramPacket> CHANNEL;

    public Sender(DatagramChannel socket, ConcurrentLinkedDeque<DatagramPacket> channel) {
        this.SOCKET = socket;
        this.CHANNEL = channel;
    }

    @Override
    public void run() {

    }
}
