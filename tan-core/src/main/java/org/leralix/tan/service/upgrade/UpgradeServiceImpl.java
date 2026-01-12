package org.leralix.tan.service.upgrade;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.territory.TownData;
import org.leralix.tan.dataclass.territory.progression.TownTier;
import org.leralix.tan.domain.upgrade.model.TownUpgrade;
import org.leralix.tan.domain.upgrade.model.UpgradeCategory;
import org.leralix.tan.domain.upgrade.model.UpgradeRequirement;
import org.leralix.tan.domain.upgrade.model.UpgradeReward;
import org.leralix.tan.service.prestige.PrestigeService;
import org.leralix.tan.storage.stored.TownDataStorage;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Implementation of the upgrade service.
 */
public class UpgradeServiceImpl implements UpgradeService {

    private static UpgradeServiceImpl instance;

    private final Logger logger;
    private final Map<String, TownUpgrade> upgradesById;
    private final Map<UpgradeCategory, List<TownUpgrade>> upgradesByCategory;

    private boolean initialized = false;

    private UpgradeServiceImpl() {
        this.logger = TownsAndNations.getPlugin().getLogger();
        this.upgradesById = new ConcurrentHashMap<>();
        this.upgradesByCategory = new ConcurrentHashMap<>();

        // Initialize category lists
        for (UpgradeCategory category : UpgradeCategory.values()) {
            upgradesByCategory.put(category, new ArrayList<>());
        }
    }

    /**
     * Gets the singleton instance.
     */
    public static UpgradeServiceImpl getInstance() {
        if (instance == null) {
            instance = new UpgradeServiceImpl();
        }
        return instance;
    }

    @Override
    public void initialize() {
        if (initialized) {
            logger.warning("UpgradeService already initialized!");
            return;
        }

        logger.info("Initializing UpgradeService...");

        // Load default upgrades
        loadDefaultUpgrades();

        initialized = true;
        logger.info("UpgradeService initialized with " + upgradesById.size() + " upgrades.");
    }

    /**
     * Loads default upgrades for testing.
     * TODO: Replace with YAML config loading
     */
    private void loadDefaultUpgrades() {
        // Create a simple upgrade: Increase player cap
        TownUpgrade playerCapUpgrade = TownUpgrade.builder()
                .id("player_cap_1")
                .name("Town Hall Expansion I")
                .description("Increases your town's player capacity by 5.")
                .cost(50)
                .addRequirement(new UpgradeRequirement.TierRequirement(TownTier.CAMPING))
                .addRequirement(new UpgradeRequirement.LevelRequirement(5))
                .addReward(new UpgradeReward.StatIncrease(
                        UpgradeReward.StatType.PLAYER_CAP,
                        5
                ))
                .category(UpgradeCategory.INFRASTRUCTURE)
                .icon(Material.DIAMOND)
                .build();

        registerUpgrade(playerCapUpgrade);

        // Create a chunk cap upgrade
        TownUpgrade chunkCapUpgrade = TownUpgrade.builder()
                .id("chunk_cap_1")
                .name("Land Expansion I")
                .description("Increases your town's claim capacity by 10 chunks.")
                .cost(75)
                .addRequirement(new UpgradeRequirement.TierRequirement(TownTier.HAMLET))
                .addRequirement(new UpgradeRequirement.LevelRequirement(10))
                .addReward(new UpgradeReward.StatIncrease(
                        UpgradeReward.StatType.CHUNK_CAP,
                        10
                ))
                .category(UpgradeCategory.INFRASTRUCTURE)
                .icon(Material.GRASS_BLOCK)
                .build();

        registerUpgrade(chunkCapUpgrade);

        // Create a mob protection upgrade
        TownUpgrade mobBanUpgrade = TownUpgrade.builder()
                .id("mob_protection")
                .name("Mob Protection")
                .description("Prevents monsters from spawning in your town's claims.")
                .cost(100)
                .addRequirement(new UpgradeRequirement.TierRequirement(TownTier.CAMPING))
                .addRequirement(new UpgradeRequirement.LevelRequirement(5))
                .addReward(new UpgradeReward.FeatureUnlock(
                        UpgradeReward.FeatureType.MOB_BAN
                ))
                .category(UpgradeCategory.INFRASTRUCTURE)
                .icon(Material.IRON_SWORD)
                .build();

        registerUpgrade(mobBanUpgrade);

        logger.info("Loaded " + upgradesById.size() + " default upgrades.");
    }

