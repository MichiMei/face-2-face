package datagrams.messages;

import datagrams.helpers.MessageConstants;

import java.math.BigInteger;
import java.util.Arrays;

public class EntryKey implements IPayload{
    private byte[] EntryKey;

    public BigInteger getEntryKey(){
        return new BigInteger(EntryKey);
    }
    private EntryKey(){
        EntryKey = new byte[MessageConstants.ENTRYKEY_SIZE_BYTES];
    }

    public EntryKey(BigInteger entryKey) throws ArrayIndexOutOfBoundsException{
        this();
        this.setEntryKey(entryKey);
    }

    public void setEntryKey(BigInteger entryKey)throws ArrayIndexOutOfBoundsException {
        try {
            if (entryKey.bitCount() / 8.0 > MessageConstants.ENTRYKEY_SIZE_BYTES) {
                throw new ArrayIndexOutOfBoundsException();
            } else {
                this.EntryKey = Arrays.copyOf(entryKey.toByteArray(), this.EntryKey.length);
            }
        } catch (Exception e) {
            System.err.println("BigInteger zu groß für EntryKey!");
            e.printStackTrace();
        }
    }

    public EntryKey(byte[] entryKey) throws ArrayIndexOutOfBoundsException{
        this();
        this.setEntryKey(entryKey);
    }

    public void setEntryKey(byte[] entryKey)throws ArrayIndexOutOfBoundsException{
        try{
            if(entryKey.length > MessageConstants.ENTRYKEY_SIZE_BYTES){
                throw new ArrayIndexOutOfBoundsException();
            }else{
                this.EntryKey = Arrays.copyOf(entryKey, this.EntryKey.length);
            }
        }catch (Exception e){
            System.err.println("Array zu groß für EntryKey!");
            e.printStackTrace();
        }
    }

    @Override
    public byte[] toBytestream() {
        return Arrays.copyOf(this.EntryKey, this.EntryKey.length);
    }
    public static IPayload fromBytestream(byte[] in){
        try{
            if(in.length > MessageConstants.ENTRYKEY_SIZE_BYTES){
                throw new ArrayIndexOutOfBoundsException();
            }else{
                return new EntryKey(Arrays.copyOf(in, MessageConstants.ENTRYKEY_SIZE_BYTES));
            }
        }catch(Exception e){
            System.err.println("Array zu groß für EntryKey!");
            e.printStackTrace();
        }
        return null;
    }

    public void print() {
        System.out.println("Payload EntryKey:");
        System.out.println("HEX VALUE:");
        MessageConstants.prettyPrintByteArray(this.EntryKey);
        System.out.println("AS BIGINTEGER:");
        System.out.println(new BigInteger(this.EntryKey).toString());
    }
}
