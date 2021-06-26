package huberlin.p2projekt21.kademlia;

import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

public class KademliaInstance implements Runnable{

    public static final int NODE_ID_LENGTH = 256;
    public static final int RANDOM_ID_LENGTH = 160;
    public static final int K = 20;
    public static final int ALPHA = 3;

    private final ConcurrentLinkedDeque<DatagramPacket> incomingChannel;
    private final ConcurrentLinkedDeque<DatagramPacket> outgoingChannel;
    private final Random random;
    private final BigInteger ownID;
    private volatile AtomicBoolean running;

    private final KBuckets kBuckets;
    // TODO map of randomIDs to receivers

    public KademliaInstance(ConcurrentLinkedDeque<DatagramPacket> incomingChannel, ConcurrentLinkedDeque<DatagramPacket> outgoingChannel, InetAddress address, int port) {
        this.incomingChannel = incomingChannel;
        this.outgoingChannel = outgoingChannel;
        random = new Random();
        ownID = new BigInteger(NODE_ID_LENGTH, random);
        running = new AtomicBoolean(true);

        kBuckets = new KBuckets(K, NODE_ID_LENGTH, ownID);

        bootstrapping(address, port);
    }

    @Override
    public void run() {
        while (running.get()) {
            // retrieve next incoming message
            if (incomingChannel.isEmpty()) continue;
            DatagramPacket datagram = incomingChannel.poll();
            if (datagram == null) continue;

            // TODO decode incoming message

            // TODO switch message type
            // TODO request -> answer
            // TODO answer -> check if request in list, compute
        }

        // kademlia stopping
        assert(!running.get());
        // TODO do clean-up stuff here
    }

    /**
     * Eventually stops the kademlia execution
     */
    public void stop() {
        this.running.set(false);
    }

    /**
     * Get the associated value for the given key
     * null is returned if no value could be found
     * @param key search key
     * @return data associated with the key (or null)
     */
    public byte[] getValue(BigInteger key) {
        // TODO
        return null;
    }

    /**
     * Store the given key-value pair
     * @param key key for the given value
     * @param data byte representation of the data to be stored
     * @return TODO probably return void?
     */
    public boolean store(BigInteger key, byte[] data) {
        // TODO
        return false;
    }

    /**
     * Try connecting to a existing kademlia-dht
     * Address == null skips bootstrapping and instead starting a new dht
     * @param address address of a node in the dht
     * @param port port of a node in the dht
     */
    private void bootstrapping(InetAddress address, int port) {
        if (address == null) {
            return;
        }
        assert (port >= 0);
        assert (port <= 65535);
        // TODO send FindNode(nodeID)
    }

    /**
     * Find the k closest to id nodes in the network
     * cf. 2.2 Kademlia protocol
     *
     * @param id reference node to find closest nodes
     * @return list of the k closest nodes
     */
    private List<KademliaNode> lookup(BigInteger id) {
        // TODO local: list = lookup(id, alpha)
        // TODO recursive until closest k nodes responded:
            // TODO async: send FindNode/FindValue to alpha closest (not yet queried) nodes from list
            // TODO register send messages for task particular receive channel
            // TODO wait for incoming replies (TIMEOUT -> continue, edge case all queried)
            // TODO add received nodes to list
            // TODO remove 'silent' nodes from list

        return null;
    }

    /**
     * Generate new RandomID
     * @return new RandomID
     */
    private BigInteger getRandomID() {
        return new BigInteger(RANDOM_ID_LENGTH, random);
    }
}
