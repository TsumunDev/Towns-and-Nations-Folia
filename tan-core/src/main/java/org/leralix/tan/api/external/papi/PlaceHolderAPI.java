package org.leralix.tan.api.external.papi;
import java.util.HashMap;
import java.util.Map;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.api.external.papi.entries.PapiEntry;
import org.leralix.tan.api.external.papi.entries.territory.GetFirstTerritoryIdWithName;
import org.leralix.tan.api.external.papi.entries.territory.TerritoryWithIdExist;
import org.leralix.tan.api.external.papi.entries.territory.TerritoryWithIdLeaderName;
import org.leralix.tan.api.external.papi.entries.territory.TerritoryWithNameExist;
import org.leralix.tan.api.external.papi.entries.territory.TerritoryWithNameLeaderName;
import org.leralix.tan.api.external.papi.entries.player.OtherPlayerChatMode;
import org.leralix.tan.api.external.papi.entries.player.OtherPlayerRegionName;
import org.leralix.tan.api.external.papi.entries.player.OtherPlayerTownName;
import org.leralix.tan.api.external.papi.entries.player.OtherPlayerTownTag;
import org.leralix.tan.api.external.papi.entries.player.PlayerBalance;
import org.leralix.tan.api.external.papi.entries.player.PlayerBiggerOverlordName;
import org.leralix.tan.api.external.papi.entries.player.PlayerChatMode;
import org.leralix.tan.api.external.papi.entries.player.PlayerColoredTownTag;
import org.leralix.tan.api.external.papi.entries.player.PlayerNameHaveTown;
import org.leralix.tan.api.external.papi.entries.player.PlayerNameIsTownLeader;
import org.leralix.tan.api.external.papi.entries.player.PlayerRegionBalance;
import org.leralix.tan.api.external.papi.entries.player.PlayerRegionChunkActualQuantity;
import org.leralix.tan.api.external.papi.entries.player.PlayerRegionName;
import org.leralix.tan.api.external.papi.entries.player.PlayerRegionResidentQuantity;
import org.leralix.tan.api.external.papi.entries.player.PlayerTownBalance;
import org.leralix.tan.api.external.papi.entries.player.PlayerTownChunkActualQuantity;
import org.leralix.tan.api.external.papi.entries.player.PlayerTownChunkMaxQuantity;
import org.leralix.tan.api.external.papi.entries.player.PlayerTownName;
import org.leralix.tan.api.external.papi.entries.player.PlayerTownRankColoredName;
import org.leralix.tan.api.external.papi.entries.player.PlayerTownRankName;
import org.leralix.tan.api.external.papi.entries.player.PlayerTownRemainingQuantity;
import org.leralix.tan.api.external.papi.entries.player.PlayerTownResidentQuantity;
import org.leralix.tan.api.external.papi.entries.player.PlayerTownTag;
public class PlaceHolderAPI extends PlaceholderExpansion {
  static final String PLACEHOLDER_NOT_FOUND = "[TAN] Placeholder not found";
  private final Map<String, PapiEntry> entries;
  @Override
  @NotNull public String getAuthor() {
    return "Leralix";
  }
  @Override
  @NotNull public String getIdentifier() {
    return "tan";
  }
  @Override
  @NotNull public String getVersion() {
    return TownsAndNations.getPlugin().getCurrentVersion().toString();
  }
  @Override
  public boolean persist() {
    return true;
  }
  @Override
  public boolean canRegister() {
    return true;
  }
  public PlaceHolderAPI() {
    entries = new HashMap<>();
    registerEntry(new GetFirstTerritoryIdWithName());
    registerEntry(new OtherPlayerTownName());
    registerEntry(new OtherPlayerTownTag());
    registerEntry(new OtherPlayerRegionName());
    registerEntry(new OtherPlayerChatMode());
    registerEntry(new PlayerBalance());
    registerEntry(new PlayerBiggerOverlordName());
    registerEntry(new PlayerChatMode());
    registerEntry(new PlayerNameHaveTown());
    registerEntry(new PlayerNameIsTownLeader());
    registerEntry(new PlayerRegionBalance());
    registerEntry(new PlayerRegionChunkActualQuantity());
    registerEntry(new PlayerRegionName());
    registerEntry(new PlayerTownBalance());
    registerEntry(new PlayerTownChunkActualQuantity());
    registerEntry(new PlayerTownChunkMaxQuantity());
    registerEntry(new PlayerTownName());
    registerEntry(new PlayerTownRankColoredName());
    registerEntry(new PlayerTownRankName());
    registerEntry(new PlayerTownRemainingQuantity());
    registerEntry(new PlayerTownResidentQuantity());
    registerEntry(new PlayerTownTag());
    registerEntry(new PlayerColoredTownTag());
    registerEntry(new TerritoryWithIdExist());
    registerEntry(new TerritoryWithNameExist());
    registerEntry(new TerritoryWithNameLeaderName());
  }
  void registerEntry(PapiEntry playerBalance) {
    entries.put(playerBalance.getIdentifier(), playerBalance);
  }
  @Override
  public String onRequest(OfflinePlayer player, @NotNull String params) {
    String paramIdentifier = removePlaceholder(params);
    if (entries.containsKey(paramIdentifier)) {
      return entries.get(paramIdentifier).getData(player, params);
    }
    return PLACEHOLDER_NOT_FOUND;
  }
  public String removePlaceholder(String params) {
    return params.replaceAll("\\{[^}]*}", "{}");
  }
}
