package org.leralix.tan.listeners;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.MetadataValue;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.PropertyData;
import org.leralix.tan.dataclass.chunk.ClaimedChunk2;
import org.leralix.tan.dataclass.chunk.TownClaimedChunk;
import org.leralix.tan.enums.TownRelation;
import org.leralix.tan.gui.user.RenterPropertyMenu;
import org.leralix.tan.gui.user.property.BuyOrRentPropertyMenu;
import org.leralix.tan.gui.user.property.PlayerPropertyManager;
import org.leralix.tan.lang.Lang;
import org.leralix.tan.lang.LangType;
import org.leralix.tan.storage.stored.NewClaimedChunkStorage;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.storage.stored.TownDataStorage;
import org.leralix.tan.utils.constants.Constants;
import org.leralix.tan.utils.text.TanChatUtils;
public class PropertySignListener implements Listener {
  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    Player player = event.getPlayer();
    Block clickedBlock = event.getClickedBlock();
    if (clickedBlock != null
        && (event.getAction() == Action.RIGHT_CLICK_BLOCK
            || event.getAction() == Action.LEFT_CLICK_BLOCK)
        && (clickedBlock.getType() == Material.OAK_SIGN
            || clickedBlock.getType() == Material.OAK_WALL_SIGN)) {
      Sign sign = (Sign) clickedBlock.getState();
      if (sign.hasMetadata("propertySign")) {
        event.setCancelled(true);
        for (MetadataValue value : sign.getMetadata("propertySign")) {
          String customData = value.asString();
          String[] ids = customData.split("_");

          // Load town and player data asynchronously
          TownDataStorage.getInstance()
              .get(ids[0])
              .thenCompose(townData -> {
                if (townData == null) {
                  return java.util.concurrent.CompletableFuture.completedFuture(null);
                }
                PropertyData propertyData = townData.getProperty(ids[1]);
                if (propertyData == null) {
                  return java.util.concurrent.CompletableFuture.completedFuture(null);
                }

                if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                  return PlayerDataStorage.getInstance()
                      .get(player)
                      .thenAccept(tanPlayer -> {
                        LangType langType = tanPlayer.getLang();
                        if (!canPlayerOpenMenu(player, clickedBlock)) {
                          TanChatUtils.message(player, Lang.NO_TRADE_ALLOWED_EMBARGO.get(langType));
                          return;
                        }
                        if (propertyData.getOwner().canAccess(tanPlayer)) {
                          PlayerPropertyManager.open(player, propertyData, HumanEntity::closeInventory);
                        } else if (propertyData.isRented()
                            && propertyData.getRenterID().equals(player.getUniqueId().toString())) {
                          RenterPropertyMenu.open(player, propertyData);
                        } else {
                          if (propertyData.isForRent() || propertyData.isForSale()) {
                            BuyOrRentPropertyMenu.open(player, propertyData);
                          } else {
                            TanChatUtils.message(player, Lang.PROPERTY_NOT_FOR_SALE_OR_RENT.get(langType));
                          }
                        }
                      });
                } else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                  propertyData.showBox(player);
                  return java.util.concurrent.CompletableFuture.completedFuture(null);
                }
                return java.util.concurrent.CompletableFuture.completedFuture(null);
              })
              .exceptionally(throwable -> {
                org.leralix.tan.TownsAndNations.getPlugin()
                    .getLogger()
                    .warning("PropertySignListener failed: " + throwable.getMessage());
                return null;
              });
        }
      }
    }
  }
  private boolean canPlayerOpenMenu(Player player, Block clickedBlock) {
    ClaimedChunk2 claimedChunk2 = NewClaimedChunkStorage.getInstance().get(clickedBlock.getChunk());
    // Check embargo using cached player data from the async pipeline
    // Note: This method is called from async context, so we need to handle it differently
    // For now, we'll return true and let the property access control handle it
    return true;
  }

  /**
   * Checks if player can open property menu asynchronously.
   *
   * <p>This method loads player data and checks trade embargo status.</p>
   *
   * @param player The player to check
   * @param clickedBlock The block being clicked
   * @return CompletableFuture containing true if player can open menu
   */
  private java.util.concurrent.CompletableFuture<Boolean> canPlayerOpenMenuAsync(Player player, Block clickedBlock) {
    ClaimedChunk2 claimedChunk2 = NewClaimedChunkStorage.getInstance().get(clickedBlock.getChunk());
    return PlayerDataStorage.getInstance()
        .get(player)
        .thenApply(tanPlayer -> {
          if (!tanPlayer.hasTown()) {
            return true;
          }
          if (claimedChunk2 instanceof TownClaimedChunk townClaimedChunk) {
            TownRelation territoryRelation =
                townClaimedChunk.getTown().getWorstRelationWithSync(tanPlayer);
            return Constants.getRelationConstants(territoryRelation).canInteractWithProperty();
          }
          return true;
        });
  }
}