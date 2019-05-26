package nist.p_70nanb17h188.demo.pscr19.gui.work_offload;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.nio.ByteBuffer;

import nist.p_70nanb17h188.demo.pscr19.Helper;

class DataWorkResult {
    private static final byte TYPE_WORK_RESULT = 4;
    private final int workId;
    @NonNull
    private final byte[] data;

    DataWorkResult(int workId, @NonNull byte[] data) {
        this.workId = workId;
        this.data = data;
    }

    int getWorkId() {
        return workId;
    }

    @NonNull
    byte[] getData() {
        return data;
    }

    @NonNull
    byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(Helper.INTEGER_SIZE * 2 + 1 + data.length);
        buffer.put(TYPE_WORK_RESULT);
        buffer.putInt(workId);
        buffer.putInt(data.length);
        buffer.put(data);
        return buffer.array();
    }

    @Nullable
    static DataWorkResult fromBytes(@NonNull byte[] data) {
        if (data.length < Helper.INTEGER_SIZE * 2 + 1) return null;
        ByteBuffer buffer = ByteBuffer.wrap(data);
        if (buffer.get() != TYPE_WORK_RESULT) return null;
        int workId = buffer.getInt();
        int length = buffer.getInt();
        if (length != buffer.remaining()) return null;
        byte[] content = new byte[length];
        buffer.get(content);
        return new DataWorkResult(workId, content);
    }
}
