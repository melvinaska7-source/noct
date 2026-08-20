package polar.ru.api.utils.rpc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

final class DiscordIpcClient
implements Closeable {
    private static final int OP_HANDSHAKE = 0;
    private static final int OP_FRAME = 1;
    private static final int OP_CLOSE = 2;
    private static final int OP_PING = 3;
    private static final int OP_PONG = 4;
    private static final int IPC_VERSION = 1;
    private static final int MAX_PIPES = 10;
    private static final String[] PIPE_PATH_FORMATS = new String[]{"\\\\?\\pipe\\discord-ipc-%d", "\\\\.\\pipe\\discord-ipc-%d"};
    private final String clientId;
    private final long processId;
    private RandomAccessFile pipe;

    DiscordIpcClient(String clientId) {
        this.clientId = clientId;
        this.processId = ProcessHandle.current().pid();
    }

    boolean isConnected() {
        return this.pipe != null;
    }

    void connect() throws IOException {
        IOException lastException = null;
        for (String pathFormat : PIPE_PATH_FORMATS) {
            for (int i2 = 0; i2 < 10; ++i2) {
                RandomAccessFile candidate = null;
                try {
                    this.pipe = candidate = new RandomAccessFile(String.format(pathFormat, i2), "rw");
                    this.handshake();
                    return;
                }
                catch (IOException exception) {
                    lastException = exception;
                    this.pipe = null;
                    if (candidate == null) continue;
                    candidate.close();
                    continue;
                }
            }
        }
        throw new IOException("Discord desktop client is not available.", lastException);
    }

    void setActivity(Activity activity) throws IOException {
        this.ensureConnected();
        JsonObject payload = new JsonObject();
        JsonObject args = new JsonObject();
        payload.addProperty("cmd", "SET_ACTIVITY");
        payload.addProperty("nonce", UUID.randomUUID().toString());
        args.addProperty("pid", (Number)this.processId);
        args.add("activity", (JsonElement)activity.toJson());
        payload.add("args", (JsonElement)args);
        this.writeJson(1, payload);
        JsonObject response = this.readJsonFrame();
        this.ensureNoError(response);
    }

    private void handshake() throws IOException {
        JsonObject payload = new JsonObject();
        payload.addProperty("v", (Number)1);
        payload.addProperty("client_id", this.clientId);
        this.writeJson(0, payload);
        JsonObject response = this.readJsonFrame();
        this.ensureNoError(response);
    }

    private void ensureConnected() throws IOException {
        if (!this.isConnected()) {
            throw new IOException("Discord IPC connection is closed.");
        }
    }

    private void writeJson(int opcode, JsonObject payload) throws IOException {
        this.writeFrame(opcode, payload.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void writeFrame(int opcode, byte[] payload) throws IOException {
        this.ensureConnected();
        ByteBuffer header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        header.putInt(opcode);
        header.putInt(payload.length);
        this.pipe.write(header.array());
        this.pipe.write(payload);
    }

    private JsonObject readJsonFrame() throws IOException {
        byte[] payload;
        int opcode;
        while (true) {
            ByteBuffer header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
            this.pipe.readFully(header.array());
            opcode = header.getInt(0);
            int length = header.getInt(4);
            payload = new byte[length];
            this.pipe.readFully(payload);
            if (opcode != 3) break;
            this.writeFrame(4, payload);
        }
        if (opcode == 2) {
            throw new EOFException("Discord IPC closed the connection.");
        }
        if (opcode != 1) {
            throw new IOException("Unexpected Discord IPC opcode: " + opcode);
        }
        return JsonParser.parseString((String)new String(payload, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private void ensureNoError(JsonObject payload) throws IOException {
        if (!payload.has("evt") || payload.get("evt").isJsonNull()) {
            return;
        }
        String event = payload.get("evt").getAsString();
        if (!"ERROR".equals(event)) {
            return;
        }
        JsonObject data = payload.has("data") && payload.get("data").isJsonObject() ? payload.getAsJsonObject("data") : new JsonObject();
        int code = data.has("code") ? data.get("code").getAsInt() : -1;
        String message = data.has("message") ? data.get("message").getAsString() : "Unknown Discord RPC error";
        throw new IOException("Discord RPC error " + code + ": " + message);
    }

    @Override
    public void close() throws IOException {
        if (this.pipe != null) {
            this.pipe.close();
            this.pipe = null;
        }
    }

    static final class Activity {
        private final String details;
        private final String state;
        private final long startTimestamp;
        private final String detailsUrl;
        private final String stateUrl;

        Activity(String details, String state, long startTimestamp, String detailsUrl, String stateUrl) {
            this.details = details;
            this.state = state;
            this.startTimestamp = startTimestamp;
            this.detailsUrl = detailsUrl;
            this.stateUrl = stateUrl;
        }

        JsonObject toJson() {
            JsonObject activity = new JsonObject();
            JsonObject timestamps = new JsonObject();
            activity.addProperty("type", (Number)0);
            activity.addProperty("details", this.details);
            activity.addProperty("state", this.state);
            activity.addProperty("instance", Boolean.valueOf(true));
            if (this.detailsUrl != null && !this.detailsUrl.isEmpty()) {
                activity.addProperty("details_url", this.detailsUrl);
            }
            if (this.stateUrl != null && !this.stateUrl.isEmpty()) {
                activity.addProperty("state_url", this.stateUrl);
            }
            if (this.startTimestamp > 0L) {
                timestamps.addProperty("start", (Number)this.startTimestamp);
                activity.add("timestamps", (JsonElement)timestamps);
            }
            return activity;
        }
    }
}

