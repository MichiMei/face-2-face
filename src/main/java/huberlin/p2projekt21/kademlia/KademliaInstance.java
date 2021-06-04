package huberlin.p2projekt21.kademlia;

import java.net.DatagramPacket;
import java.util.concurrent.ConcurrentLinkedDeque;

public class KademliaInstance implements Runnable{

    private final ConcurrentLinkedDeque<DatagramPacket> INCOMING_CHANNEL;
    private final ConcurrentLinkedDeque<DatagramPacket> OUTGOING_CHANNEL;

    public KademliaInstance(ConcurrentLinkedDeque<DatagramPacket> incomingChannel, ConcurrentLinkedDeque<DatagramPacket> outgoingChannel) {
        this.INCOMING_CHANNEL = incomingChannel;
        this.OUTGOING_CHANNEL = outgoingChannel;
    }

    @Override
    public void run() {

    }
}
