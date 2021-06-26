package huberlin.p2projekt21;
import datagrams.helpers.MessageConstants;
import datagrams.messages.*;


import javax.xml.crypto.Data;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Hello world!
 *
 */
public class App
{
    public static void doPingMessage() throws Exception {

        //PING message
        GenericMessage pingMessage = new GenericMessage(InetAddress.getByName("localhost"), 666, InetAddress.getByName("localhost"), 6969);
        pingMessage.setTypeHeader(MessageConstants.TYPE_PING);
        pingMessage.setRandomID();
        pingMessage.setSenderNodeID();
        pingMessage.print();
        DatagramPacket test = pingMessage.toDatagram();
        pingMessage.fromDatagramPacket(test);
        pingMessage.print();
    }

    public static void doPongMessage() throws Exception{
        //PONG message
        GenericMessage pongMessage = new GenericMessage(InetAddress.getByName("localhost"), 666, InetAddress.getByName("localhost"), 6969);
        pongMessage.setTypeHeader(MessageConstants.TYPE_PONG);
        pongMessage.setRandomID();
        pongMessage.setSenderNodeID();
        pongMessage.print();
        DatagramPacket testPong = pongMessage.toDatagram();
        pongMessage.fromDatagramPacket(testPong);
        pongMessage.print();
    }
    public static void doStoreStatus() throws Exception{
        //StoreR mit Status und TCPInfo
        GenericMessage tcpMessage = new GenericMessage(InetAddress.getByName("localhost"), 666, InetAddress.getByName("localhost"), 6969);
        tcpMessage.setTypeHeader(MessageConstants.TYPE_STORE_STATUS);
        tcpMessage.setSenderNodeID();
        tcpMessage.setRandomID();

        StatusTcpInfo statusTcpInfo = new StatusTcpInfo();
        byte[] randomTCPInfo = new byte[133];
        ThreadLocalRandom.current().nextBytes(randomTCPInfo);
        statusTcpInfo.setTCPInfo(randomTCPInfo);
        statusTcpInfo.setStatus((byte) 17);

        tcpMessage.setPayload(statusTcpInfo);
        tcpMessage.print();

        DatagramPacket tcpDatagram = tcpMessage.toDatagram();
        GenericMessage tcpFrom = new GenericMessage();
        tcpFrom.fromDatagramPacket(tcpDatagram);
        tcpFrom.print();
    }

    public static void doStore() throws Exception{
        //Store mit entrykey und entryvalue
        System.out.println("STORE ENTRYKEY:");
        GenericMessage storeMessage = new GenericMessage(InetAddress.getByName("localhost"), 666, InetAddress.getByName("localhost"), 6969);
        storeMessage.setTypeHeader(MessageConstants.TYPE_STORE_ENTRYKEY);
        storeMessage.setRandomID();
        storeMessage.setSenderNodeID();
        EntryKey entryKey = new EntryKey(new BigInteger(31*8, new Random()));
        byte[] entryValue = new BigInteger(32*32, new Random()).toByteArray();
        entryKey.setEntryValue(entryValue);
        storeMessage.setPayload(entryKey);
        storeMessage.print();


        DatagramPacket testStore = storeMessage.toDatagram();
        GenericMessage storeRecv = new GenericMessage();
        storeRecv.fromDatagramPacket(testStore);
        storeRecv.print();
    }

    public static void doFindNode() throws Exception{
        System.out.println("FINDNODE:");
        GenericMessage storeMessage = new GenericMessage(InetAddress.getByName("localhost"), 666, InetAddress.getByName("localhost"), 6969);
        storeMessage.setTypeHeader(MessageConstants.TYPE_FINDNODE);
        storeMessage.setRandomID();
        storeMessage.setSenderNodeID();

        NodeID nodeID = new NodeID(new BigInteger(32*8, new Random()));
        storeMessage.setPayload(nodeID);
        storeMessage.print();

        DatagramPacket testStore = storeMessage.toDatagram();
        GenericMessage storeRecv = new GenericMessage();
        storeRecv.fromDatagramPacket(testStore);
        storeRecv.print();
    }

    public static void doFindNodeR() throws Exception{
        System.out.println("FINDNODE_R:");
        GenericMessage storeMessage = new GenericMessage(InetAddress.getByName("localhost"), 666, InetAddress.getByName("localhost"), 6969);
        storeMessage.setTypeHeader(MessageConstants.TYPE_FINDNODE_R);
        storeMessage.setRandomID();
        storeMessage.setSenderNodeID();

        Tuples.Tuple tuple1 = new Tuples.Tuple(4000, InetAddress.getByName("localhost"), new NodeID(new BigInteger(31*8, new Random())));
        Tuples.Tuple tuple2 = new Tuples.Tuple(4004, InetAddress.getByName("google.com"), new NodeID(new BigInteger(31*8, new Random())));
        Tuples.Tuple tuple3 = new Tuples.Tuple(4008, InetAddress.getByName("bing.com"), new NodeID(new BigInteger(31*8, new Random())));

        Tuples tuples = new Tuples();
        tuples.addTuple(tuple1);
        tuples.addTuple(tuple2);
        tuples.addTuple(tuple3);
        tuples.addTuple(2014, InetAddress.getByName("microsoft.com"), new NodeID(new BigInteger(31*8, new Random())));
        storeMessage.setPayload(tuples);
        storeMessage.print();

        DatagramPacket testStore = storeMessage.toDatagram();
        GenericMessage storeRecv = new GenericMessage();
        storeRecv.fromDatagramPacket(testStore);
        storeRecv.print();
    }

    public static void doFindValue() throws Exception{
        System.out.println("FINDVALUE:");
        GenericMessage storeMessage = new GenericMessage(InetAddress.getByName("localhost"), 666, InetAddress.getByName("localhost"), 6969);
        storeMessage.setTypeHeader(MessageConstants.TYPE_FINDVALUE);
        storeMessage.setRandomID();
        storeMessage.setSenderNodeID();

        NodeID nodeID = new NodeID(new BigInteger(31*8, new Random()));
        storeMessage.setPayload(nodeID);
        storeMessage.print();

        DatagramPacket testStore = storeMessage.toDatagram();
        GenericMessage storeRecv = new GenericMessage();
        storeRecv.fromDatagramPacket(testStore);
        storeRecv.print();
    }

    public static void doFindValueR() throws Exception{
        System.out.println("FINDVALUE_R:");
        GenericMessage storeMessage = new GenericMessage(InetAddress.getByName("localhost"), 666, InetAddress.getByName("localhost"), 6969);
        storeMessage.setTypeHeader(MessageConstants.TYPE_FINDVALUE_R);
        storeMessage.setRandomID();
        storeMessage.setSenderNodeID();

        byte[] entryVal = new BigInteger(32*123, new Random()).toByteArray();
        //EntryValue entryValue = new EntryValue(true, entryVal); ALTERNATIV
        EntryValue entryValue = new EntryValue();
        entryValue.setEntryValue(entryVal);
        entryValue.setValueNotInfo(true);
        storeMessage.setPayload(entryValue);
        storeMessage.print();

        DatagramPacket testStore = storeMessage.toDatagram();
        GenericMessage storeRecv = new GenericMessage();
        storeRecv.fromDatagramPacket(testStore);
        storeRecv.print();
    }

    public static void main(String[] args) throws Exception{

        try{
           doFindValueR();
       }catch (Exception e){
           System.exit(1);
       }



    }

    public static void printNew(){
        System.out.println("---------------------------------------------------------------------------------");
    }
}
