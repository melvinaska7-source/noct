package zov.alphadlc.util.party;

import java.util.ArrayList;
import java.util.List;

public class PartyManager {
    private String currentParty = null;
    private final List<String> members = new ArrayList<>();

    public void createParty(String leader) {
        currentParty = leader;
        members.clear();
        members.add(leader);
    }

    public void invitePlayer(String player) {
        // TODO: реализовать логику приглашения
    }

    public void joinParty(String party) {
        currentParty = party;
    }

    public void leaveParty() {
        currentParty = null;
        members.clear();
    }

    public void disbandParty() {
        currentParty = null;
        members.clear();
    }

    public void kickPlayer(String player) {
        members.remove(player);
    }

    public List<String> getMembers() {
        return new ArrayList<>(members);
    }

    public String getCurrentParty() {
        return currentParty;
    }
}
