package zov.alphadlc.util.commands.defaults;

import net.minecraft.util.Formatting;
import zov.alphadlc.util.chat.ChatUtil;
import zov.alphadlc.util.commands.Command;
import zov.alphadlc.util.party.PartyManager;

public class PartyCommand extends Command {

    private final PartyManager partyManager = new PartyManager();

    public PartyCommand() {
        super("party", ".party <create|invite|join|leave|disband|kick|list>", "Управление пати");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 1) {
            ChatUtil.send("§cИспользование: §f" + getSyntax());
            return;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> {
                partyManager.createParty(mc.player.getNameForScoreboard());
                ChatUtil.send(Formatting.GREEN + "Пати создано!");
            }
            case "invite" -> {
                if (args.length < 2) {
                    ChatUtil.send("§cУкажите игрока: §f.party invite <ник>");
                    return;
                }
                String target = args[1];
                partyManager.invitePlayer(target);
                ChatUtil.send(Formatting.GRAY + "Вы пригласили игрока " + Formatting.WHITE + target);
            }
            case "join" -> {
                if (args.length < 2) {
                    ChatUtil.send("§cУкажите лидера пати: §f.party join <ник>");
                    return;
                }
                String party = args[1];
                partyManager.joinParty(party);
                ChatUtil.send(Formatting.GRAY + "Вы вошли в пати " + Formatting.WHITE + party);
            }
            case "leave" -> {
                partyManager.leaveParty();
                ChatUtil.send(Formatting.GRAY + "Вы покинули пати");
            }
            case "disband" -> {
                partyManager.disbandParty();
                ChatUtil.send(Formatting.GRAY + "Вы распустили пати");
            }
            case "kick" -> {
                if (args.length < 2) {
                    ChatUtil.send("§cУкажите игрока: §f.party kick <ник>");
                    return;
                }
                String target = args[1];
                partyManager.kickPlayer(target);
                ChatUtil.send(Formatting.GRAY + "Вы кикнули игрока " + Formatting.WHITE + target);
            }
            case "list" -> {
                ChatUtil.send(Formatting.GRAY + "Игроки в пати:");
                for (String m : partyManager.getMembers()) {
                    ChatUtil.send(" " + Formatting.WHITE + m);
                }
            }
            default -> ChatUtil.send("§cНеизвестная команда. Использование: §f" + getSyntax());
        }
    }
}
