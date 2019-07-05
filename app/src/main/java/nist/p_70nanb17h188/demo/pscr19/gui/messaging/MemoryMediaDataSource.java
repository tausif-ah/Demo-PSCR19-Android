package nist.p_70nanb17h188.demo.pscr19.gui.messaging;

import android.media.MediaDataSource;

import java.io.IOException;

public class MemoryMediaDataSource extends MediaDataSource {
    private final byte[] content;

    MemoryMediaDataSource(byte[] content) {
        this.content = content;
    }

    @Override
    public int readAt(long position, byte[] buffer, int offset, int size) {
        int toWrite = content.length - (int) position;
        if (toWrite < 0) toWrite = 0;
        toWrite = Math.min(toWrite, size);
        if (toWrite == 0) return 0;
        System.arraycopy(content, (int) position, buffer, offset, toWrite);
        return toWrite;

    }

    @Override
    public long getSize() {
        return content.length;
    }

    @Override
    public void close() {

    }
}
