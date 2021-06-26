package datagrams.messages;

public interface IPayload {
    public byte[] toBytestream();
    public void print();
    //public static IPayload fromBytestream(byte[] in);
}
