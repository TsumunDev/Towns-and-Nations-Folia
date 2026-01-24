package org.leralix.tan.dataclass.territory.diplomacy;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.leralix.tan.dataclass.DiplomacyProposal;
import org.leralix.tan.dataclass.RelationData;

/**
 * Unit tests for DiplomacyComponent class.
 * Tests diplomacy management, immutability, and validation.
 */
public class DiplomacyComponentTest {

    @Test
    @DisplayName("Default constructor should create component with empty state")
    void testDefaultConstructor() {
        // Act
        DiplomacyComponent diplomacy = new DiplomacyComponent();

        // Assert
        assertNotNull(diplomacy.getDiplomacyProposals());
        assertTrue(diplomacy.getDiplomacyProposals().isEmpty(), "Proposals should be empty");
        assertNotNull(diplomacy.getOverlordsProposals());
        assertTrue(diplomacy.getOverlordsProposals().isEmpty(), "Overlord proposals should be empty");
        assertNotNull(diplomacy.getRelations(), "Relations should not be null");
        assertEquals(0, diplomacy.getProposalCount());
        assertEquals(0, diplomacy.getOverlordProposalCount());
    }

    @Test
    @DisplayName("Constructor with values should store them correctly")
    void testConstructorWithValues() {
        // Arrange
        DiplomacyProposal proposal = mock(DiplomacyProposal.class);
        RelationData relations = mock(RelationData.class);
        Map<String, DiplomacyProposal> proposals = Map.of("town1", proposal);
        List<String> overlordProposals = List.of("overlord1");

        // Act
        DiplomacyComponent diplomacy = new DiplomacyComponent(proposals, overlordProposals, relations);

        // Assert
        assertEquals(1, diplomacy.getDiplomacyProposals().size());
        assertEquals(proposal, diplomacy.getDiplomacyProposals().get("town1"));
        assertEquals(1, diplomacy.getOverlordsProposals().size());
        assertEquals("overlord1", diplomacy.getOverlordsProposals().get(0));
        assertEquals(relations, diplomacy.getRelations());
    }

    @Test
    @DisplayName("Constructor with null values should use defaults")
    void testConstructorWithNulls() {
        // Act
        DiplomacyComponent diplomacy = new DiplomacyComponent(null, null, null);

        // Assert
        assertTrue(diplomacy.getDiplomacyProposals().isEmpty());
        assertTrue(diplomacy.getOverlordsProposals().isEmpty());
        assertNotNull(diplomacy.getRelations());
    }

    @Test
    @DisplayName("getDiplomacyProposals should return a defensive copy")
    void testGetDiplomacyProposalsDefensiveCopy() {
        // Arrange
        DiplomacyProposal proposal = mock(DiplomacyProposal.class);
        DiplomacyComponent diplomacy = new DiplomacyComponent(
            Map.of("town1", proposal),
            List.of(),
            mock(RelationData.class)
        );

        // Act
        Map<String, DiplomacyProposal> proposals = diplomacy.getDiplomacyProposals();
        proposals.put("town2", mock(DiplomacyProposal.class));

        // Assert
        assertEquals(1, diplomacy.getDiplomacyProposals().size(), "Original should be unchanged");
        assertFalse(diplomacy.getDiplomacyProposals().containsKey("town2"));
    }

    @Test
    @DisplayName("getOverlordsProposals should return a defensive copy")
    void testGetOverlordsProposalsDefensiveCopy() {
        // Arrange
        DiplomacyComponent diplomacy = new DiplomacyComponent(
            Map.of(),
            List.of("overlord1"),
            mock(RelationData.class)
        );

        // Act
        List<String> proposals = diplomacy.getOverlordsProposals();
        proposals.add("overlord2");

        // Assert
        assertEquals(1, diplomacy.getOverlordsProposals().size(), "Original should be unchanged");
        assertFalse(diplomacy.getOverlordsProposals().contains("overlord2"));
    }

    @Test
    @DisplayName("withProposal should return new instance with added proposal")
    void testWithProposal() {
        // Arrange
        DiplomacyComponent original = new DiplomacyComponent();
        DiplomacyProposal proposal = mock(DiplomacyProposal.class);

        // Act
        DiplomacyComponent modified = original.withProposal("town1", proposal);

        // Assert
        assertNotSame(original, modified, "Should return new instance");
        assertEquals(0, original.getProposalCount(), "Original should be unchanged");
        assertEquals(1, modified.getProposalCount());
        assertEquals(proposal, modified.getDiplomacyProposals().get("town1"));
    }

