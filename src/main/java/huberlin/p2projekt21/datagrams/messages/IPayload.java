package huberlin.p2projekt21.datagrams.messages;

public interface IPayload {
    public byte[] toBytestream();
    public void print();
    //public static IPayload fromBytestream(byte[] in);
}
