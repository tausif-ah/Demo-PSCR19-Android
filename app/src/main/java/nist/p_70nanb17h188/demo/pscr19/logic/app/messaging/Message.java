package nist.p_70nanb17h188.demo.pscr19.logic.app.messaging;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.nio.ByteBuffer;

import nist.p_70nanb17h188.demo.pscr19.Helper;
import nist.p_70nanb17h188.demo.pscr19.logic.net.Name;

public class Message {

    public enum MessageType {
        MSG((byte) 0), PNT((byte) 1);
        private final byte representation;

        MessageType(byte representation) {
            this.representation = representation;
        }

        public byte getRepresentation() {
            return representation;
        }

        @Nullable
        public static MessageType getType(byte representation) {
            switch (representation) {
                case 0:
                    return MSG;
                case 1:
                    return PNT;
                default:
                    return null;
            }
        }
    }

    private final long sendTime;
    private final Name senderGroup, receiverGroup;
    private final String senderName;
    private final Name[] carriedNames;
    private final MessageType type;
    private final String mime;
    private final String content;
    private final int writeSize;

    private static int calculateWriteSize(@NonNull String senderName, int carriedNamesLength, @NonNull String mime, @NonNull String content) {
        return Helper.LONG_SIZE // sendTime
                + Helper.INTEGER_SIZE
                + Name.WRITE_SIZE * (2 + carriedNamesLength) // senderGroup, receiverGroup, carriedNames
                + 1 // type
                + Helper.getStringWriteSize(senderName)
                + Helper.getStringWriteSize(mime)
                + Helper.getStringWriteSize(content);
    }

    public Message(long sendTime,
                   @NonNull Name senderGroup, @NonNull Name receiverGroup,
                   @NonNull String senderName,
                   @NonNull Name[] carriedNames,
                   @NonNull MessageType type,
                   @NonNull String mime, @NonNull String content) {
        this(sendTime, senderGroup, receiverGroup, senderName, carriedNames, type, mime, content,
                calculateWriteSize(senderName, carriedNames.length, mime, content));
    }

    private Message(long sendTime,
                    @NonNull Name senderGroup, @NonNull Name receiverGroup,
                    @NonNull String senderName,
                    @NonNull Name[] carriedNames,
                    @NonNull MessageType type,
                    @NonNull String mime, @NonNull String content,
                    int writeSize) {
        this.sendTime = sendTime;
        this.senderGroup = senderGroup;
        this.receiverGroup = receiverGroup;
        this.senderName = senderName;
        this.carriedNames = carriedNames;
        this.type = type;
        this.mime = mime;
        this.content = content;
        this.writeSize = writeSize;
    }

    public void write(ByteBuffer buffer) {
        buffer.putLong(sendTime);
        senderGroup.write(buffer);
        receiverGroup.write(buffer);
        Helper.writeString(buffer, senderName);
        buffer.putInt(carriedNames.length);
        for (Name carriedName : carriedNames)
            carriedName.write(buffer);
        buffer.put(type.representation);
        Helper.writeString(buffer, mime);
        Helper.writeString(buffer, content);
    }

    public static Message read(ByteBuffer buffer) {
        int startPosition = buffer.position();
        // sendTime
        if (buffer.remaining() < Helper.LONG_SIZE) return null;
        long sendTime = buffer.getLong();
        // senderGroup
        Name senderGroup = Name.read(buffer);
        if (senderGroup == null) return null;
        // receiverGroup
        Name receiverGroup = Name.read(buffer);
        if (receiverGroup == null) return null;
        // senderName
        String senderName = Helper.readString(buffer);
        if (senderName == null) return null;
        // carried names
        if (buffer.remaining() < Helper.INTEGER_SIZE) return null;
        int carriedNamesSize = buffer.getInt();
        if (buffer.remaining() < Name.WRITE_SIZE * carriedNamesSize) return null;
        Name[] carriedNames = new Name[carriedNamesSize];
        for (int i = 0; i < carriedNamesSize; i++) {
            carriedNames[i] = Name.read(buffer);
            if (carriedNames[i] == null) return null;
        }
        // type
        byte bType = buffer.get();
        MessageType type = MessageType.getType(bType);
        if (type == null) return null;
        // mime
        String mime = Helper.readString(buffer);
        if (mime == null) return null;
        // content
        String content = Helper.readString(buffer);
        if (content == null) return null;
        int writeSize = buffer.position() - startPosition;

        return new Message(sendTime, senderGroup, receiverGroup, senderName, carriedNames, type, mime, content, writeSize);
    }


    public long getSendTime() {
        return sendTime;
    }

    public Name getSenderGroup() {
        return senderGroup;
    }

    public Name getReceiverGroup() {
        return receiverGroup;
    }

    public String getSenderName() {
        return senderName;
    }

    public Name[] getCarriedNames() {
        return carriedNames;
    }

    public MessageType getType() {
        return type;
    }

    public String getMime() {
        return mime;
    }

    public String getContent() {
        return content;
    }

    public int getWriteSize() {
        return writeSize;
    }

}
