package filesys;

public class Block {
    private byte[] data;

    public Block(byte[] size) {
        this.data = size;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
