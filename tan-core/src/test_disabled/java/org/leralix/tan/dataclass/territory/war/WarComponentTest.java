package org.leralix.tan.dataclass.territory.war;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for WarComponent class.
 * Tests war management, immutability, and validation.
 */
public class WarComponentTest {

    @Test
    @DisplayName("Default constructor should create component with empty state")
    void testDefaultConstructor() {
        // Act
        WarComponent war = new WarComponent();

        // Assert
        assertNotNull(war.getAttackIncomingList());
        assertTrue(war.getAttackIncomingList().isEmpty(), "Attacks should be empty");
        assertTrue(war.getFortIds().isEmpty(), "Forts should be empty");
        assertTrue(war.getOccupiedFortIds().isEmpty(), "Occupied forts should be empty");
        assertEquals(0, war.getIncomingAttackCount());
        assertEquals(0, war.getFortCount());
        assertEquals(0, war.getOccupiedFortCount());
    }

    @Test
    @DisplayName("Constructor with values should store them correctly")
    void testConstructorWithValues() {
        // Arrange
        Collection<String> attacks = List.of("attack1", "attack2");
        List<String> forts = List.of("fort1");
        List<String> occupied = List.of("occupied1");

        // Act
        WarComponent war = new WarComponent(attacks, forts, occupied);

        // Assert
        assertEquals(2, war.getAttackIncomingList().size());
        assertTrue(war.getAttackIncomingList().contains("attack1"));
        assertEquals(1, war.getFortIds().size());
        assertEquals("fort1", war.getFortIds().get(0));
        assertEquals(1, war.getOccupiedFortIds().size());
        assertEquals("occupied1", war.getOccupiedFortIds().get(0));
    }

    @Test
    @DisplayName("Constructor with null values should use defaults")
    void testConstructorWithNulls() {
        // Act
        WarComponent war = new WarComponent(null, null, null);

        // Assert
        assertTrue(war.getAttackIncomingList().isEmpty());
        assertTrue(war.getFortIds().isEmpty());
        assertTrue(war.getOccupiedFortIds().isEmpty());
    }

    @Test
    @DisplayName("getAttackIncomingList should return a defensive copy")
    void testGetAttackIncomingListDefensiveCopy() {
        // Arrange
        WarComponent war = new WarComponent(List.of("attack1"), List.of(), List.of());

        // Act
        Collection<String> attacks = war.getAttackIncomingList();
        attacks.add("attack2");

        // Assert
        assertEquals(1, war.getAttackIncomingList().size(), "Original should be unchanged");
        assertFalse(war.getAttackIncomingList().contains("attack2"));
    }

    @Test
    @DisplayName("getFortIds should return a defensive copy")
    void testGetFortIdsDefensiveCopy() {
        // Arrange
        WarComponent war = new WarComponent(List.of(), List.of("fort1"), List.of());

        // Act
        List<String> forts = war.getFortIds();
        forts.add("fort2");

        // Assert
        assertEquals(1, war.getFortIds().size(), "Original should be unchanged");
        assertFalse(war.getFortIds().contains("fort2"));
    }

    @Test
    @DisplayName("getOccupiedFortIds should return a defensive copy")
    void testGetOccupiedFortIdsDefensiveCopy() {
        // Arrange
        WarComponent war = new WarComponent(List.of(), List.of(), List.of("occupied1"));

        // Act
        List<String> occupied = war.getOccupiedFortIds();
        occupied.add("occupied2");

        // Assert
        assertEquals(1, war.getOccupiedFortIds().size(), "Original should be unchanged");
        assertFalse(war.getOccupiedFortIds().contains("occupied2"));
    }

    @Test
    @DisplayName("withAttack should return new instance with added attack")
    void testWithAttack() {
        // Arrange
        WarComponent original = new WarComponent();

        // Act
        WarComponent modified = original.withAttack("attack1");

        // Assert
        assertNotSame(original, modified, "Should return new instance");
        assertEquals(0, original.getIncomingAttackCount(), "Original should be unchanged");
        assertEquals(1, modified.getIncomingAttackCount());
        assertTrue(modified.getAttackIncomingList().contains("attack1"));
    }

    @Test
    @DisplayName("withoutAttack should return new instance with removed attack")
    void testWithoutAttack() {
        // Arrange
        WarComponent original = new WarComponent(List.of("attack1", "attack2"), List.of(), List.of());

        // Act
        WarComponent modified = original.withoutAttack("attack1");

        // Assert
        assertEquals(2, original.getIncomingAttackCount(), "Original should be unchanged");
        assertEquals(1, modified.getIncomingAttackCount());
        assertFalse(modified.getAttackIncomingList().contains("attack1"));
        assertTrue(modified.getAttackIncomingList().contains("attack2"));
    }

