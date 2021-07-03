package datagrams.helpers;

import datagrams.messages.*;

public class PayloadConverter {
    public static IPayload convertFromBytestream(byte[] bytesIn, byte type){
        switch(type){
            case MessageConstants.TYPE_PING:
                return null;
            case MessageConstants.TYPE_PONG:
                return null;
            case MessageConstants.TYPE_STORE_ENTRYKEY:
                return EntryKey.fromBytestream(bytesIn);
            case MessageConstants.TYPE_STORE_STATUS:
                return StatusTcpInfo.fromBytestream(bytesIn);
            case MessageConstants.TYPE_FINDNODE:
                return NodeID.fromBytestream(bytesIn);
            case MessageConstants.TYPE_FINDNODE_R:
                return Tuples.fromBytestream(bytesIn);
            case MessageConstants.TYPE_FINDVALUE:
                return NodeID.fromBytestream(bytesIn);
            case MessageConstants.TYPE_FINDVALUE_R:
                return EntryValue.fromBytestream(bytesIn);
        }
        return null;
    }
}
