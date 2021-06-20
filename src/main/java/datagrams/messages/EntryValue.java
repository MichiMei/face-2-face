package datagrams.messages;

import datagrams.helpers.MessageConstants;

import java.util.Arrays;

public class EntryValue implements IPayload {
    private boolean isValueNotInfo;
    private byte[] entryValue;

    public boolean isValueNotInfo(){
        return isValueNotInfo;
    }
    public void setValueNotInfo(boolean toSet){
        this.isValueNotInfo = toSet;
    }

    public EntryValue(boolean isValueNotInfo, byte[] value){
        this.setValueNotInfo(isValueNotInfo);
        this.setEntryValue(value);
    }

    public EntryValue(){
        this.isValueNotInfo = true;
        this.entryValue = null;
    }

    public void setEntryValue(byte[] in){
        this.entryValue = Arrays.copyOf(in, in.length);
    }

    public byte[] toBytestream(){
        byte[] out = new byte[entryValue.length + 1];
        if(isValueNotInfo){
            out[0] = 0x7F;
        }else{
            out[0] = 0x00;
        }
        System.arraycopy(this.entryValue, 0, out, 1, this.entryValue.length);
        return out;
    }

    public static IPayload fromBytestream(byte[] in){
        boolean isValueNotInfo;
        byte[] entryValue = new byte[in.length - 1];
        if(in[0] == 0x7F){
            isValueNotInfo = true;
        }else{
            isValueNotInfo = false;
        }
        System.arraycopy(in, 1, entryValue, 0, in.length-1);

        return new EntryValue(isValueNotInfo, entryValue);
    }
    public void print() {
        System.out.println("Payload EntryValue");
        MessageConstants.prettyPrintByteArray(this.entryValue);
    }
}
