package filesys;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import exception.PermissaoException;

public class File {
    private Metadata metadata;
    private static final int BLOCK_SIZE = 4096;
    private List<Block> blocks = new ArrayList<>();

    public void write(byte[] buffer, boolean append) throws PermissaoException {
        if (metadata.getPermissions().values().stream().noneMatch(p -> p.contains("w"))) {
            throw new PermissaoException("User " + metadata.getOwner() + " doesn't have permission to write!");
        }

        if (append) {
            blocks.clear();
        }

        int offset = 0;
        while (offset < buffer.length) {
            int remaining = buffer.length - offset;
            int chunkSize = Math.min(BLOCK_SIZE, remaining);
            byte[] chunk = new byte[chunkSize];
            System.arraycopy(buffer, offset, chunk, 0, chunkSize);
            blocks.add(new Block(chunk));
            offset += chunkSize;
        }
    }

    public byte[] read() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (Block block : blocks) {
            output.writeBytes(block.getData());
        }
        return output.toByteArray();
    }
}
