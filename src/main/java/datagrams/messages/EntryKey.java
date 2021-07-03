package datagrams.messages;

import datagrams.helpers.MessageConstants;

import java.math.BigInteger;
import java.util.Arrays;

public class EntryKey implements IPayload{
    private byte[] EntryKey;
    private byte[] EntryValue;


    public BigInteger getEntryKey(){
        return new BigInteger(EntryKey);
    }
    private EntryKey(){
        EntryKey = new byte[MessageConstants.ENTRYKEY_SIZE_BYTES];
        EntryValue = null;
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

    public void setEntryValue(byte[] in){
        this.EntryValue = Arrays.copyOf(in, in.length);
    }

    @Override
    public byte[] toBytestream() {
        byte[] out = new byte[this.EntryKey.length + this.EntryValue.length + 4];
        byte[] entryValueLength = MessageConstants.intToByteArray(this.EntryValue.length);
        System.arraycopy(entryValueLength, 0, out, 0, 4);
        System.arraycopy(this.EntryKey, 0, out, 4, this.EntryKey.length);
        System.arraycopy(this.EntryValue, 0, out, 4+this.EntryKey.length, this.EntryValue.length);
        return out;
    }
    public static IPayload fromBytestream(byte[] in){
        //int EntryValueLength = in.length - MessageConstants.ENTRYKEY_SIZE_BYTES;
        int EntryValueLength = MessageConstants.byteArrayToInt(Arrays.copyOfRange(in, 0, 4));
        byte[] EntryKey = new byte[MessageConstants.ENTRYKEY_SIZE_BYTES];
        byte[] EntryValue = new byte[EntryValueLength];

        System.arraycopy(in, 4, EntryKey, 0, MessageConstants.ENTRYKEY_SIZE_BYTES);
        System.arraycopy(in, 4 + MessageConstants.ENTRYKEY_SIZE_BYTES, EntryValue, 0, EntryValueLength);

        EntryKey entryKey = new EntryKey();
        entryKey.setEntryKey(EntryKey);
        entryKey.setEntryValue(EntryValue);

        return entryKey;

        /*
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
         */
    }

    public void print() {
        System.out.println("Payload EntryKey:");
        System.out.println("HEX VALUE:");
        MessageConstants.prettyPrintByteArray(this.EntryKey);
        System.out.println("AS BIGINTEGER:");
        System.out.println(new BigInteger(this.EntryKey).toString());
        System.out.println("ENTRYVALUE: ");
        MessageConstants.prettyPrintByteArray(this.EntryValue);
    }
}
