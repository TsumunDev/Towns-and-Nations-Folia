package org.leralix.tan.commands.player;
import java.util.Collections;
import java.util.List;
import org.bukkit.entity.Player;
import org.leralix.lib.commands.PlayerSubCommand;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.chunk.ClaimedChunk2;
import org.leralix.tan.dataclass.chunk.TownClaimedChunk;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.enums.RolePermission;
import org.leralix.tan.lang.Lang;
import org.leralix.tan.lang.LangType;
import org.leralix.tan.storage.stored.NewClaimedChunkStorage;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.upgrade.rewards.bool.EnableTownSpawn;
import org.leralix.tan.utils.FoliaScheduler;
import org.leralix.tan.utils.text.TanChatUtils;
public class SetTownSpawnCommand extends PlayerSubCommand {
  @Override
  public String getName() {
    return "setspawn";
  }
  @Override
  public String getDescription() {
    return Lang.SET_SPAWN_COMMAND_DESC.getDefault();
  }
  public int getArguments() {
    return 1;
  }
  @Override
  public String getSyntax() {
    return "/ccn setspawn";
  }
  @Override
  public List<String> getTabCompleteSuggestions(Player player, String lowerCase, String[] args) {
    return Collections.emptyList();
  }
  @Override
  public void perform(Player player, String[] args) {
    PlayerDataStorage.getInstance()
        .get(player)
        .thenAccept(
            tanPlayer -> {
              FoliaScheduler.runTask(
                  TownsAndNations.getPlugin(),
                  () -> {
                    LangType langType = tanPlayer.getLang();
                    if (args.length != 1) {
                      TanChatUtils.message(
                          player, Lang.CORRECT_SYNTAX_INFO.get(langType, getSyntax()));
                      return;
                    }
                    if (!tanPlayer.hasTown()) {
                      TanChatUtils.message(player, Lang.PLAYER_NO_TOWN.get(langType));
                      return;
                    }
                    TownData townData = tanPlayer.getTownSync();
                    if (townData == null) {
                      TanChatUtils.message(player, Lang.PLAYER_NO_TOWN.get(langType));
                      return;
                    }
                    if (!townData.doesPlayerHavePermission(
                        tanPlayer, RolePermission.TOWN_ADMINISTRATOR)) {
                      TanChatUtils.message(player, Lang.PLAYER_NO_PERMISSION.get(langType));
                      return;
                    }
                    EnableTownSpawn enableTownSpawn =
                        townData.getNewLevel().getStat(EnableTownSpawn.class);
                    if (!enableTownSpawn.isEnabled()) {
                      TanChatUtils.message(player, Lang.SPAWN_NOT_UNLOCKED.get(langType));
                      return;
                    }
                    ClaimedChunk2 currentChunk =
                        NewClaimedChunkStorage.getInstance().get(player.getLocation().getChunk());
                    if (!(currentChunk instanceof TownClaimedChunk townChunk
                        && townChunk.getTown().equals(townData))) {
                      TanChatUtils.message(player, Lang.SPAWN_NEED_TO_BE_IN_CHUNK.get(langType));
                      return;
                    }
                    townData.setSpawn(player.getLocation());
                    TanChatUtils.message(player, Lang.SPAWN_SET_SUCCESS.get(langType));
                  });
            })
        .exceptionally(
            throwable -> {
              TownsAndNations.getPlugin()
                  .getLogger()
                  .severe("SetTownSpawnCommand failed: " + throwable.getMessage());
              return null;
            });
  }
}