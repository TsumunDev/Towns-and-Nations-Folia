package org.leralix.tan.domain.quest.queststatic;

import org.leralix.tan.dataclass.territory.progression.TownTier;
import org.leralix.tan.domain.quest.model.Quest;
import org.leralix.tan.domain.quest.model.QuestObjective;
import org.leralix.tan.domain.quest.model.QuestReward;
import org.leralix.tan.domain.quest.model.QuestType;

import java.util.List;
import java.util.Objects;

/**
 * Immutable implementation of a quest defined in configuration.
 * <p>
 * Static quests are loaded from YAML config files and represent
 * the core quest content of the game.
 * </p>
 *
 * @param id Unique identifier (e.g., "wheat_harvest_i")
 * @param name Human-readable name
 * @param description Detailed description
 * @param type Type of quest
 * @param objectives List of objectives to complete
 * @param rewards List of rewards upon completion
 * @param requiredTier Minimum tier to accept
 * @param repeatable Whether this quest can be repeated
 * @param cooldownMs Cooldown in milliseconds before repeat
 */
public record StaticQuest(
        String id,
        String name,
        String description,
        QuestType type,
        List<QuestObjective> objectives,
        List<QuestReward> rewards,
        TownTier requiredTier,
        boolean repeatable,
        long cooldownMs
) implements Quest {

    public StaticQuest {
        Objects.requireNonNull(id, "Quest ID cannot be null");
        Objects.requireNonNull(name, "Quest name cannot be null");
        Objects.requireNonNull(type, "Quest type cannot be null");
        Objects.requireNonNull(objectives, "Quest objectives cannot be null");
        Objects.requireNonNull(rewards, "Quest rewards cannot be null");
        Objects.requireNonNull(requiredTier, "Required tier cannot be null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("Quest ID cannot be blank");
        }
        if (objectives.isEmpty()) {
            throw new IllegalArgumentException("Quest must have at least one objective");
        }
        if (rewards.isEmpty()) {
            throw new IllegalArgumentException("Quest must have at least one reward");
        }
        if (cooldownMs < 0) {
            throw new IllegalArgumentException("Cooldown cannot be negative");
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public QuestType getType() {
        return type;
    }

    @Override
    public List<QuestObjective> getObjectives() {
        return objectives;
    }

    @Override
    public List<QuestReward> getRewards() {
        return rewards;
    }

    @Override
    public TownTier getRequiredTier() {
        return requiredTier;
    }

    @Override
    public boolean isRepeatable() {
        return repeatable;
    }

    @Override
    public long getCooldownMs() {
        return cooldownMs;
    }
}
