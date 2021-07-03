package datagrams.helpers;

import java.math.BigInteger;
import java.nio.ByteBuffer;

public class MessageConstants {
    private MessageConstants(){}

    public static final int HEADER_SIZE_BYTES = 53;
    public static final int TYPE_SIZE_BYTES = 1;
    public static final int NODEID_SIZE_BYTES = 32;
    public static final int RANDOMID_SIZE_BYTES = 20;
    public static final int ENTRYKEY_SIZE_BYTES = 32;
    public static final int STATUS_SIZE_BYTES = 1;

    public static final byte TYPE_HEADER = 0;
    public static final byte TYPE_PING = 1;
    public static final byte TYPE_PONG = 2;
    public static final byte TYPE_STORE_ENTRYKEY = 3;
    public static final byte TYPE_STORE_STATUS = 4;
    public static final byte TYPE_FINDNODE = 5;
    public static final byte TYPE_FINDNODE_R = 6;
    public static final byte TYPE_FINDVALUE = 7;
    public static final byte TYPE_FINDVALUE_R = 8;

    public static void prettyPrintByteArray(byte[] msg){
        for (int j = 1; j < msg.length+1; j++) {
            if (j % 8 == 1 || j == 0) {
                if( j != 0){
                    System.out.println();
                }
                System.out.format("0%d\t|\t", j / 8);
            }
            System.out.format("%02X", msg[j-1]);
            if (j % 4 == 0) {
                System.out.print(" ");
            }
        }
        System.out.println();
    }

    public static boolean hasLessThan32Bytes(BigInteger bigInt){
        if(bigInt.signum() == -1){
            return bigInt.bitLength() <= 31*8;
        }else{
            return bigInt.bitLength() <= 32*8;
        }
    }
    public static boolean hasLessThan20Bytes(BigInteger bigInt){
        if(bigInt.signum() == -1){
            return bigInt.bitLength() <= 19*8;
        }else{
            return bigInt.bitLength() <= 20*8;
        }
    }

    public static byte[] intToByteArray(int toArray){
        return ByteBuffer.allocate(4).putInt(toArray).array();
    }
    public static int byteArrayToInt(byte[] in){
        return in[0] << 24 | (in[1] & 0xFF) << 16 | (in[2] & 0xFF) << 8 | (in[3] & 0xFF);
    }
}
