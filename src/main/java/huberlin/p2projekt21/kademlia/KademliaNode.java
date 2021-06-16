package huberlin.p2projekt21.kademlia;

import java.math.BigInteger;
import java.net.InetAddress;

public class KademliaNode {
    private final BigInteger id;
    private final InetAddress address;
    private final int port;

    public KademliaNode(BigInteger id, InetAddress address, int port) {
        this.id = id;
        this.address = address;
        this.port = port;
    }

    public BigInteger getId() {
        return id;
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }
}
