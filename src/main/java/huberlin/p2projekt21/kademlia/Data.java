package huberlin.p2projekt21.kademlia;

import huberlin.p2projekt21.Helper;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

public class Data {
    public static final int SIGNATURE_LENGTH_BYTES = 256;
    public static final int RSA_KEY_LENGTH_BYTES = 294;

    final Page page;
    final byte[] signature;
    final byte[] publicKey;

    /**
     * Create data from page, signature, publicKey and timestamp
     *
     * @param page serialized page
     * @param signature serialized signature
     * @param publicKey serialized publicKey
     * @param timeStamp timeStamp of that page
     */
    public Data(byte[] page, byte[] signature, byte[] publicKey, long timeStamp) {
        assert(signature.length == SIGNATURE_LENGTH_BYTES);
        assert(publicKey.length == RSA_KEY_LENGTH_BYTES);
        this.page = new Page(page, timeStamp);
        this.signature = signature;
        this.publicKey = publicKey;
    }

    /**
     * Deserialize page
     *
     * @param bytes serialized page
     */
    public Data(byte[] bytes) {
        assert (bytes.length > SIGNATURE_LENGTH_BYTES+RSA_KEY_LENGTH_BYTES);
        byte[] tmp = new byte[bytes.length-SIGNATURE_LENGTH_BYTES-RSA_KEY_LENGTH_BYTES];
        this.signature = new byte[SIGNATURE_LENGTH_BYTES];
        this.publicKey = new byte[RSA_KEY_LENGTH_BYTES];

        int pos = 0;
        System.arraycopy(bytes, pos, publicKey, 0, RSA_KEY_LENGTH_BYTES);
        pos += RSA_KEY_LENGTH_BYTES;
        System.arraycopy(bytes, pos, signature, 0, SIGNATURE_LENGTH_BYTES);
        pos += SIGNATURE_LENGTH_BYTES;
        System.arraycopy(bytes, pos, tmp, 0, tmp.length);
        pos += tmp.length;
        assert (pos == bytes.length);
        this.page = new Page(tmp);
    }

    /**
     * Deserialize page from array of serialized page, signature and key (in that order)
     *
     * @param tmp array of serialized page, signature and key (in that order)
     */
    public Data(byte[][] tmp) {
        page = new Page(tmp[0]);
        signature = tmp[1];
        publicKey = tmp[2];
        assert(signature.length == SIGNATURE_LENGTH_BYTES);
        assert(publicKey.length == RSA_KEY_LENGTH_BYTES);
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public Page getPage() {
        return page;
    }

    public byte[] getSignature() {
        return signature;
    }

    public byte[] toBytes() {
        assert(signature.length == SIGNATURE_LENGTH_BYTES);
        assert(publicKey.length == RSA_KEY_LENGTH_BYTES);

        byte[] dataBytes = page.toBytes();
        byte[] bytes = new byte[SIGNATURE_LENGTH_BYTES+RSA_KEY_LENGTH_BYTES+dataBytes.length];
        int pos = 0;
        System.arraycopy(publicKey, 0, bytes, pos, RSA_KEY_LENGTH_BYTES);
        pos += RSA_KEY_LENGTH_BYTES;
        System.arraycopy(signature, 0, bytes, pos, SIGNATURE_LENGTH_BYTES);
        pos += SIGNATURE_LENGTH_BYTES;
        System.arraycopy(dataBytes, 0, bytes, pos, dataBytes.length);
        pos += dataBytes.length;
        assert (pos == bytes.length);
        return bytes;
    }

    public BigInteger getKeyHash() throws InvalidKeySpecException, NoSuchAlgorithmException {
        return Helper.bigIntHashForKey(Helper.getPublicKeyFromBytes(this.publicKey));
    }

    /**
     * Returns if the owner of that data and the other one is the same
     * (meaning the same public key)
     *
     * @param other other data
     * @return true if same author, false otherwise
     */
    public boolean sameOwner(Data other) {
        return Arrays.equals(this.publicKey, other.publicKey);
    }

    /**
     * Compares the timeStamp of the Pages
     * 0: equal
     * <0: other is newer
     * >0: this is newer
     *
     * @param other other data
     * @return which page is newer
     * @throws DifferentAuthorsException thrown if authors of the pages differ
     */
    public int compareDate(Data other) throws DifferentAuthorsException {
        if (!this.sameOwner(other)) {
            throw new DifferentAuthorsException("authors differ");
        }

        return Long.compare(this.page.timeStamp, other.page.timeStamp);
    }

    public class Page {
        final byte[] data;
        final long timeStamp;

        /**
         * Create new Page
         *
         * @param data data of the page
         * @param timeStamp timestamp of the page
         */
        public Page(byte[] data, long timeStamp) {
            this.data = data;
            this.timeStamp = timeStamp;
        }

        /**
         * Deserialize serialized page
         *
         * @param serialized serialized page
         */
        public Page(byte[] serialized) {
            byte[] tmp = new byte[Long.BYTES];
            System.arraycopy(serialized, 0, tmp, 0, tmp.length);
            this.timeStamp = bytesToLong(tmp);
            this.data = new byte[serialized.length-tmp.length];
            System.arraycopy(serialized, tmp.length, this.data, 0, data.length);
        }

        public byte[] getData() {
            return data;
        }

        public long getTimeStamp() {
            return timeStamp;
        }

        public byte[] toBytes() {
            byte[] res = new byte[data.length+Long.BYTES];
            byte[] tmp = longToBytes(timeStamp);
            System.arraycopy(tmp, 0, res, 0, tmp.length);
            System.arraycopy(data, 0, res, tmp.length, data.length);
            return res;
        }

        /**
         * Serialize long
         *
         * Source: Brad Mace, https://stackoverflow.com/questions/4485128/how-do-i-convert-long-to-byte-and-back-in-java
         *
         * @param x long to serialize
         * @return byte array representing the long
         */
        private byte[] longToBytes(long x) {
            ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
            buffer.putLong(x);
            return buffer.array();
        }

        /**
         * Deserialize long
         *
         * Source: Brad Mace, https://stackoverflow.com/questions/4485128/how-do-i-convert-long-to-byte-and-back-in-java
         *
         * @param bytes byte array to deserialize
         * @return deserialized long
         */
        private long bytesToLong(byte[] bytes) {
            ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
            buffer.put(bytes);
            buffer.flip();//need flip
            return buffer.getLong();
        }
    }

    public class DifferentAuthorsException extends Exception {
        public DifferentAuthorsException(String msg) {
            super(msg);
        }
        public DifferentAuthorsException() {
            super();
        }
    }
}
