package net.windit.bililive;

import okio.ByteString;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.zip.InflaterInputStream;

public class LivePacket {

    public final static int SEQUENCE_ID = 1;

    // 协议版本
    public final static short PROTOCOL_VERSION_RAW_JSON = 0;
    public final static short PROTOCOL_VERSION_HEARTBEAT = 1;
    public final static short PROTOCOL_VERSION_ZLIB_JSON = 2;

    // 操作类型
    public final static int OPERATION_HEARTBEAT = 2;
    public final static int OPERATION_HEARTBEAT_RESPONSE = 3;
    public final static int OPERATION_MESSAGE = 5;
    public final static int OPERATION_ENTER_ROOM = 7;
    public final static int OPERATION_ENTER_ROOM_RESPONSE = 8;

    // 心跳包内容是固定的,其body为[object Object]
    public final static ByteString HEARTBEAT_PACKET_RAW = ByteString.decodeBase64("AAAAHwAQAAEAAAACAAAAAVtvYmplY3QgT2JqZWN0XQ==");
    private final short headerLen = 16;
    private int packetLen;
    private short protoVer;
    private int operation;
    private int seqid;
    private byte[] body;

    public LivePacket(short protoVer, int operation, int seqid, byte[] body) {
        if (body != null) {
            this.packetLen = headerLen + body.length;
            this.body = body.clone();
        } else {
            this.packetLen = headerLen;
        }
        this.protoVer = protoVer;
        this.operation = operation;
        this.seqid = seqid;
    }

    public LivePacket() {
        packetLen = headerLen;
    }

    public static LivePacket decode(ByteString byteString) {
        return decode(byteString.asByteBuffer());
    }

    public static LivePacket decode(byte[] bytes) {
        return decode(ByteBuffer.wrap(bytes));
    }

    public static LivePacket decode(ByteBuffer buf) {
        int packetLen = buf.getInt();
        short headerLen = buf.getShort();
        byte[] body = new byte[packetLen - headerLen];
        LivePacket packet = new LivePacket(buf.getShort(), buf.getInt(), buf.getInt(), null);
        buf.get(body);
        if (packet.protoVer == PROTOCOL_VERSION_ZLIB_JSON) {
            // 压缩内容, 需要解压
            body = decompress(body);
        }
        packet.setBody(body);
        return packet;
    }

    public static byte[] decompress(byte[] data) {
        InflaterInputStream in = new InflaterInputStream(new ByteArrayInputStream(data));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        byte[] result;
        int length;
        try {
            while ((length = in.read(buffer)) != -1) {
                out.write(buffer, 0, length);
            }
            result = out.toByteArray();
        } catch (IOException e) {
            result = data;
        }
        return result;
    }

    public int getPacketLen() {
        return packetLen;
    }

    public short getHeaderLen() {
        return headerLen;
    }

    public short getProtoVer() {
        return protoVer;
    }

    public void setProtoVer(short protoVer) {
        this.protoVer = protoVer;
    }

    public int getOperation() {
        return operation;
    }

    public void setOperation(int operation) {
        this.operation = operation;
    }

    public int getSeqid() {
        return seqid;
    }

    public void setSeqid(int seqid) {
        this.seqid = seqid;
    }

    public byte[] getBody() {
        return body.clone();
    }

    public void setBody(byte[] body) {
        this.body = body.clone();
        this.packetLen = headerLen + body.length;
    }

    public List<String> getDecodedJSON() {
        List<String> jsons = new ArrayList<>();
        // 如果此包不是消息包, 或者此包是信息包但不含压缩过的 json 消息,
        // 则将此包的数据字段当单条 json 消息处理
        if (operation != OPERATION_MESSAGE || protoVer == PROTOCOL_VERSION_RAW_JSON) {
            jsons.add(new String(body, StandardCharsets.UTF_8));
            return jsons;
        }
        // 一个信息包中可能有多条 json, 需要切割
        int offest = 0;
        while (offest < body.length) {
            // 单条 json 的头部, 包含 json 消息的长度信息
            byte[] singleJsonHeader = new byte[headerLen];
            System.arraycopy(body, offest, singleJsonHeader, 0, headerLen);
            ByteBuffer buf = ByteBuffer.wrap(singleJsonHeader);
            int jsonMsgLen = buf.getInt();
            byte[] jsonMsg = new byte[jsonMsgLen - headerLen];
            System.arraycopy(body, offest + headerLen, jsonMsg, 0, jsonMsg.length);
            jsons.add(new String(jsonMsg, StandardCharsets.UTF_8));
            offest += jsonMsgLen;
        }
        return jsons;
    }

    public ByteString encode() {
        ByteBuffer buf = ByteBuffer.allocate(packetLen);
        buf.putInt(packetLen).putShort(headerLen).putShort(protoVer).putInt(operation).putInt(seqid);
        if (this.body != null) {
            buf.put(this.body);
        }
        return ByteString.of(buf.array());
    }

    @Override
    public String toString() {
        return "LivePacket{" +
                "packetLen=" + packetLen +
                ", headerLen=" + headerLen +
                ", protoVer=" + protoVer +
                ", operation=" + operation +
                ", seqid=" + seqid +
                ", bodyStr=" + new String(body, StandardCharsets.UTF_8) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LivePacket packet = (LivePacket) o;
        return packetLen == packet.packetLen &&
                protoVer == packet.protoVer &&
                operation == packet.operation &&
                seqid == packet.seqid &&
                Arrays.equals(body, packet.body);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(headerLen, packetLen, protoVer, operation, seqid);
        result = 31 * result + Arrays.hashCode(body);
        return result;
    }

    public static class EnterRoom {
        public int uid = 0;
        public int roomid;
        public int protover = 2;
        public String platform = "web";
        public String clientver = "2.6.17";
        public int type = 2;
        public String key;
    }
}
