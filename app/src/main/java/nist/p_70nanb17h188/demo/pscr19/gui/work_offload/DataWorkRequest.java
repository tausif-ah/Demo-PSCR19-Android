package nist.p_70nanb17h188.demo.pscr19.gui.work_offload;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.nio.ByteBuffer;

import nist.p_70nanb17h188.demo.pscr19.Helper;

class DataWorkRequest {
    private static final byte TYPE_WORK_REQUEST = 1;
    private final int workId;

    DataWorkRequest(int workId) {
        this.workId = workId;
    }

    int getWorkId() {
        return workId;
    }

    @NonNull
    byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(Helper.INTEGER_SIZE + 1);
        buffer.put(TYPE_WORK_REQUEST);
        buffer.putInt(workId);
        return buffer.array();
    }

    @Nullable
    static DataWorkRequest fromBytes(@NonNull byte[] bytes) {
        if (bytes.length != Helper.INTEGER_SIZE + 1) return null;
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        if (buffer.get() != TYPE_WORK_REQUEST) return null;
        int workId = buffer.getInt();
        return new DataWorkRequest(workId);
    }
}
