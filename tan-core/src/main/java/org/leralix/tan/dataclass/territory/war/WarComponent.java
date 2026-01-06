package org.leralix.tan.dataclass.territory.war;
import java.util.*;
public final class WarComponent {
    private final Collection<String> attackIncomingList;
    private final List<String> fortIds;
    private final List<String> occupiedFortIds;
    public WarComponent() {
        this.attackIncomingList = new ArrayList<>();
        this.fortIds = new ArrayList<>();
        this.occupiedFortIds = new ArrayList<>();
    }
    public WarComponent(
            Collection<String> attackIncomingList,
            List<String> fortIds,
            List<String> occupiedFortIds) {
        this.attackIncomingList = attackIncomingList != null
            ? new ArrayList<>(attackIncomingList)
            : new ArrayList<>();
        this.fortIds = fortIds != null ? new ArrayList<>(fortIds) : new ArrayList<>();
        this.occupiedFortIds = occupiedFortIds != null
            ? new ArrayList<>(occupiedFortIds)
            : new ArrayList<>();
    }
    public Collection<String> getAttackIncomingList() {
        return new ArrayList<>(attackIncomingList);
    }
    public List<String> getFortIds() {
        return new ArrayList<>(fortIds);
    }
    public List<String> getOccupiedFortIds() {
        return new ArrayList<>(occupiedFortIds);
    }
    public WarComponent withAttack(String attackId) {
        Collection<String> newAttacks = new ArrayList<>(this.attackIncomingList);
        newAttacks.add(attackId);
        return new WarComponent(newAttacks, this.fortIds, this.occupiedFortIds);
    }
    public WarComponent withoutAttack(String attackId) {
        Collection<String> newAttacks = new ArrayList<>(this.attackIncomingList);
        newAttacks.remove(attackId);
        return new WarComponent(newAttacks, this.fortIds, this.occupiedFortIds);
    }
    public WarComponent withClearedAttacks() {
        return new WarComponent(new ArrayList<>(), this.fortIds, this.occupiedFortIds);
    }
    public WarComponent withFort(String fortId) {
        List<String> newForts = new ArrayList<>(this.fortIds);
        newForts.add(fortId);
        return new WarComponent(this.attackIncomingList, newForts, this.occupiedFortIds);
    }
    public WarComponent withoutFort(String fortId) {
        List<String> newForts = new ArrayList<>(this.fortIds);
        newForts.remove(fortId);
        return new WarComponent(this.attackIncomingList, newForts, this.occupiedFortIds);
    }
    public WarComponent withOccupiedFort(String fortId) {
        List<String> newOccupied = new ArrayList<>(this.occupiedFortIds);
        newOccupied.add(fortId);
        return new WarComponent(this.attackIncomingList, this.fortIds, newOccupied);
    }
    public WarComponent withoutOccupiedFort(String fortId) {
        List<String> newOccupied = new ArrayList<>(this.occupiedFortIds);
        newOccupied.remove(fortId);
        return new WarComponent(this.attackIncomingList, this.fortIds, newOccupied);
    }
    public boolean hasIncomingAttacks() {
        return !attackIncomingList.isEmpty();
    }
    public boolean isUnderAttack(String attackId) {
        return attackIncomingList.contains(attackId);
    }
    public boolean ownsFort(String fortId) {
        return fortIds.contains(fortId);
    }
    public boolean occupiesFort(String fortId) {
        return occupiedFortIds.contains(fortId);
    }
    public int getIncomingAttackCount() {
        return attackIncomingList.size();
    }
    public int getFortCount() {
        return fortIds.size();
    }
    public int getOccupiedFortCount() {
        return occupiedFortIds.size();
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WarComponent that = (WarComponent) o;
        return Objects.equals(attackIncomingList, that.attackIncomingList)
            && Objects.equals(fortIds, that.fortIds)
            && Objects.equals(occupiedFortIds, that.occupiedFortIds);
    }
    @Override
    public int hashCode() {
        return Objects.hash(attackIncomingList, fortIds, occupiedFortIds);
    }
    @Override
    public String toString() {
        return "WarComponent{" +
            "incomingAttacks=" + attackIncomingList.size() +
            ", forts=" + fortIds.size() +
            ", occupiedForts=" + occupiedFortIds.size() +
            '}';
    }
    public static Builder builder() {
        return new Builder();
    }
    public static class Builder {
        private Collection<String> attackIncomingList = new ArrayList<>();
        private List<String> fortIds = new ArrayList<>();
        private List<String> occupiedFortIds = new ArrayList<>();
        public Builder attackIncomingList(Collection<String> attacks) {
            this.attackIncomingList = attacks;
            return this;
        }
        public Builder addAttack(String attackId) {
            this.attackIncomingList.add(attackId);
            return this;
        }
        public Builder fortIds(List<String> fortIds) {
            this.fortIds = fortIds;
            return this;
        }
        public Builder addFort(String fortId) {
            this.fortIds.add(fortId);
            return this;
        }
        public Builder occupiedFortIds(List<String> occupiedFortIds) {
            this.occupiedFortIds = occupiedFortIds;
            return this;
        }
        public Builder addOccupiedFort(String fortId) {
            this.occupiedFortIds.add(fortId);
            return this;
        }
        public WarComponent build() {
            return new WarComponent(attackIncomingList, fortIds, occupiedFortIds);
        }
    }
}