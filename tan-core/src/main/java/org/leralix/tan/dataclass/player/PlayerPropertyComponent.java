package org.leralix.tan.dataclass.player;

import java.util.ArrayList;
import java.util.List;
import org.leralix.tan.dataclass.PropertyData;

/**
 * Manages property ownership for a player.
 * Data (propertiesListID) remains in PlayerData for Gson serialization compatibility.
 */
public class PlayerPropertyComponent {
    private final List<String> propertyIds;

    public PlayerPropertyComponent(List<String> propertyIds) {
        this.propertyIds = propertyIds;
    }

    public List<String> getPropertiesListID() {
        return propertyIds;
    }

    public void addProperty(PropertyData propertyData) {
        propertyIds.add(propertyData.getTotalID());
    }

    public List<PropertyData> getProperties() {
        return new ArrayList<>();
    }

    public void removeProperty(PropertyData propertyData) {
        propertyIds.remove(propertyData.getTotalID());
    }
}
