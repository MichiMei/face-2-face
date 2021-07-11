package huberlin.p2projekt21.kademlia;

import huberlin.p2projekt21.Helper;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public class Data {
    public static final int SIGNATURE_LENGTH_BYTES = 256;
    public static final int RSA_KEY_LENGTH_BYTES = 294;

    final byte[] data;
    final byte[] signature;
    final byte[] publicKey;

    public Data(byte[] data, byte[] signature, byte[] publicKey) {
        assert(signature.length == SIGNATURE_LENGTH_BYTES);
        assert(publicKey.length == RSA_KEY_LENGTH_BYTES);
        this.data = data;
        this.signature = signature;
        this.publicKey = publicKey;
    }

    public Data(byte[] bytes) {
        assert (bytes.length > SIGNATURE_LENGTH_BYTES+RSA_KEY_LENGTH_BYTES);
        this.data = new byte[bytes.length-SIGNATURE_LENGTH_BYTES-RSA_KEY_LENGTH_BYTES];
        this.signature = new byte[SIGNATURE_LENGTH_BYTES];
        this.publicKey = new byte[RSA_KEY_LENGTH_BYTES];

        int pos = 0;
        System.arraycopy(bytes, pos, publicKey, 0, RSA_KEY_LENGTH_BYTES);
        pos += RSA_KEY_LENGTH_BYTES;
        System.arraycopy(bytes, pos, signature, 0, SIGNATURE_LENGTH_BYTES);
        pos += SIGNATURE_LENGTH_BYTES;
        System.arraycopy(bytes, pos, data, 0, data.length);
        pos += data.length;
        assert (pos == bytes.length);
    }

    public Data(byte[][] tmp) {
        data = tmp[0];
        signature = tmp[1];
        publicKey = tmp[2];
        assert(signature.length == SIGNATURE_LENGTH_BYTES);
        assert(publicKey.length == RSA_KEY_LENGTH_BYTES);
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public byte[] getData() {
        return data;
    }

    public byte[] getSignature() {
        return signature;
    }

    public byte[] toBytes() {
        assert(signature.length == SIGNATURE_LENGTH_BYTES);
        assert(publicKey.length == RSA_KEY_LENGTH_BYTES);

        byte[] bytes = new byte[SIGNATURE_LENGTH_BYTES+RSA_KEY_LENGTH_BYTES+data.length];
        int pos = 0;
        System.arraycopy(publicKey, 0, bytes, pos, RSA_KEY_LENGTH_BYTES);
        pos += RSA_KEY_LENGTH_BYTES;
        System.arraycopy(signature, 0, bytes, pos, SIGNATURE_LENGTH_BYTES);
        pos += SIGNATURE_LENGTH_BYTES;
        System.arraycopy(data, 0, bytes, pos, data.length);
        pos += data.length;
        assert (pos == bytes.length);
        return bytes;
    }

    public BigInteger getKeyHash() throws InvalidKeySpecException, NoSuchAlgorithmException {
        return Helper.bigIntHashForKey(Helper.getPublicKeyFromBytes(this.publicKey));
    }
}
