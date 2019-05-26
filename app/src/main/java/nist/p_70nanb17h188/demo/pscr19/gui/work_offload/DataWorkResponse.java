package nist.p_70nanb17h188.demo.pscr19.gui.work_offload;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.nio.ByteBuffer;

import nist.p_70nanb17h188.demo.pscr19.Helper;

class DataWorkResponse {
    private static final byte TYPE_WORK_RESPONSE = 2;
    private final int workId;

    DataWorkResponse(int workId) {
        this.workId = workId;
    }

    int getWorkId() {
        return workId;
    }

    @NonNull
    byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(Helper.INTEGER_SIZE + 1);
        buffer.put(TYPE_WORK_RESPONSE);
        buffer.putInt(workId);
        return buffer.array();
    }

    @Nullable
    static DataWorkResponse fromBytes(@NonNull byte[] bytes) {
        if (bytes.length != Helper.INTEGER_SIZE + 1) return null;
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        if (buffer.get() != TYPE_WORK_RESPONSE) return null;
        int workId = buffer.getInt();
        return new DataWorkResponse(workId);
    }
}
