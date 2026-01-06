package org.leralix.tan.dataclass.territory.diplomacy;
import org.leralix.tan.dataclass.DiplomacyProposal;
import org.leralix.tan.dataclass.RelationData;
import org.leralix.tan.enums.TownRelation;
import java.util.*;
public final class DiplomacyComponent {
    private final Map<String, DiplomacyProposal> diplomacyProposals;
    private final List<String> overlordsProposals;
    private final RelationData relations;
    public DiplomacyComponent() {
        this.diplomacyProposals = new HashMap<>();
        this.overlordsProposals = new ArrayList<>();
        this.relations = new RelationData();
    }
    public DiplomacyComponent(
            Map<String, DiplomacyProposal> diplomacyProposals,
            List<String> overlordsProposals,
            RelationData relations) {
        this.diplomacyProposals = diplomacyProposals != null
            ? new HashMap<>(diplomacyProposals)
            : new HashMap<>();
        this.overlordsProposals = overlordsProposals != null
            ? new ArrayList<>(overlordsProposals)
            : new ArrayList<>();
        this.relations = relations != null ? relations : new RelationData();
    }
    public Map<String, DiplomacyProposal> getDiplomacyProposals() {
        return new HashMap<>(diplomacyProposals);
    }
    public List<String> getOverlordsProposals() {
        return new ArrayList<>(overlordsProposals);
    }
    public RelationData getRelations() {
        return relations;
    }
    public DiplomacyComponent withProposal(String territoryID, DiplomacyProposal proposal) {
        Map<String, DiplomacyProposal> newProposals = new HashMap<>(this.diplomacyProposals);
        newProposals.put(territoryID, proposal);
        return new DiplomacyComponent(newProposals, this.overlordsProposals, this.relations);
    }
    public DiplomacyComponent withoutProposal(String territoryID) {
        Map<String, DiplomacyProposal> newProposals = new HashMap<>(this.diplomacyProposals);
        newProposals.remove(territoryID);
        return new DiplomacyComponent(newProposals, this.overlordsProposals, this.relations);
    }
    public DiplomacyComponent withOverlordProposal(String overlordID) {
        List<String> newProposals = new ArrayList<>(this.overlordsProposals);
        newProposals.add(overlordID);
        return new DiplomacyComponent(this.diplomacyProposals, newProposals, this.relations);
    }
    public DiplomacyComponent withoutOverlordProposal(String overlordID) {
        List<String> newProposals = new ArrayList<>(this.overlordsProposals);
        newProposals.remove(overlordID);
        return new DiplomacyComponent(this.diplomacyProposals, newProposals, this.relations);
    }
    public DiplomacyComponent withClearedProposals() {
        return new DiplomacyComponent(new HashMap<>(), new ArrayList<>(), this.relations);
    }
    public boolean hasProposalFrom(String territoryID) {
        return diplomacyProposals.containsKey(territoryID);
    }
    public boolean hasOverlordProposalFrom(String overlordID) {
        return overlordsProposals.contains(overlordID);
    }
    public int getProposalCount() {
        return diplomacyProposals.size();
    }
    public int getOverlordProposalCount() {
        return overlordsProposals.size();
    }
    public List<String> getTerritoriesWithRelation(TownRelation relation) {
        return relations.getTerritoriesIDWithRelation(relation);
    }
    public TownRelation getRelationWith(String territoryID) {
        return relations.getRelationWith(territoryID);
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiplomacyComponent that = (DiplomacyComponent) o;
        return Objects.equals(diplomacyProposals, that.diplomacyProposals)
            && Objects.equals(overlordsProposals, that.overlordsProposals)
            && Objects.equals(relations, that.relations);
    }
    @Override
    public int hashCode() {
        return Objects.hash(diplomacyProposals, overlordsProposals, relations);
    }
    @Override
    public String toString() {
        return "DiplomacyComponent{" +
            "proposals=" + diplomacyProposals.size() +
            ", overlordProposals=" + overlordsProposals.size() +
            ", relations=" + relations +
            '}';
    }
    public static Builder builder() {
        return new Builder();
    }
    public static class Builder {
        private Map<String, DiplomacyProposal> diplomacyProposals = new HashMap<>();
        private List<String> overlordsProposals = new ArrayList<>();
        private RelationData relations = new RelationData();
        public Builder diplomacyProposals(Map<String, DiplomacyProposal> proposals) {
            this.diplomacyProposals = proposals;
            return this;
        }
        public Builder addProposal(String territoryID, DiplomacyProposal proposal) {
            this.diplomacyProposals.put(territoryID, proposal);
            return this;
        }
        public Builder overlordsProposals(List<String> proposals) {
            this.overlordsProposals = proposals;
            return this;
        }
        public Builder addOverlordProposal(String overlordID) {
            this.overlordsProposals.add(overlordID);
            return this;
        }
        public Builder relations(RelationData relations) {
            this.relations = relations;
            return this;
        }
        public DiplomacyComponent build() {
            return new DiplomacyComponent(diplomacyProposals, overlordsProposals, relations);
        }
    }
}