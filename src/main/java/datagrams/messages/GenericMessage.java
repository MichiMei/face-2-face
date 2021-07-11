package datagrams.messages;
import datagrams.helpers.MessageConstants;
import datagrams.helpers.PayloadConverter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.net.InetAddress;
import java.net.DatagramPacket;

public class GenericMessage {
    private byte[] typeHeader;
    private byte[] senderNodeID;
    private byte[] randomID;

    private InetAddress senderIP;
    private int senderPort;

    private InetAddress receiverIP;
    private int receiverPort;

    private IPayload payload;


    public GenericMessage() throws Exception{
        this.senderIP = InetAddress.getByName("127.0.0.1");
        this.receiverIP = InetAddress.getByName("127.0.0.1");

        this.senderPort = 1;
        this.receiverPort = 1;

        this.typeHeader = new byte[MessageConstants.TYPE_SIZE_BYTES];
        this.senderNodeID = new byte[MessageConstants.NODEID_SIZE_BYTES];
        this.randomID = new byte[MessageConstants.RANDOMID_SIZE_BYTES];

        this.payload = null;
    }
    public GenericMessage(InetAddress senderIP, int senderPort, InetAddress receiverIP, int receiverPort) throws Exception{
        this();
        this.senderIP = senderIP;
        this.senderPort = senderPort;
        this.receiverIP = receiverIP;
        this.receiverPort = receiverPort;
    }

    public void setTypeHeader(byte typeHeader){
        this.typeHeader[0] = typeHeader;
    }
    public void setTypeHeader(byte[] typeHeader) {
        this.typeHeader = typeHeader;
    }

    public void setRandomID(byte[] randomID) throws ArrayIndexOutOfBoundsException {
        try{
            if(randomID.length > this.randomID.length){
                throw new ArrayIndexOutOfBoundsException();
            }else{
                this.randomID = MessageConstants.copyOf(randomID, this.randomID.length);
            }
        }catch(Exception e) {
            System.err.println("RandomID Array zu groß!");
            e.printStackTrace();
        }
    }
    public void setRandomID(BigInteger randomID) throws ArrayIndexOutOfBoundsException{
        try{
            if(!MessageConstants.hasLessThan20Bytes(randomID)){
                throw new ArrayIndexOutOfBoundsException();
            }
            this.randomID = MessageConstants.bigIntToByteArray(randomID, this.randomID.length);
        }catch(Exception e){
            System.err.println("RandomID BigInteger zu groß!");
            e.printStackTrace();
        }
    }
    public void setRandomID(){
        ThreadLocalRandom.current().nextBytes(this.randomID);
    }

    public void setSenderNodeID(byte[] senderNodeID){
        this.senderNodeID = senderNodeID;
    }
    public void setSenderNodeID(BigInteger senderNodeID) throws ArrayIndexOutOfBoundsException{
        //System.out.println("BITLENGTHS: " + senderNodeID.bitLength());
        try{
            if (!MessageConstants.hasLessThan32Bytes(senderNodeID)){
                throw new ArrayIndexOutOfBoundsException();
            }else {
                this.senderNodeID = MessageConstants.bigIntToByteArray(senderNodeID, this.senderNodeID.length);
            }
        }catch(Exception e){
            System.err.println("SenderNodeID BigInteger zu groß!");
            e.printStackTrace();
        }
    }
    public void setSenderNodeID(){
        ThreadLocalRandom.current().nextBytes(this.senderNodeID);
    }

    public BigInteger getRandomID() {
        return new BigInteger(1,this.randomID);
    }

    public BigInteger getSenderNodeID() {
        return new BigInteger(1,this.senderNodeID);
    }

    public BigInteger getTypeHeader() {
        return new BigInteger(1, this.typeHeader);
    }
    public void setPayload(IPayload payload){
        this.payload = payload;
    }
    public IPayload getPayload() { return this.payload; }

    private byte[] toByteArray() {
        byte[] payloadStream = null;
        byte[] out = null;
        int payloadLen = 0;

        if(payload != null){
            payloadStream = payload.toBytestream();
            payloadLen = payloadStream.length;
        }
        out = new byte[payloadLen + typeHeader.length + senderNodeID.length + randomID.length];
        try{
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(this.typeHeader);
            outputStream.write(this.senderNodeID);
            outputStream.write(this.randomID);
            if(payload != null){
                outputStream.write(payloadStream);
            }
            out = outputStream.toByteArray();
        }catch(IOException e){
            System.err.println("IOException beim serialisieren eines GenericMessage!");
            e.printStackTrace();
        }
        return out;
    }
    //Füllt die Datenstruktur mit den entsprechenden Daten aus dem Bytearray
    private void fromByteArray(byte[] in){
        int pos = 0;
        System.arraycopy(in,pos, this.typeHeader, 0, this.typeHeader.length);
        pos += this.typeHeader.length;
        System.arraycopy(in, pos, this.senderNodeID, 0,this.senderNodeID.length);
        pos += this.senderNodeID.length;
        System.arraycopy(in, pos, this.randomID, 0,this.randomID.length);
        pos += this.randomID.length;
        int payloadLen = in.length - pos;
        if(payloadLen > 0) {
            byte[] fullPayload = new byte[payloadLen];
            System.arraycopy(in, pos, fullPayload, 0, payloadLen);
            this.payload = PayloadConverter.convertFromBytestream(fullPayload, this.typeHeader[0]);
        }else{
            this.payload = null;
        }
    }


    public DatagramPacket toDatagram() {
        byte[] out = this.toByteArray();
        return new DatagramPacket(out, 0, out.length, this.receiverIP, this.receiverPort);
    }
    public void fromDatagramPacket(DatagramPacket from){
        byte[] buf = from.getData();
        this.fromByteArray(buf);

        this.senderIP = from.getAddress();
        this.senderPort = from.getPort();
    }

    public void print(){
        System.out.println("Printing GenericMessage");
        if (this.senderIP != null) System.out.print("From: " + this.senderIP.toString() + ":" + this.senderPort + " ");
        if (this.receiverIP != null) System.out.println("To: " + this.receiverIP.toString() + ":" + this.receiverPort);
        System.out.println("Type: " + this.typeHeader[0]);
        System.out.println("NodeID");
        MessageConstants.prettyPrintByteArray(this.senderNodeID);
        System.out.println(new BigInteger(this.senderNodeID).toString());
        System.out.println("RandomID");
        MessageConstants.prettyPrintByteArray(this.randomID);
        System.out.println(new BigInteger(this.randomID).toString());

        if(payload != null){
            payload.print();
        }
    }

    public InetAddress getSenderIP() {
        return senderIP;
    }

    public int getSenderPort() {
        return senderPort;
    }
}
