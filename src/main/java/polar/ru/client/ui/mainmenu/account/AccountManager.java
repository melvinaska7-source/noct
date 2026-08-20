package polar.ru.client.ui.mainmenu.account;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.session.Session;
import polar.ru.api.QClient;
import polar.ru.client.ui.mainmenu.account.Account;
import polar.ru.client.ui.mainmenu.account.AccountFile;
import polar.ru.mixin.IMinecraftClientAccessor;
import polar.ru.polar;

public final class AccountManager
extends CopyOnWriteArrayList<Account>
implements QClient {
    private final AccountFile accountFile;
    private String lastSelected = null;

    public AccountManager() {
        File directory = polar.INSTANCE.globalsDir == null ? new File("C:\\polar", "polar") : polar.INSTANCE.globalsDir;
        this.accountFile = new AccountFile(new File(directory, "accounts.json"));
        this.accountFile.read(this);
        this.registerShutdownHook();
    }

    private void registerShutdownHook() {
        Thread hook = new Thread(() -> {
            try {
                if (this.lastSelected != null) {
                    this.accountFile.writeLastSelected(this, this.lastSelected);
                } else {
                    this.accountFile.write(this);
                }
            }
            catch (Exception e2) {
                e2.printStackTrace(System.err);
            }
        }, "polar-Accounts-Shutdown");
        hook.setDaemon(false);
        Runtime.getRuntime().addShutdownHook(hook);
    }

    public void saveLastSelected(String name) {
        this.lastSelected = name;
        this.accountFile.writeLastSelected(this, name);
    }

    public void restoreLastSession() {
        String last = this.accountFile.getLast();
        if (last == null || last.isEmpty()) {
            return;
        }
        this.getAccount(last).ifPresent(account -> {
            try {
                Constructor constructor = Session.class.getDeclaredConstructor(String.class, UUID.class, String.class, Optional.class, Optional.class, Session.AccountType.class);
                constructor.setAccessible(true);
                Session session = (Session)constructor.newInstance(account.name(), UUID.nameUUIDFromBytes(("OfflinePlayer:" + account.name()).getBytes()), mc.getSession() == null ? "" : mc.getSession().getAccessToken(), Optional.empty(), Optional.empty(), Session.AccountType.MOJANG);
                ((IMinecraftClientAccessor)mc).setSession(session);
            }
            catch (Exception exception) {
                // empty catch block
            }
        });
    }

    public AccountFile file() {
        return this.accountFile;
    }

    public void save() {
        this.accountFile.write(this);
    }

    public void addAccount(Account account) {
        if (account == null || this.isAccount(account.name())) {
            return;
        }
        this.add(account);
        this.save();
    }

    public Optional<Account> getAccount(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return this.stream().filter(account -> account.name().equalsIgnoreCase(name)).findFirst();
    }

    public boolean isAccount(String name) {
        return this.getAccount(name).isPresent();
    }

    public void removeAccount(String name) {
        if (name == null) {
            return;
        }
        this.removeIf(account -> account.name().equalsIgnoreCase(name));
        this.save();
    }

    public void clearAccounts() {
        this.clear();
        this.save();
    }

    public List<Account> getFavoriteAccountsSorted() {
        return this.stream().filter(Account::favorite).toList();
    }
}

