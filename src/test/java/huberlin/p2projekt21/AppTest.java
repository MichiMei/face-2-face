package huberlin.p2projekt21;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import huberlin.p2projekt21.datagrams.messages.GenericMessage;
import org.junit.Test;

import java.math.BigInteger;
import java.net.InetAddress;

/**
 * Unit test for simple App.
 */
public class AppTest 
{

    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue()
    {
        assertTrue( true );
    }

    @Test
    public void maxNodeIDTest() throws Exception {
        int idL = 256;

        BigInteger id = BigInteger.ONE.shiftLeft(idL).subtract(BigInteger.ONE);
        assertEquals(id.bitLength(), idL);

        GenericMessage msg = new GenericMessage(InetAddress.getByName("localhost"), 666, InetAddress.getByName("localhost"), 6969);
        msg.setSenderNodeID(id);

        assertEquals("node id error\nexp: " + id.toString(16) + "\nis:  " + msg.getSenderNodeID().toString(16), msg.getSenderNodeID(), id);
    }

    @Test
    public void maxRandomIDTest() throws Exception {
        int rndL = 160;

        BigInteger rnd = BigInteger.ONE.shiftLeft(rndL).subtract(BigInteger.ONE);
        assertEquals(rnd.bitLength(), rndL);

        GenericMessage msg = new GenericMessage(InetAddress.getByName("localhost"), 666, InetAddress.getByName("localhost"), 6969);
        msg.setRandomID(rnd);

        assertEquals("random id error\nexp: " + rnd.toString(16) + "\nis:  " + msg.getRandomID().toString(16), msg.getRandomID(), rnd);
    }

    @Test
    public void zeroNodeIDTest() throws Exception {
        int idL = 256;

        BigInteger id = BigInteger.ZERO;
        assertTrue(id.bitLength() <= idL);

        GenericMessage msg = new GenericMessage(InetAddress.getByName("localhost"), 666, InetAddress.getByName("localhost"), 6969);
        msg.setSenderNodeID(id);

        assertEquals("node id error\nexp: " + id.toString(16) + "\nis:  " + msg.getSenderNodeID().toString(16), msg.getSenderNodeID(), id);
    }

    @Test
    public void zeroRandomIDTest() throws Exception {
        int rndL = 160;

        BigInteger rnd = BigInteger.ZERO;
        assertTrue(rnd.bitLength() <= rndL);

        GenericMessage msg = new GenericMessage(InetAddress.getByName("localhost"), 666, InetAddress.getByName("localhost"), 6969);
        msg.setRandomID(rnd);

        assertEquals("random id error\nexp: " + rnd.toString(16) + "\nis:  " + msg.getRandomID().toString(16), msg.getRandomID(), rnd);
    }

    @Test
    public void oneNodeIDTest() throws Exception {
        int idL = 256;

        BigInteger id = BigInteger.ONE;
        assertTrue(id.bitLength() <= idL);

        GenericMessage msg = new GenericMessage(InetAddress.getByName("localhost"), 666, InetAddress.getByName("localhost"), 6969);
        msg.setSenderNodeID(id);

        assertEquals("node id error\nexp: " + id.toString(16) + "\nis:  " + msg.getSenderNodeID().toString(16), msg.getSenderNodeID(), id);
    }

    @Test
    public void oneRandomIDTest() throws Exception {
        int rndL = 160;

        BigInteger rnd = BigInteger.ONE;
        assertTrue(rnd.bitLength() <= rndL);

        GenericMessage msg = new GenericMessage(InetAddress.getByName("localhost"), 666, InetAddress.getByName("localhost"), 6969);
        msg.setRandomID(rnd);

        assertEquals("random id error\nexp: " + rnd.toString(16) + "\nis:  " + msg.getRandomID().toString(16), msg.getRandomID(), rnd);
    }
}
