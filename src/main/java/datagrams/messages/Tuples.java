package datagrams.messages;

import datagrams.helpers.MessageConstants;

import java.math.BigInteger;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class Tuples implements IPayload{
    public static class Tuple{
        private int port;
        private InetAddress IpAddress;
        private NodeID nodeID;

        public Tuple(int port, InetAddress IpAddress, NodeID nodeID){
            this.port = port;
            this.IpAddress = IpAddress;
            this.nodeID = nodeID;
        }
        public int getPort() {
            return this.port;
        }
        public InetAddress getIpAddress(){
            return this.IpAddress;
        }
        public NodeID getNodeID(){
            return this.nodeID;
        }
    }

    private int noTuples;
    private final int TUPLE_SIZE_BYTES = 8 + MessageConstants.NODEID_SIZE_BYTES;
    private ArrayList<Tuple> tupleArrayList;

    public Tuples(){
        this.noTuples = 0;
        this.tupleArrayList = new ArrayList<Tuple>();
    }
    public void addTuple(Tuple toAdd){
        tupleArrayList.add(toAdd);
        ++this.noTuples;
    }
    public void addTuple(int port, InetAddress IpAddress, NodeID nodeID){
        Tuple toAdd = new Tuple(port, IpAddress, nodeID);
        this.addTuple(toAdd);
    }
    public Tuple[] getTuplesAsArray(){
        return tupleArrayList.toArray(new Tuple[0]);
    }

    public byte[] toBytestream() {
        byte[] out = new byte[(noTuples * TUPLE_SIZE_BYTES) + 4];
        byte[] noTuplesByte = ByteBuffer.allocate(4).putInt(noTuples).array();
        System.arraycopy(noTuplesByte, 0, out, 0, noTuplesByte.length);
        int currentPos = noTuplesByte.length;
        Tuple[] tuplesToBytestream = this.getTuplesAsArray();

        for(int iii = 0; iii < tuplesToBytestream.length; iii++){
            Tuple currentTuple = tuplesToBytestream[iii];
            byte[] combinedTuple = new byte[TUPLE_SIZE_BYTES];

            byte[] portByte = ByteBuffer.allocate(4).putInt(currentTuple.getPort()).array();
            System.arraycopy(portByte, 0, combinedTuple, 0, portByte.length);

            byte[] ipByte = currentTuple.getIpAddress().getAddress();
            System.arraycopy(ipByte, 0, combinedTuple, portByte.length, ipByte.length);

            byte[] nodeIdByte = currentTuple.getNodeID().toBytestream();
            System.arraycopy(nodeIdByte, 0, combinedTuple, portByte.length + ipByte.length, nodeIdByte.length);

            System.arraycopy(combinedTuple, 0, out, currentPos, combinedTuple.length);
            currentPos += TUPLE_SIZE_BYTES;
        }
        return out;
    }

    public static IPayload fromBytestream(byte[] in){
        int noTuples = in[0] << 24 | (in[1] & 0xFF) << 16 | (in[2] & 0xFF) << 8 | (in[3] & 0xFF);
        int currentPos = 4;
        Tuples out = new Tuples();

        for (int iii = 0; iii < noTuples; iii++){
            int port = in[currentPos] << 24 | (in[currentPos + 1] & 0xFF) << 16 | (in[currentPos + 2] & 0xFF) << 8 | (in[currentPos + 3] & 0xFF);
            currentPos += 4;

            byte[] ipBytes = new byte[4];
            System.arraycopy(in, currentPos, ipBytes, 0, 4);
            InetAddress ipAddress = null;
            try{
                ipAddress = InetAddress.getByAddress(ipBytes);
            }catch(Exception e){
                System.err.println("Unknown host exception Tuples fromBytestream");
                e.printStackTrace();
            }

            currentPos += 4;

            byte[] nodeIdByte = new byte[MessageConstants.NODEID_SIZE_BYTES];
            System.arraycopy(in, currentPos, nodeIdByte, 0, MessageConstants.NODEID_SIZE_BYTES);
            IPayload nodeID = NodeID.fromBytestream(nodeIdByte);
            currentPos += MessageConstants.NODEID_SIZE_BYTES;

            out.addTuple(port, ipAddress, (NodeID) nodeID);
        }
        return out;
    }

    public void print() {
        System.out.println("Payload Tuples:");
        System.out.println("NoTuples:" + this.noTuples);

        Tuple[] tuples = tupleArrayList.toArray(new Tuple[0]);

        for (int iii = 0; iii < noTuples; iii++) {
            Tuple tuple = tuples[iii];
            System.out.println("Tuple " + iii);
            System.out.println("IP & Port: " + tuple.getIpAddress().getHostAddress() + ":" + tuple.getPort());
            System.out.println("NodeID HEX VALUE:");
            MessageConstants.prettyPrintByteArray(tuple.getNodeID().toBytestream());
            System.out.println("AS BIGINTEGER:");
            System.out.println(new BigInteger(tuple.getNodeID().toBytestream()).toString());
        }
    }
}
