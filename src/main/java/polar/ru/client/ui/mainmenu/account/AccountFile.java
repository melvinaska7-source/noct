package polar.ru.client.ui.mainmenu.account;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import polar.ru.api.QClient;
import polar.ru.client.ui.mainmenu.account.Account;
import polar.ru.client.ui.mainmenu.account.AccountManager;

public record AccountFile(File file) implements QClient
{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public AccountFile {
        Objects.requireNonNull(file, "file");
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public boolean read(AccountManager accounts) {
        if (!this.file.exists()) {
            return false;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(this.file));){
            JsonArray array;
            JsonObject jsonObject = (JsonObject)GSON.fromJson((Reader)reader, JsonObject.class);
            if (jsonObject == null) {
                boolean bl = false;
                return bl;
            }
            JsonArray jsonArray = array = jsonObject.has("accounts") ? jsonObject.getAsJsonArray("accounts") : null;
            if (array != null) {
                for (JsonElement element : array) {
                    try {
                        JsonObject accountObject = element.getAsJsonObject();
                        if (!accountObject.has("name") || !accountObject.has("creationDate")) continue;
                        String name = accountObject.get("name").getAsString();
                        LocalDateTime creationDate = LocalDateTime.parse(accountObject.get("creationDate").getAsString());
                        boolean favorite = accountObject.has("favorite") && accountObject.get("favorite").getAsBoolean();
                        Account account = new Account(creationDate, name);
                        account.favorite(favorite);
                        if (accounts.isAccount(name)) continue;
                        accounts.add(account);
                    }
                    catch (IllegalStateException | NullPointerException | DateTimeParseException runtimeException) {}
                }
            }
            boolean bl = true;
            return bl;
        }
        catch (IOException | RuntimeException ignored) {
            return false;
        }
    }

    public boolean write(AccountManager accounts) {
        return this.writeJson(this.buildJson(accounts, mc.getSession() == null ? "" : mc.getSession().getUsername()));
    }

    public boolean writeLastSelected(AccountManager accounts, String lastName) {
        return this.writeJson(this.buildJson(accounts, lastName == null ? "" : lastName));
    }

    private boolean writeJson(JsonObject json) {
        File parent = this.file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            return false;
        }
        try (FileWriter writer = new FileWriter(this.file)) {
            GSON.toJson((JsonElement)json, (Appendable)writer);
            return true;
        }
        catch (IOException ignored) {
            return false;
        }
    }

    private JsonObject buildJson(AccountManager accounts, String lastName) {
        JsonObject json = new JsonObject();
        JsonArray array = new JsonArray();
        for (Account account : accounts) {
            JsonObject accountObject = new JsonObject();
            accountObject.addProperty("name", account.name());
            accountObject.addProperty("creationDate", account.creationDate().toString());
            accountObject.addProperty("favorite", Boolean.valueOf(account.favorite()));
            array.add((JsonElement)accountObject);
        }
        json.add("accounts", (JsonElement)array);
        json.addProperty("last", lastName);
        return json;
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public String getLast() {
        if (!this.file.exists()) {
            return "";
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(this.file));){
            JsonObject jsonObject = (JsonObject)GSON.fromJson((Reader)reader, JsonObject.class);
            if (jsonObject == null || !jsonObject.has("last")) {
                String string = "";
                return string;
            }
            String string = jsonObject.get("last").getAsString();
            return string;
        }
        catch (Exception ignored) {
            return "";
        }
    }
}

