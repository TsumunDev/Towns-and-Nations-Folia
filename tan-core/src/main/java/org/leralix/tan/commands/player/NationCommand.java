package org.leralix.tan.commands.player;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.bukkit.entity.Player;
import org.leralix.lib.commands.PlayerSubCommand;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.territory.RegionData;
import org.leralix.tan.lang.Lang;
import org.leralix.tan.lang.LangType;
import org.leralix.tan.storage.stored.NationDataStorage;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.storage.stored.RegionDataStorage;
import org.leralix.tan.utils.FoliaScheduler;
import org.leralix.tan.utils.text.TanChatUtils;
public class NationCommand extends PlayerSubCommand {
  @Override
  public String getName() {
    return "nation";
  }
  @Override
  public String getDescription() {
    return Lang.NATION_COMMAND_DESC.getDefault();
  }
  @Override
  public int getArguments() {
    return 2;
  }
  @Override
  public String getSyntax() {
    return "/ccn nation <create|disband> [name]";
  }
  @Override
  public List<String> getTabCompleteSuggestions(Player player, String lowerCase, String[] args) {
    if (args.length == 1) {
      return Arrays.asList("create", "disband");
    }
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
                    LangType lang = tanPlayer.getLang();
                    if (args.length < 1) {
                      TanChatUtils.message(player, Lang.TOO_MANY_ARGS_ERROR.get(lang));
                      TanChatUtils.message(
                          player, Lang.CORRECT_SYNTAX_INFO.get(lang, getSyntax()));
                      return;
                    }
                    switch (args[0].toLowerCase()) {
                      case "create" -> handleCreate(player, tanPlayer, args, lang);
                      case "disband" -> handleDisband(player, tanPlayer, lang);
                      default -> {
                        TanChatUtils.message(player, Lang.TOO_MANY_ARGS_ERROR.get(lang));
                        TanChatUtils.message(
                            player, Lang.CORRECT_SYNTAX_INFO.get(lang, getSyntax()));
                      }
                    }
                  });
            });
  }
  private void handleCreate(Player player, ITanPlayer tanPlayer, String[] args, LangType lang) {
    if (!TownsAndNations.getPlugin().getConfig().getBoolean("EnableNation", true)) {
      TanChatUtils.message(player, Lang.NATION_NOT_ENABLED.get(lang));
      return;
    }
    if (!player.hasPermission("tan.base.nation.create")) {
      TanChatUtils.message(player, Lang.PLAYER_NO_PERMISSION.get(lang));
      return;
    }
    if (args.length < 2) {
      TanChatUtils.message(player, Lang.NATION_CREATE_MISSING_NAME.get(lang));
      return;
    }
    String nationName = args[1];
    if (!tanPlayer.hasRegion()) {
      TanChatUtils.message(player, Lang.NATION_CREATE_MUST_BE_REGION_LEADER.get(lang));
      return;
    }
    RegionData region = tanPlayer.getRegionSync();
    if (region == null) {
      TanChatUtils.message(player, Lang.NATION_CREATE_MUST_BE_REGION_LEADER.get(lang));
      return;
    }
    if (!region.isLeader(tanPlayer)) {
      TanChatUtils.message(player, Lang.NATION_CREATE_MUST_BE_REGION_LEADER.get(lang));
      return;
    }
    if (region.haveOverlord()) {
      TanChatUtils.message(player, Lang.NATION_CREATE_REGION_ALREADY_HAS_NATION.get(lang));
      return;
    }
    double nationCost = TownsAndNations.getPlugin().getConfig().getDouble("nationCost", 25000);
    if (region.getBalance() < nationCost) {
      TanChatUtils.message(
          player,
          Lang.TERRITORY_NOT_ENOUGH_MONEY.get(
              lang, region.getColoredName(), Double.toString(nationCost - region.getBalance())));
      return;
    }
    if (NationDataStorage.getInstance().isNameUsed(nationName)) {
      TanChatUtils.message(player, Lang.NAME_ALREADY_USED.get(lang));
      return;
    }
    int maxNameSize = TownsAndNations.getPlugin().getConfig().getInt("NationNameSize", 45);
    if (nationName.length() > maxNameSize) {
      TanChatUtils.message(player, Lang.NAME_ALREADY_USED.get(lang));
      return;
    }
    region.removeFromBalance(nationCost);
    NationDataStorage.getInstance()
        .createNewNation(nationName, region, tanPlayer)
        .thenAccept(
            nation -> {
              FoliaScheduler.runTask(
                  TownsAndNations.getPlugin(),
                  () -> {
                    TanChatUtils.message(
                        player,
                        Lang.NATION_CREATED_SUCCESS.get(lang, nation.getName()),
                        org.leralix.lib.data.SoundEnum.GOOD);
                  });
            });
  }
  private void handleDisband(Player player, ITanPlayer tanPlayer, LangType lang) {
    if (!player.hasPermission("tan.base.nation.disband")) {
      TanChatUtils.message(player, Lang.PLAYER_NO_PERMISSION.get(lang));
      return;
    }
    if (!tanPlayer.hasRegion()) {
      TanChatUtils.message(player, Lang.NATION_DISBAND_MUST_BE_NATION_LEADER.get(lang));
      return;
    }
    RegionData region = tanPlayer.getRegionSync();
    if (region == null || !region.haveOverlord()) {
      TanChatUtils.message(player, Lang.NATION_DISBAND_MUST_BE_NATION_LEADER.get(lang));
      return;
    }
    var nationOpt = region.getOverlord();
    if (nationOpt.isEmpty() || !(nationOpt.get() instanceof org.leralix.tan.dataclass.territory.NationData nation)) {
      TanChatUtils.message(player, Lang.NATION_DISBAND_MUST_BE_NATION_LEADER.get(lang));
      return;
    }
    if (!nation.isLeader(tanPlayer)) {
      TanChatUtils.message(player, Lang.NATION_DISBAND_MUST_BE_NATION_LEADER.get(lang));
      return;
    }
    nation.delete()
        .thenRun(
            () -> {
              FoliaScheduler.runTask(
                  TownsAndNations.getPlugin(),
                  () -> {
                    TanChatUtils.message(
                        player,
                        Lang.NATION_DISBANDED_SUCCESS.get(lang),
                        org.leralix.lib.data.SoundEnum.GOOD);
                  });
            });
  }
}
