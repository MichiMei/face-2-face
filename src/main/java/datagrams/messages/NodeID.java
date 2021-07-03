package datagrams.messages;

import datagrams.helpers.MessageConstants;

import java.math.BigInteger;
import java.util.Arrays;

public class NodeID implements IPayload{
    private byte[] NodeID;

    public BigInteger getNodeID(){
        return new BigInteger(1, NodeID);
    }

    public NodeID(BigInteger nodeID) throws ArrayIndexOutOfBoundsException{
        this.setNodeID(nodeID);
    }

    public void setNodeID(BigInteger nodeID) throws ArrayIndexOutOfBoundsException{
        try{
            if(!MessageConstants.hasLessThan32Bytes(nodeID)){
                throw new ArrayIndexOutOfBoundsException();
            }else{
                this.NodeID = MessageConstants.bigIntToByteArray(nodeID, MessageConstants.NODEID_SIZE_BYTES);
            }
        }catch (Exception e){
            System.err.println("BigInteger zu groß für NodeID!");
            e.printStackTrace();
        }
    }

    public NodeID(byte[] nodeID) throws ArrayIndexOutOfBoundsException{
        this.setNodeID(nodeID);
    }

    public void setNodeID(byte[] nodeID) throws ArrayIndexOutOfBoundsException{
        try{
            if(nodeID.length > MessageConstants.NODEID_SIZE_BYTES){
                throw new ArrayIndexOutOfBoundsException();
            }else{
                this.NodeID = MessageConstants.copyOf(nodeID, MessageConstants.NODEID_SIZE_BYTES);
            }
        }catch (Exception e){
            System.err.println("Array zu groß für NodeID!");
            e.printStackTrace();
        }
    }


    public byte[] toBytestream() {
        return MessageConstants.copyOf(this.NodeID, this.NodeID.length);
    }


    public static IPayload fromBytestream(byte[] in) {
        byte[] out = new byte[MessageConstants.NODEID_SIZE_BYTES];

        System.arraycopy(in, 0, out, 0, MessageConstants.NODEID_SIZE_BYTES);
        return new NodeID(out);
        /*
        try{
            if(in.length > MessageConstants.NODEID_SIZE_BYTES){
                throw new ArrayIndexOutOfBoundsException();
            }else{
                return new NodeID(Arrays.copyOf(in, MessageConstants.NODEID_SIZE_BYTES));
            }
        }catch(Exception e){
            System.err.println("Array zu groß für NodeID!");
            e.printStackTrace();
        }
        return null;
         */
    }
    public void print(){
        System.out.println("PAYLOAD NodeID");
        System.out.println("HEX VALUE:");
        MessageConstants.prettyPrintByteArray(this.NodeID);
        System.out.println("AS BIGINTEGER:");
        System.out.println(new BigInteger(NodeID).toString());
    }
}