    @Test
    @DisplayName("withoutProposal should return new instance with removed proposal")
    void testWithoutProposal() {
        // Arrange
        DiplomacyProposal proposal = mock(DiplomacyProposal.class);
        DiplomacyComponent original = new DiplomacyComponent(
            Map.of("town1", proposal, "town2", mock(DiplomacyProposal.class)),
            List.of(),
            mock(RelationData.class)
        );

        // Act
        DiplomacyComponent modified = original.withoutProposal("town1");

        // Assert
        assertEquals(2, original.getProposalCount(), "Original should be unchanged");
        assertEquals(1, modified.getProposalCount());
        assertFalse(modified.getDiplomacyProposals().containsKey("town1"));
        assertTrue(modified.getDiplomacyProposals().containsKey("town2"));
    }

    @Test
    @DisplayName("withOverlordProposal should return new instance with added proposal")
    void testWithOverlordProposal() {
        // Arrange
        DiplomacyComponent original = new DiplomacyComponent();

        // Act
        DiplomacyComponent modified = original.withOverlordProposal("overlord1");

        // Assert
        assertNotSame(original, modified, "Should return new instance");
        assertEquals(0, original.getOverlordProposalCount(), "Original should be unchanged");
        assertEquals(1, modified.getOverlordProposalCount());
        assertEquals("overlord1", modified.getOverlordsProposals().get(0));
    }

    @Test
    @DisplayName("withoutOverlordProposal should return new instance with removed proposal")
    void testWithoutOverlordProposal() {
        // Arrange
        DiplomacyComponent original = new DiplomacyComponent(
            Map.of(),
            List.of("overlord1", "overlord2"),
            mock(RelationData.class)
        );

        // Act
        DiplomacyComponent modified = original.withoutOverlordProposal("overlord1");

        // Assert
        assertEquals(2, original.getOverlordProposalCount(), "Original should be unchanged");
        assertEquals(1, modified.getOverlordProposalCount());
        assertFalse(modified.getOverlordsProposals().contains("overlord1"));
        assertTrue(modified.getOverlordsProposals().contains("overlord2"));
    }

    @Test
    @DisplayName("withClearedProposals should return new instance with empty proposals")
    void testWithClearedProposals() {
        // Arrange
        DiplomacyComponent original = new DiplomacyComponent(
            Map.of("town1", mock(DiplomacyProposal.class)),
            List.of("overlord1"),
            mock(RelationData.class)
        );

        // Act
        DiplomacyComponent modified = original.withClearedProposals();

        // Assert
        assertEquals(1, original.getProposalCount(), "Original should be unchanged");
        assertEquals(1, original.getOverlordProposalCount(), "Original should be unchanged");
        assertEquals(0, modified.getProposalCount());
        assertEquals(0, modified.getOverlordProposalCount());
        assertNotNull(modified.getRelations(), "Relations should be preserved");
    }

