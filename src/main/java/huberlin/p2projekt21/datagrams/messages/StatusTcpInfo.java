package huberlin.p2projekt21.datagrams.messages;

import huberlin.p2projekt21.datagrams.helpers.MessageConstants;

import java.util.Arrays;

public class StatusTcpInfo implements IPayload{
    private byte status;
    private byte[] TCPInfo;

    public StatusTcpInfo(byte status, byte[] TCPInfo){
        this();
        this.setStatus(status);
        this.setTCPInfo(TCPInfo);
    }
    public StatusTcpInfo(){
        this.status = 0;
        this.TCPInfo = null;
    }

    public void setStatus(byte statusIn){
        this.status = statusIn;
    }
    public void setStatus(byte[] statusIn) throws ArrayIndexOutOfBoundsException{
        try{
            if(statusIn.length != 1){
                throw new ArrayIndexOutOfBoundsException();
            }else{
                this.status = statusIn[0];
            }
        }catch (Exception e){
            System.err.println("Array zu groß für Status!");
            e.printStackTrace();
        }
    }
    public void setTCPInfo(byte[] in){
        this.TCPInfo = MessageConstants.copyOf(in, in.length);
    }

    public byte getStatus(){
        return status;
    }

    public byte[] getTCPInfo() {
        return MessageConstants.copyOf(TCPInfo, TCPInfo.length);
    }
    public byte[] toBytestream(){
        byte[] out = new byte[TCPInfo.length + 1 + 4];
        byte[] infoLen = MessageConstants.intToByteArray(TCPInfo.length);
        System.arraycopy(infoLen, 0, out, 0, 4);
        out[4] = status;
        System.arraycopy(TCPInfo, 0, out, 5, TCPInfo.length);
        return out;
    }

    public static IPayload fromBytestream(byte[] in){
        byte status = in[4];
        int infoLen = MessageConstants.byteArrayToInt(Arrays.copyOfRange(in, 0, 4));
        byte[] tcpInfo = new byte[infoLen];


        System.arraycopy(in, 5, tcpInfo, 0, infoLen);
        StatusTcpInfo out = new StatusTcpInfo(status, tcpInfo);
        return out;
    }
    public void print() {
        System.out.println("Payload StatusTcpInfo");
        System.out.println("Status: " + this.status);
        System.out.println("TCPInfo:");
        MessageConstants.prettyPrintByteArray(this.TCPInfo);
    }
}
