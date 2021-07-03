package huberlin.p2projekt21;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.net.InetAddress;
import java.net.DatagramPacket;

import datagrams.messages.*;
import datagrams.helpers.*;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
    @Test
    public void maxNodeIDTest() throws Exception {
        int idL = 256;

        BigInteger id = BigInteger.ONE.shiftLeft(idL).subtract(BigInteger.ONE);
        assertTrue(id.bitLength() == idL);

        GenericMessage msg = new GenericMessage(InetAddress.getByName("localhost"), 666, InetAddress.getByName("localhost"), 6969);
        msg.setSenderNodeID(id);

        assertTrue("node id error\nexp: " + id.toString(16) + "\nis:  " + msg.getSenderNodeID().toString(16), msg.getSenderNodeID().equals(id));
    }

    @Test
    public void maxRandomIDTest() throws Exception {
        int rndL = 160;

        BigInteger rnd = BigInteger.ONE.shiftLeft(rndL).subtract(BigInteger.ONE);
        assertTrue(rnd.bitLength() == rndL);

        GenericMessage msg = new GenericMessage(InetAddress.getByName("localhost"), 666, InetAddress.getByName("localhost"), 6969);
        msg.setRandomID(rnd);

        assertTrue("random id error\nexp: " + rnd.toString(16) + "\nis:  " + msg.getRandomID().toString(16), msg.getRandomID().equals(rnd));
    }

    @Test
    public void zeroNodeIDTest() throws Exception {
        int idL = 256;

        BigInteger id = BigInteger.ZERO;
        assertTrue(id.bitLength() <= idL);

        GenericMessage msg = new GenericMessage(InetAddress.getByName("localhost"), 666, InetAddress.getByName("localhost"), 6969);
        msg.setSenderNodeID(id);

        assertTrue("node id error\nexp: " + id.toString(16) + "\nis:  " + msg.getSenderNodeID().toString(16), msg.getSenderNodeID().equals(id));
    }

    @Test
    public void zeroRandomIDTest() throws Exception {
        int rndL = 160;

        BigInteger rnd = BigInteger.ZERO;
        assertTrue(rnd.bitLength() <= rndL);

        GenericMessage msg = new GenericMessage(InetAddress.getByName("localhost"), 666, InetAddress.getByName("localhost"), 6969);
        msg.setRandomID(rnd);

        assertTrue("random id error\nexp: " + rnd.toString(16) + "\nis:  " + msg.getRandomID().toString(16), msg.getRandomID().equals(rnd));
    }

    @Test
    public void oneNodeIDTest() throws Exception {
        int idL = 256;

        BigInteger id = BigInteger.ONE;
        assertTrue(id.bitLength() <= idL);

        GenericMessage msg = new GenericMessage(InetAddress.getByName("localhost"), 666, InetAddress.getByName("localhost"), 6969);
        msg.setSenderNodeID(id);

        assertTrue("node id error\nexp: " + id.toString(16) + "\nis:  " + msg.getSenderNodeID().toString(16), msg.getSenderNodeID().equals(id));
    }

    @Test
    public void oneRandomIDTest() throws Exception {
        int rndL = 160;

        BigInteger rnd = BigInteger.ONE;
        assertTrue(rnd.bitLength() <= rndL);

        GenericMessage msg = new GenericMessage(InetAddress.getByName("localhost"), 666, InetAddress.getByName("localhost"), 6969);
        msg.setRandomID(rnd);

        assertTrue("random id error\nexp: " + rnd.toString(16) + "\nis:  " + msg.getRandomID().toString(16), msg.getRandomID().equals(rnd));
    }
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue()
    {
        assertTrue( true );
    }
}
