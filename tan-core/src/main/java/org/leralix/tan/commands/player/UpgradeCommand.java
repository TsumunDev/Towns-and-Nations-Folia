package org.leralix.tan.commands.player;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;
import org.leralix.lib.commands.PlayerSubCommand;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.gui.user.territory.upgrade.UpgradeShopMenu;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.storage.stored.TownDataStorage;
import org.leralix.tan.utils.FoliaScheduler;
import org.leralix.tan.utils.text.TanChatUtils;

/**
 * Command to open the town upgrades menu.
 * Usage: /ccn upgrade
 */
public class UpgradeCommand extends PlayerSubCommand {

    @Override
    public String getName() {
        return "upgrade";
    }

    @Override
    public String getDescription() {
        return "Opens the town upgrades menu";
    }

    @Override
    public int getArguments() {
        return 1;
    }

    @Override
    public String getSyntax() {
        return "/ccn upgrade";
    }

    @Override
    public List<String> getTabCompleteSuggestions(Player player, String lowerCase, String[] args) {
        return Collections.emptyList();
    }

    @Override
    public void perform(Player player, String[] args) {
        PlayerDataStorage.getInstance().get(player)
                .thenCompose(tanPlayer -> {
                    String townId = tanPlayer.getTownId();

                    if (townId == null) {
                        FoliaScheduler.runTask(TownsAndNations.getPlugin(), () -> {
                            TanChatUtils.message(player, "§cYou are not in a town!");
                        });
                        return CompletableFuture.completedFuture(null);
                    }

                    return TownDataStorage.getInstance().get(townId);
                })
                .thenAccept(townData -> {
                    if (townData == null) {
                        FoliaScheduler.runTask(TownsAndNations.getPlugin(), () -> {
                            TanChatUtils.message(player, "§cTown not found!");
                        });
                        return;
                    }

                    // Open GUI (handles threading internally)
                    UpgradeShopMenu.open(player, townData);
                });
    }
}
