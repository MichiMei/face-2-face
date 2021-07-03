package huberlin.p2projekt21;

import static org.junit.Assert.assertTrue;

import datagrams.messages.GenericMessage;
import huberlin.p2projekt21.kademlia.KademliaInstance;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;

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
    public void messageTest() throws Exception {
        int idL = KademliaInstance.NODE_ID_LENGTH-1;
        int rndL = KademliaInstance.RANDOM_ID_LENGTH;
        while ((rndL+1)/8 > rndL/8) rndL++;


        BigInteger id = BigInteger.ONE.shiftLeft(idL).subtract(BigInteger.ONE);
        assertTrue(id.bitLength() == idL);

        BigInteger rnd = BigInteger.ONE.shiftLeft(rndL).subtract(BigInteger.ONE);
        assertTrue(rnd.bitLength() == rndL);

        GenericMessage msg = new GenericMessage(InetAddress.getByName("localhost"), 666, InetAddress.getByName("localhost"), 6969);
        msg.setSenderNodeID(id);
        msg.setRandomID(rnd);

        assertTrue("node id error\nexp: " + id.toString(16) + "\nis:  " + msg.getSenderNodeID().toString(16), msg.getSenderNodeID().equals(id));
        assertTrue("random id error\nexp: " + rnd.toString(16) + "\nis:  " + msg.getRandomID().toString(16), msg.getRandomID().equals(rnd));

    }
}
