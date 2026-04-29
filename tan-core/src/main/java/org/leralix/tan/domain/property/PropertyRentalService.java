package org.leralix.tan.domain.property;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.PropertyData;
import org.leralix.tan.dataclass.territory.TerritoryData;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.dataclass.territory.permission.RelationPermission;
import org.leralix.tan.storage.stored.PlayerDataStorage;
import org.leralix.tan.utils.NumberUtils;
import org.leralix.tan.utils.constants.Constants;

/**
 * Handles rental logic for a property. State is kept on PropertyData for Gson compatibility.
 */
public class PropertyRentalService {

    private final PropertyData property;

    public PropertyRentalService(PropertyData property) {
        this.property = property;
    }

    public void allocateRenter(Player renter) {
        property.setRentingPlayerID(renter.getUniqueId().toString());
        property.setIsForRent(false);
        if (Constants.shouldPayRentAtStart()) payRent();
        org.leralix.tan.utils.FoliaScheduler.runTask(TownsAndNations.getPlugin(), property::updateSign);
        property.getPermissionManager().setAll(RelationPermission.SELECTED_ONLY);
    }

    public Player getRenterPlayer() {
        String rentingPlayerID = property.getRenterID();
        if (rentingPlayerID == null) {
            return null;
        }
        try {
            return Bukkit.getPlayer(UUID.fromString(rentingPlayerID));
        } catch (IllegalArgumentException e) {
            TownsAndNations.getPlugin()
                .getLogger()
                .warning("Invalid renting player UUID for property " + property.getName() + ": " + rentingPlayerID);
            return null;
        }
    }

    public OfflinePlayer getOfflineRenter() {
        String rentingPlayerID = property.getRenterID();
        if (rentingPlayerID == null) {
            return null;
        }
        try {
            return Bukkit.getOfflinePlayer(UUID.fromString(rentingPlayerID));
        } catch (IllegalArgumentException e) {
            TownsAndNations.getPlugin()
                .getLogger()
                .warning("Invalid renting player UUID for property " + property.getName() + ": " + rentingPlayerID);
            return null;
        }
    }

    public CompletableFuture<Void> payRent() {
        String rentingPlayerID = property.getRenterID();
        if (rentingPlayerID == null) {
            return CompletableFuture.completedFuture(null);
        }

        UUID renterUuid;
        try {
            renterUuid = UUID.fromString(rentingPlayerID);
        } catch (IllegalArgumentException e) {
            TownsAndNations.getPlugin()
                .getLogger()
                .warning("Invalid renting player UUID for property " + property.getName() + ", expelling renter");
            expelRenterAsync(true);
            return CompletableFuture.completedFuture(null);
        }

        TerritoryData town = property.getTown();
        if (town == null) {
            TownsAndNations.getPlugin()
                .getLogger()
                .warning("Property " + property.getName() + " has no valid town, cannot collect rent");
            return CompletableFuture.completedFuture(null);
        }

        double baseRent = property.getBaseRentPrice();
        double rent = getRentPrice();
        double taxRent = rent - baseRent;

        OfflinePlayer renter = Bukkit.getOfflinePlayer(renterUuid);

        return org.leralix.tan.service.AsyncEconomyService.getBalance(renter)
            .thenCompose(balance -> {
                if (balance < rent) {
                    expelRenterAsync(true);
                    return CompletableFuture.completedFuture(null);
                }
                return org.leralix.tan.service.AsyncEconomyService.withdraw(renter, rent)
                    .thenRun(() -> {
                        property.getOwner().addToBalance(baseRent);
                        town.addToBalance(taxRent);
                    });
            })
            .exceptionally(throwable -> {
                TownsAndNations.getPlugin()
                    .getLogger()
                    .warning("Failed to collect rent for property " + property.getName() + ": " + throwable.getMessage());
                return null;
            });
    }

    public CompletableFuture<Void> expelRenterAsync(boolean rentBack) {
        if (!property.isRented()) {
            return CompletableFuture.completedFuture(null);
        }

        return PlayerDataStorage.getInstance()
            .get(property.getRenterID())
            .thenAccept(renter -> {
                renter.removeProperty(property);
                property.setRentingPlayerID(null);
                if (rentBack) property.setIsForRent(true);
                org.leralix.tan.utils.FoliaScheduler.runTask(
                    TownsAndNations.getPlugin(),
                    property::updateSign);
                property.getPermissionManager().setAll(RelationPermission.SELECTED_ONLY);
            })
            .exceptionally(throwable -> {
                TownsAndNations.getPlugin()
                    .getLogger()
                    .warning("Failed to expel renter from property " + property.getName() + ": " + throwable.getMessage());
                return null;
            });
    }

    public void expelRenter(boolean rentBack) {
        if (!property.isRented()) return;
        ITanPlayer renter = PlayerDataStorage.getInstance().getSync(property.getRenterID());
        renter.removeProperty(property);
        property.setRentingPlayerID(null);
        if (rentBack) property.setIsForRent(true);
        org.leralix.tan.utils.FoliaScheduler.runTask(TownsAndNations.getPlugin(), property::updateSign);
        property.getPermissionManager().setAll(RelationPermission.SELECTED_ONLY);
    }

    public double getRentPrice() {
        TownData town = property.getTown();
        if (town == null) {
            return property.getBaseRentPrice();
        }
        return NumberUtils.roundWithDigits(property.getBaseRentPrice() * (1 + town.getTaxOnRentingProperty()));
    }
}
