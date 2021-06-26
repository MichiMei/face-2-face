package datagrams.messages;

import datagrams.helpers.MessageConstants;

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
        this.TCPInfo = Arrays.copyOf(in, in.length);
    }

    public byte getStatus(){
        return status;
    }

    public byte[] getTCPInfo() {
        return Arrays.copyOf(TCPInfo, TCPInfo.length);
    }
    public byte[] toBytestream(){
        byte[] out = new byte[TCPInfo.length + 1];
        out[0] = status;
        System.arraycopy(TCPInfo, 0, out, 1, TCPInfo.length);
        return out;
    }

    public static IPayload fromBytestream(byte[] in){
        byte status = in[0];
        byte[] tcpInfo = new byte[in.length - 1];
        System.arraycopy(in, 1, tcpInfo, 0, in.length - 1);
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