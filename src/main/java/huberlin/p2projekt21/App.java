package huberlin.p2projekt21;
import datagrams.helpers.MessageConstants;
import datagrams.messages.EntryKey;
import datagrams.messages.GenericMessage;
import datagrams.messages.StatusTcpInfo;


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
    public static void main(String[] args) throws Exception{
        //PING MESSAGE
        GenericMessage pingMessage = new GenericMessage(InetAddress.getByName("localhost"), 666, InetAddress.getByName("localhost"), 6969);
        pingMessage.setTypeHeader(MessageConstants.TYPE_PING);
        pingMessage.setRandomID();
        pingMessage.setSenderNodeID();
        pingMessage.print();
        DatagramPacket test = pingMessage.toDatagram();
        pingMessage.fromDatagramPacket(test);
        //pingMessage.print();

        printNew();

        //PONG MESSAGE
        GenericMessage pongMessage = new GenericMessage(InetAddress.getByName("localhost"), 666, InetAddress.getByName("localhost"), 6969);
        pongMessage.setTypeHeader(MessageConstants.TYPE_PONG);
        pongMessage.setRandomID();
        pongMessage.setSenderNodeID();
        pongMessage.print();
        DatagramPacket testPong = pongMessage.toDatagram();
        pongMessage.fromDatagramPacket(testPong);
        //pongMessage.print();

        printNew();
        //StoreMessage
        System.out.println("STORE ENTRYKEY:");
        GenericMessage storeMessage = new GenericMessage(InetAddress.getByName("localhost"), 666, InetAddress.getByName("localhost"), 6969);
        storeMessage.setTypeHeader(MessageConstants.TYPE_STORE_ENTRYKEY);
        storeMessage.setRandomID();
        storeMessage.setSenderNodeID();
        EntryKey entryKey = new EntryKey(new BigInteger(32*8, new Random()));
        storeMessage.setPayload(entryKey);
        //storeMessage.print();


        DatagramPacket testStore = storeMessage.toDatagram();
        GenericMessage storeRecv = new GenericMessage();
        storeRecv.fromDatagramPacket(testStore);
        //storeRecv.print();

        printNew();
        //Store Status
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

    public static void printNew(){
        System.out.println("---------------------------------------------------------------------------------");
    }
}
