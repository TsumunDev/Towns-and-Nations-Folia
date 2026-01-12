package org.leralix.tan.commands.debug;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.leralix.lib.commands.SubCommand;
import org.leralix.tan.gui.user.territory.quest.QuestListMenu;
import org.leralix.tan.storage.stored.PlayerDataStorage;

import java.util.Collections;
import java.util.List;

/**
 * DEBUG command to test the quest system.
 * Usage: /coconationdebug quest
 */
public class TestQuestCommand extends SubCommand {

    @Override
    public String getName() {
        return "quest";
    }

    @Override
    public String getDescription() {
        return "Opens the quest menu (TEST)";
    }

    @Override
    public int getArguments() {
        return 1;
    }

    @Override
    public String getSyntax() {
        return "/coconationdebug quest";
    }

    @Override
    public List<String> getTabCompleteSuggestions(CommandSender player, String lowerCase, String[] args) {
        return Collections.emptyList();
    }

    @Override
    public void perform(CommandSender commandSender, String[] args) {
        if (!(commandSender instanceof Player player)) {
            commandSender.sendMessage("§cThis command can only be used by players!");
            return;
        }

        if (args.length != 1) {
            commandSender.sendMessage("§cUsage: /coconationdebug quest");
            return;
        }

        PlayerDataStorage.getInstance().get(player)
                .thenAccept(tanPlayer -> {
                    String townId = tanPlayer.getTownId();

                    if (townId == null) {
                        player.sendMessage("§cYou must be in a town to use this command!");
                        return;
                    }

                    player.sendMessage("§aOpening quest menu for town: " + townId);

                    // Open GUI (handles threading internally)
                    QuestListMenu.open(player, townId);
                });
    }
}