    /**
     * Registers an upgrade in the service.
     */
    private void registerUpgrade(TownUpgrade upgrade) {
        upgradesById.put(upgrade.id(), upgrade);
        upgradesByCategory.get(upgrade.category()).add(upgrade);
    }

    @Override
    public CompletableFuture<List<TownUpgrade>> getAvailableUpgrades(String townId) {
        return TownDataStorage.getInstance().get(townId)
                .thenApply(town -> {
                    if (town == null) {
                        return List.of();
                    }

                    // Filter upgrades by requirements
                    return upgradesById.values().stream()
                            .filter(upgrade -> upgrade.canPurchase(town))
                            .collect(Collectors.toList());
                });
    }

    @Override
    public List<TownUpgrade> getAllUpgrades() {
        return List.copyOf(upgradesById.values());
    }

    @Override
    public Optional<TownUpgrade> getUpgrade(String upgradeId) {
        return Optional.ofNullable(upgradesById.get(upgradeId));
    }

    @Override
    public CompletableFuture<PurchaseResult> purchaseUpgrade(
            Player player,
            String townId,
            String upgradeId
    ) {
        // Get the upgrade
        Optional<TownUpgrade> upgradeOpt = getUpgrade(upgradeId);
        if (upgradeOpt.isEmpty()) {
            return CompletableFuture.completedFuture(
                    PurchaseResult.NOT_FOUND
            );
        }

        TownUpgrade upgrade = upgradeOpt.get();

        // Get the town
        return TownDataStorage.getInstance().get(townId)
                .thenCompose(town -> {
                    if (town == null) {
                        return CompletableFuture.completedFuture(
                                PurchaseResult.TOWN_NOT_FOUND
                        );
                    }

                    // Check requirements
                    if (!upgrade.canPurchase(town)) {
                        return CompletableFuture.completedFuture(
                                PurchaseResult.REQUIREMENTS_NOT_MET
                        );
                    }

                    // Check prestige balance
                    return PrestigeService.getInstance().getBalance(townId)
                            .thenCompose(balance -> {
                                if (balance < upgrade.cost()) {
                                    return CompletableFuture.completedFuture(
                                            PurchaseResult.INSUFFICIENT_PRESTIGE
                                    );
                                }

                                // Spend prestige
                                return PrestigeService.getInstance()
                                        .spendPrestige(townId, upgrade.cost(), "Purchased: " + upgrade.name())
                                        .thenApply(success -> {
                                            if (!success) {
                                                return PurchaseResult.ERROR;
                                            }

                                            // Apply upgrade rewards
                                            for (UpgradeReward reward : upgrade.rewards()) {
                                                try {
                                                    reward.apply(town);
                                                } catch (Exception e) {
                                                    logger.severe("Failed to apply upgrade reward: " + e.getMessage());
                                                    e.printStackTrace();
                                                }
                                            }

                                            // Mark as purchased
                                            town.addPurchasedUpgrade(upgradeId);

                                            // Save town
                                            TownDataStorage.getInstance().update(town);

                                            // Notify player
                                            player.sendMessage("§a§lUpgrade Purchased!");
                                            player.sendMessage("§e" + upgrade.name());
                                            player.sendMessage("§7" + upgrade.description());

                                            return PurchaseResult.SUCCESS;
                                        });
                            });
                });
    }

    @Override
    public CompletableFuture<Boolean> canPurchase(String townId, String upgradeId) {
        Optional<TownUpgrade> upgradeOpt = getUpgrade(upgradeId);
        if (upgradeOpt.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }

        return TownDataStorage.getInstance().get(townId)
                .thenApply(town -> town != null && upgradeOpt.get().canPurchase(town));
    }

    @Override
    public CompletableFuture<Set<String>> getPurchasedUpgrades(String townId) {
        // TODO: Implement upgrade tracking in TownData
        // For now, return empty set
        return CompletableFuture.completedFuture(Set.of());
    }
}