    @Test
    @DisplayName("hasProposalFrom should return true when proposal exists")
    void testHasProposalFromTrue() {
        // Arrange
        DiplomacyProposal proposal = mock(DiplomacyProposal.class);
        DiplomacyComponent diplomacy = new DiplomacyComponent(
            Map.of("town1", proposal),
            List.of(),
            mock(RelationData.class)
        );

        // Act
        boolean result = diplomacy.hasProposalFrom("town1");

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("hasProposalFrom should return false when proposal doesn't exist")
    void testHasProposalFromFalse() {
        // Arrange
        DiplomacyComponent diplomacy = new DiplomacyComponent();

        // Act
        boolean result = diplomacy.hasProposalFrom("nonexistent");

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("hasOverlordProposalFrom should return true when proposal exists")
    void testHasOverlordProposalFromTrue() {
        // Arrange
        DiplomacyComponent diplomacy = new DiplomacyComponent(
            Map.of(),
            List.of("overlord1", "overlord2"),
            mock(RelationData.class)
        );

        // Act
        boolean result = diplomacy.hasOverlordProposalFrom("overlord1");

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("hasOverlordProposalFrom should return false when proposal doesn't exist")
    void testHasOverlordProposalFromFalse() {
        // Arrange
        DiplomacyComponent diplomacy = new DiplomacyComponent();

        // Act
        boolean result = diplomacy.hasOverlordProposalFrom("nonexistent");

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("getProposalCount should return correct count")
    void testGetProposalCount() {
        // Arrange
        DiplomacyComponent diplomacy = new DiplomacyComponent(
            Map.of(
                "town1", mock(DiplomacyProposal.class),
                "town2", mock(DiplomacyProposal.class),
                "town3", mock(DiplomacyProposal.class)
            ),
            List.of(),
            mock(RelationData.class)
        );

        // Act
        int count = diplomacy.getProposalCount();

        // Assert
        assertEquals(3, count);
    }

    @Test
    @DisplayName("getOverlordProposalCount should return correct count")
    void testGetOverlordProposalCount() {
        // Arrange
        DiplomacyComponent diplomacy = new DiplomacyComponent(
            Map.of(),
            List.of("overlord1", "overlord2", "overlord3"),
            mock(RelationData.class)
        );

        // Act
        int count = diplomacy.getOverlordProposalCount();

        // Assert
        assertEquals(3, count);
    }

    @Test
    @DisplayName("Chained operations should work correctly")
    void testChainedOperations() {
        // Arrange
        DiplomacyComponent original = new DiplomacyComponent();
        DiplomacyProposal proposal = mock(DiplomacyProposal.class);

        // Act
        DiplomacyComponent modified = original
            .withProposal("town1", proposal)
            .withOverlordProposal("overlord1")
            .withOverlordProposal("overlord2");

        // Assert
        assertEquals(1, modified.getProposalCount());
        assertEquals(2, modified.getOverlordProposalCount());
        assertEquals(0, original.getProposalCount());
    }

    @Test
    @DisplayName("Builder should create component with specified values")
    void testBuilder() {
        // Arrange
        DiplomacyProposal proposal = mock(DiplomacyProposal.class);
        RelationData relations = mock(RelationData.class);

        // Act
        DiplomacyComponent diplomacy = DiplomacyComponent.builder()
            .addProposal("town1", proposal)
            .addOverlordProposal("overlord1")
            .relations(relations)
            .build();

        // Assert
        assertEquals(1, diplomacy.getProposalCount());
        assertEquals(1, diplomacy.getOverlordProposalCount());
        assertEquals(relations, diplomacy.getRelations());
    }

    @Test
    @DisplayName("Builder with partial values should use defaults for rest")
    void testBuilderPartialValues() {
        // Act
        DiplomacyComponent diplomacy = DiplomacyComponent.builder()
            .addOverlordProposal("overlord1")
            .build();

        // Assert
        assertEquals(0, diplomacy.getProposalCount(), "Should use default empty proposals");
        assertEquals(1, diplomacy.getOverlordProposalCount());
        assertNotNull(diplomacy.getRelations(), "Should use default RelationData");
    }

    @Test
    @DisplayName("equals should return true for identical components")
    void testEquals() {
        // Arrange
        DiplomacyProposal proposal = mock(DiplomacyProposal.class);
        RelationData relations = mock(RelationData.class);
        DiplomacyComponent d1 = new DiplomacyComponent(
            Map.of("town1", proposal),
            List.of("overlord1"),
            relations
        );
        DiplomacyComponent d2 = new DiplomacyComponent(
            Map.of("town1", proposal),
            List.of("overlord1"),
            relations
        );

        // Assert
        assertEquals(d1, d2);
    }

    @Test
    @DisplayName("hashCode should be consistent with equals")
    void testHashCode() {
        // Arrange
        DiplomacyProposal proposal = mock(DiplomacyProposal.class);
        RelationData relations = mock(RelationData.class);
        DiplomacyComponent d1 = new DiplomacyComponent(
            Map.of("town1", proposal),
            List.of("overlord1"),
            relations
        );
        DiplomacyComponent d2 = new DiplomacyComponent(
            Map.of("town1", proposal),
            List.of("overlord1"),
            relations
        );

        // Assert
        assertEquals(d1.hashCode(), d2.hashCode());
    }

    @Test
    @DisplayName("toString should contain proposal counts")
    void testToString() {
        // Arrange
        DiplomacyComponent diplomacy = new DiplomacyComponent(
            Map.of("town1", mock(DiplomacyProposal.class)),
            List.of("overlord1"),
            mock(RelationData.class)
        );

        // Act
        String str = diplomacy.toString();

        // Assert
        assertTrue(str.contains("proposals=1"), "Should contain proposal count");
        assertTrue(str.contains("overlordProposals=1"), "Should contain overlord proposal count");
    }
}
