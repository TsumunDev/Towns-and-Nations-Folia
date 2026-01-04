package org.leralix.tan.commands.player;

import org.leralix.tan.TownsAndNations;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.leralix.lib.commands.PlayerSubCommand;
import org.leralix.lib.data.SoundEnum;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.lang.Lang;
import org.leralix.tan.lang.LangType;
import org.leralix.tan.storage.invitation.TownInviteDataStorage;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.storage.stored.TownDataStorage;
import org.leralix.tan.utils.commands.CommandExceptionHandler;
import org.leralix.tan.utils.text.TanChatUtils;

public class JoinTownCommand extends PlayerSubCommand {
  @Override
  public String getName() {
    return "join";
  }

  @Override
  public String getDescription() {
    return Lang.TOWN_ACCEPT_INVITE_DESC.getDefault();
  }

  public int getArguments() {
    return 2;
  }

  @Override
  public String getSyntax() {
    return "/tan join <Town ID>";
  }

  @Override
  public List<String> getTabCompleteSuggestions(Player player, String lowerCase, String[] args) {
    List<String> suggestions = new ArrayList<>();
    if (args.length == 2) {
      suggestions.add("<Town ID>");
    }
    return suggestions;
  }

  @Override
  public void perform(Player player, String[] args) {
    // Validate argument count
    if (!CommandExceptionHandler.validateArgCount((CommandSender) player, args, 2, getSyntax())) {
      return;
    }

    // Async pattern: load player data without blocking
    PlayerDataStorage.getInstance()
        .get(player)
        .thenCompose(
            tanPlayer -> {
              LangType lang = tanPlayer.getLang();

              if (!player.hasPermission("tan.base.town.join")) {
                TanChatUtils.message(
                    player, Lang.PLAYER_NO_PERMISSION.get(lang), SoundEnum.NOT_ALLOWED);
                return CompletableFuture.completedFuture(null);
              }

              String townID = args[1];

              if (!TownInviteDataStorage.isInvited(player.getUniqueId().toString(), townID)) {
                TanChatUtils.message(player, Lang.TOWN_INVITATION_NO_INVITATION.get(lang));
                return CompletableFuture.completedFuture(null);
              }

<<<<<<< Updated upstream
    TownData townData = TownDataStorage.getInstance().getSync(townID);
    if (townData == null) {
      TanChatUtils.message(
          player, Lang.TOWN_NOT_FOUND.get(lang)); // Assuming a new Lang entry for town not found
      return;
    }
=======
              // Chain second async call for town data
              return TownDataStorage.getInstance()
                  .get(townID)
                  .thenApply(
                      townData -> {
                        if (townData == null) {
                          TanChatUtils.message(player, Lang.TOWN_NOT_FOUND.get(lang));
                          return null;
                        }
>>>>>>> Stashed changes

                        if (townData.isFull()) {
                          TanChatUtils.message(player, Lang.INVITATION_TOWN_FULL.get(lang));
                          return null;
                        }

                        townData.addPlayer(tanPlayer);
                        return townData;
                      });
            })
        .exceptionally(
            throwable -> {
              TownsAndNations.getPlugin().getLogger().severe("JoinTownCommand failed: " + throwable.getMessage());
              player.sendMessage("§cError processing jointown command");
              return null;
            });
  }
}