    @Test
    @DisplayName("withClearedAttacks should return new instance with empty attacks")
    void testWithClearedAttacks() {
        // Arrange
        WarComponent original = new WarComponent(List.of("attack1"), List.of("fort1"), List.of("occupied1"));

        // Act
        WarComponent modified = original.withClearedAttacks();

        // Assert
        assertEquals(1, original.getIncomingAttackCount(), "Original should be unchanged");
        assertEquals(1, original.getFortCount(), "Forts should be preserved");
        assertEquals(1, original.getOccupiedFortCount(), "Occupied forts should be preserved");
        assertEquals(0, modified.getIncomingAttackCount());
        assertEquals(1, modified.getFortCount());
        assertEquals(1, modified.getOccupiedFortCount());
    }

    @Test
    @DisplayName("withFort should return new instance with added fort")
    void testWithFort() {
        // Arrange
        WarComponent original = new WarComponent();

        // Act
        WarComponent modified = original.withFort("fort1");

        // Assert
        assertNotSame(original, modified, "Should return new instance");
        assertEquals(0, original.getFortCount(), "Original should be unchanged");
        assertEquals(1, modified.getFortCount());
        assertEquals("fort1", modified.getFortIds().get(0));
    }

    @Test
    @DisplayName("withoutFort should return new instance with removed fort")
    void testWithoutFort() {
        // Arrange
        WarComponent original = new WarComponent(List.of(), List.of("fort1", "fort2"), List.of());

        // Act
        WarComponent modified = original.withoutFort("fort1");

        // Assert
        assertEquals(2, original.getFortCount(), "Original should be unchanged");
        assertEquals(1, modified.getFortCount());
        assertFalse(modified.getFortIds().contains("fort1"));
        assertTrue(modified.getFortIds().contains("fort2"));
    }

    @Test
    @DisplayName("withOccupiedFort should return new instance with added occupied fort")
    void testWithOccupiedFort() {
        // Arrange
        WarComponent original = new WarComponent();

        // Act
        WarComponent modified = original.withOccupiedFort("occupied1");

        // Assert
        assertNotSame(original, modified, "Should return new instance");
        assertEquals(0, original.getOccupiedFortCount(), "Original should be unchanged");
        assertEquals(1, modified.getOccupiedFortCount());
        assertEquals("occupied1", modified.getOccupiedFortIds().get(0));
    }

    @Test
    @DisplayName("withoutOccupiedFort should return new instance with removed occupied fort")
    void testWithoutOccupiedFort() {
        // Arrange
        WarComponent original = new WarComponent(List.of(), List.of(), List.of("occupied1", "occupied2"));

        // Act
        WarComponent modified = original.withoutOccupiedFort("occupied1");

        // Assert
        assertEquals(2, original.getOccupiedFortCount(), "Original should be unchanged");
        assertEquals(1, modified.getOccupiedFortCount());
        assertFalse(modified.getOccupiedFortIds().contains("occupied1"));
        assertTrue(modified.getOccupiedFortIds().contains("occupied2"));
    }

