package org.leralix.tan.domain.property;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.leralix.lib.data.SoundEnum;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.PropertyData;
import org.leralix.tan.dataclass.property.PlayerOwned;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.dataclass.territory.permission.RelationPermission;
import org.leralix.tan.dataclass.newhistory.PropertyBuyTaxTransaction;
import org.leralix.tan.economy.EconomyUtil;
import org.leralix.tan.lang.Lang;
import org.leralix.tan.lang.LangType;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.utils.NumberUtils;
import org.leralix.tan.utils.text.TanChatUtils;

/**
 * Handles sales logic for a property. State is kept on PropertyData for Gson compatibility.
 */
public class PropertySalesService {

    private final PropertyData property;

    public PropertySalesService(PropertyData property) {
        this.property = property;
    }

    public double getSalePrice() {
        TownData town = property.getTown();
        if (town == null) {
            return property.getBaseSalePrice();
        }
        return NumberUtils.roundWithDigits(property.getBaseSalePrice() * (1 + town.getTaxOnBuyingProperty()));
    }

    public void buyProperty(Player buyer) {
        LangType langType = PlayerDataStorage.getInstance().getSync(buyer).getLang();
        double playerBalance = EconomyUtil.getBalance(buyer);
        double cost = getSalePrice();
        if (playerBalance < cost) {
            TanChatUtils.message(
                buyer,
                Lang.PLAYER_NOT_ENOUGH_MONEY_EXTENDED.get(
                    langType, Double.toString(cost - playerBalance)),
                SoundEnum.MINOR_BAD);
            return;
        }
        if (property.getOwner() instanceof PlayerOwned playerOwned) {
            UUID exOwnerID = UUID.fromString(playerOwned.getPlayerID());
            OfflinePlayer exOwnerOffline = Bukkit.getOfflinePlayer(exOwnerID);
            Player exOwner = exOwnerOffline.getPlayer();
            if (exOwner != null) {
                TanChatUtils.message(
                    exOwner,
                    Lang.PROPERTY_SOLD_EX_OWNER.get(
                        langType, property.getName(), buyer.getName(), Double.toString(getSalePrice())),
                    SoundEnum.GOOD);
            }
            ITanPlayer exOwnerData = PlayerDataStorage.getInstance().getSync(exOwnerID);
            if (exOwnerData != null) {
                exOwnerData.removeProperty(property);
            }
        }
        TanChatUtils.message(
            buyer,
            Lang.PROPERTY_SOLD_NEW_OWNER.get(langType, property.getName(), Double.toString(getSalePrice())),
            SoundEnum.BAD);
        TownData town = property.getTown();
        double townCut = getSalePrice() - property.getBaseSalePrice();
        TownsAndNations.getPlugin()
            .getDatabaseHandler()
            .addTransactionHistory(new PropertyBuyTaxTransaction(town, property, townCut));
        EconomyUtil.removeFromBalance(buyer, getSalePrice());
        property.getOwner().addToBalance(property.getBaseSalePrice());
        town.addToBalance(townCut);
        ITanPlayer newOwnerData =
            PlayerDataStorage.getInstance().getSync(buyer.getUniqueId().toString());
        newOwnerData.addProperty(property);
        property.setOwner(new PlayerOwned(buyer.getUniqueId().toString()));
        property.setIsForSale(false);
        org.leralix.tan.utils.FoliaScheduler.runTask(TownsAndNations.getPlugin(), property::updateSign);
        property.getPermissionManager().setAll(RelationPermission.SELECTED_ONLY);
    }
}
