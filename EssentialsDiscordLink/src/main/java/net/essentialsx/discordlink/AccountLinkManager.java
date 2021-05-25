package net.essentialsx.discordlink;

import com.earth2me.essentials.IEssentialsModule;
import net.ess3.api.IUser;

import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import static com.earth2me.essentials.I18n.tl;

public class AccountLinkManager implements IEssentialsModule {
    private static final char[] CODE_CHARACTERS = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();

    private final EssentialsDiscordLink ess;
    private final AccountStorage storage;

    private final Map<String, UUID> codeToUuidMap = new ConcurrentHashMap<>();

    public AccountLinkManager(EssentialsDiscordLink ess, AccountStorage storage) {
        this.ess = ess;
        this.storage = storage;
    }

    public boolean isLinked(final UUID uuid) {
        return getDiscordId(uuid) != null;
    }

    public boolean isLinked(final String discordId) {
        return getUUID(discordId) != null;
    }

    public String createCode(final UUID uuid) throws IllegalArgumentException {
        final Optional<Map.Entry<String, UUID>> prevCode = codeToUuidMap.entrySet().stream().filter(stringUUIDEntry -> stringUUIDEntry.getValue().equals(uuid)).findFirst();
        if (prevCode.isPresent()) {
            throw new IllegalArgumentException(tl("discordLinkPending", "/link " + prevCode.get().getKey()));
        }

        final String code = generateCode();

        // This isn't a race condition as MC commands are executed on a single thread. If that changes, so should this.
        codeToUuidMap.put(code, uuid);
        return code;
    }

    public UUID getPendingUUID(final String code) {
        return codeToUuidMap.remove(code);
    }

    public String getDiscordId(final UUID uuid) {
        return storage.getDiscordId(uuid);
    }

    public IUser getUser(final String discordId) {
        final UUID uuid = getUUID(discordId);
        if (uuid == null) {
            return null;
        }
        return ess.getEss().getUser(uuid);
    }

    public UUID getUUID(final String discordId) {
        return storage.getUUID(discordId);
    }

    public boolean removeAccount(final String discordId) {
        return storage.remove(discordId);
    }

    public boolean removeAccount(final UUID uuid) {
        return storage.remove(uuid);
    }

    public void registerAccount(final UUID uuid, final String discordId) {
        storage.add(uuid, discordId);
    }

    private String generateCode() {
        final char[] code = new char[8];
        final Random random = ThreadLocalRandom.current();

        for (int i = 0; i < 8; i++) {
            code[i] = CODE_CHARACTERS[random.nextInt(CODE_CHARACTERS.length)];
        }
        final String result = new String(code);

        if (codeToUuidMap.containsKey(result)) {
            // If this happens, buy a lottery ticket.
            return generateCode();
        }
        return result;
    }
}