    @Test
    @DisplayName("hasIncomingAttacks should return true when attacks exist")
    void testHasIncomingAttacksTrue() {
        // Arrange
        WarComponent war = new WarComponent(List.of("attack1"), List.of(), List.of());

        // Act
        boolean result = war.hasIncomingAttacks();

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("hasIncomingAttacks should return false when no attacks")
    void testHasIncomingAttacksFalse() {
        // Arrange
        WarComponent war = new WarComponent();

        // Act
        boolean result = war.hasIncomingAttacks();

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("isUnderAttack should return true when attack exists")
    void testIsUnderAttackTrue() {
        // Arrange
        WarComponent war = new WarComponent(List.of("attack1", "attack2"), List.of(), List.of());

        // Act
        boolean result = war.isUnderAttack("attack1");

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("isUnderAttack should return false when attack doesn't exist")
    void testIsUnderAttackFalse() {
        // Arrange
        WarComponent war = new WarComponent(List.of("attack1"), List.of(), List.of());

        // Act
        boolean result = war.isUnderAttack("nonexistent");

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("ownsFort should return true when fort is owned")
    void testOwnsFortTrue() {
        // Arrange
        WarComponent war = new WarComponent(List.of(), List.of("fort1", "fort2"), List.of());

        // Act
        boolean result = war.ownsFort("fort1");

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("ownsFort should return false when fort is not owned")
    void testOwnsFortFalse() {
        // Arrange
        WarComponent war = new WarComponent(List.of(), List.of("fort1"), List.of());

        // Act
        boolean result = war.ownsFort("nonexistent");

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("occupiesFort should return true when fort is occupied")
    void testOccupiesFortTrue() {
        // Arrange
        WarComponent war = new WarComponent(List.of(), List.of(), List.of("occupied1", "occupied2"));

        // Act
        boolean result = war.occupiesFort("occupied1");

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("occupiesFort should return false when fort is not occupied")
    void testOccupiesFortFalse() {
        // Arrange
        WarComponent war = new WarComponent(List.of(), List.of(), List.of("occupied1"));

        // Act
        boolean result = war.occupiesFort("nonexistent");

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("getIncomingAttackCount should return correct count")
    void testGetIncomingAttackCount() {
        // Arrange
        WarComponent war = new WarComponent(List.of("attack1", "attack2", "attack3"), List.of(), List.of());

        // Act
        int count = war.getIncomingAttackCount();

        // Assert
        assertEquals(3, count);
    }

    @Test
    @DisplayName("getFortCount should return correct count")
    void testGetFortCount() {
        // Arrange
        WarComponent war = new WarComponent(List.of(), List.of("fort1", "fort2"), List.of());

        // Act
        int count = war.getFortCount();

        // Assert
        assertEquals(2, count);
    }

    @Test
    @DisplayName("getOccupiedFortCount should return correct count")
    void testGetOccupiedFortCount() {
        // Arrange
        WarComponent war = new WarComponent(List.of(), List.of(), List.of("occupied1", "occupied2", "occupied3"));

        // Act
        int count = war.getOccupiedFortCount();

        // Assert
        assertEquals(3, count);
    }

    @Test
    @DisplayName("Chained operations should work correctly")
    void testChainedOperations() {
        // Arrange
        WarComponent original = new WarComponent();

        // Act
        WarComponent modified = original
            .withAttack("attack1")
            .withFort("fort1")
            .withOccupiedFort("occupied1")
            .withAttack("attack2");

        // Assert
        assertEquals(2, modified.getIncomingAttackCount());
        assertEquals(1, modified.getFortCount());
        assertEquals(1, modified.getOccupiedFortCount());
        assertEquals(0, original.getIncomingAttackCount());
    }

    @Test
    @DisplayName("Builder should create component with specified values")
    void testBuilder() {
        // Act
        WarComponent war = WarComponent.builder()
            .addAttack("attack1")
            .addFort("fort1")
            .addOccupiedFort("occupied1")
            .build();

        // Assert
        assertEquals(1, war.getIncomingAttackCount());
        assertEquals(1, war.getFortCount());
        assertEquals(1, war.getOccupiedFortCount());
    }

    @Test
    @DisplayName("Builder with partial values should use defaults for rest")
    void testBuilderPartialValues() {
        // Act
        WarComponent war = WarComponent.builder()
            .addFort("fort1")
            .build();

        // Assert
        assertEquals(0, war.getIncomingAttackCount(), "Should use default empty attacks");
        assertEquals(1, war.getFortCount());
        assertEquals(0, war.getOccupiedFortCount(), "Should use default empty occupied forts");
    }

    @Test
    @DisplayName("equals should return true for identical components")
    void testEquals() {
        // Arrange
        WarComponent w1 = new WarComponent(List.of("attack1"), List.of("fort1"), List.of("occupied1"));
        WarComponent w2 = new WarComponent(List.of("attack1"), List.of("fort1"), List.of("occupied1"));

        // Assert
        assertEquals(w1, w2);
    }

    @Test
    @DisplayName("hashCode should be consistent with equals")
    void testHashCode() {
        // Arrange
        WarComponent w1 = new WarComponent(List.of("attack1"), List.of("fort1"), List.of("occupied1"));
        WarComponent w2 = new WarComponent(List.of("attack1"), List.of("fort1"), List.of("occupied1"));

        // Assert
        assertEquals(w1.hashCode(), w2.hashCode());
    }

    @Test
    @DisplayName("toString should contain counts")
    void testToString() {
        // Arrange
        WarComponent war = new WarComponent(List.of("attack1"), List.of("fort1"), List.of("occupied1"));

        // Act
        String str = war.toString();

        // Assert
        assertTrue(str.contains("incomingAttacks=1"), "Should contain attack count");
        assertTrue(str.contains("forts=1"), "Should contain fort count");
        assertTrue(str.contains("occupiedForts=1"), "Should contain occupied fort count");
    }
}
