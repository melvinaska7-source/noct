package zov.alphadlc.util.party.connection;

import com.google.gson.JsonObject;
import net.minecraft.util.Formatting;
import zov.alphadlc.util.chat.ChatUtil;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class PartyApiClient {

    private static final String API_BASE = "http://localhost:8080/api/party";

    public String createParty(String leader) {
        return post("/create", "leader=" + leader);
    }

    public String invitePlayer(String partyId, String player) {
        return post("/invite", "partyId=" + partyId + "&player=" + player);
    }

    public String joinParty(String partyId, String player) {
        return post("/join", "partyId=" + partyId + "&player=" + player);
    }

    public String leaveParty(String partyId, String player) {
        return post("/leave", "partyId=" + partyId + "&player=" + player);
    }

    public String disbandParty(String partyId) {
        return post("/disband", "partyId=" + partyId);
    }

    public String kickPlayer(String partyId, String player) {
        return post("/kick", "partyId=" + partyId + "&player=" + player);
    }

    public String getPartyMembers(String partyId) {
        return get("/members?partyId=" + partyId);
    }

    // === Статические методы для ModuleStorage ===

    public static void fetchPartyStateAsync() {
        // Заглушка — можно реализовать позже
    }

    public static void fetchInvitesAsync() {
        // Заглушка — можно реализовать позже
    }

    public static void postAsync(String endpoint, JsonObject body, Consumer<JsonObject> callback) {
        new Thread(() -> {
            try {
                String result = new PartyApiClient().post(endpoint, body.toString());
                if (callback != null) {
                    JsonObject response = new JsonObject();
                    response.addProperty("response", result);
                    callback.accept(response);
                }
            } catch (Exception e) {
                ChatUtil.send(Formatting.RED + "Ошибка API (async): " + e.getMessage());
            }
        }).start();
    }

    // === Приватные методы ===

    private String post(String endpoint, String body) {
        try {
            URL url = new URL(API_BASE + endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            return readResponse(conn);
        } catch (IOException e) {
            ChatUtil.send(Formatting.RED + "Ошибка API: " + e.getMessage());
            return null;
        }
    }

    private String get(String endpoint) {
        try {
            URL url = new URL(API_BASE + endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            return readResponse(conn);
        } catch (IOException e) {
            ChatUtil.send(Formatting.RED + "Ошибка API: " + e.getMessage());
            return null;
        }
    }

    private String readResponse(HttpURLConnection conn) throws IOException {
        int code = conn.getResponseCode();
        InputStream stream = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }
}
