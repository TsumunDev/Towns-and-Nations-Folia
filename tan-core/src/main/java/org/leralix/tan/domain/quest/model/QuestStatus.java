package org.leralix.tan.domain.quest.model;

/**
 * Status of a quest in a player's quest log.
 */
public enum QuestStatus {
    /**
     * Quest is available but not yet accepted.
     */
    AVAILABLE,

    /**
     * Quest is currently active and in progress.
     */
    ACTIVE,

    /**
     * Quest objectives have been completed, awaiting reward claim.
     */
    COMPLETED,

    /**
     * Quest has been completed and rewards have been claimed.
     */
    CLAIMED,

    /**
     * Quest was abandoned by the player.
     */
    ABANDONED
}
