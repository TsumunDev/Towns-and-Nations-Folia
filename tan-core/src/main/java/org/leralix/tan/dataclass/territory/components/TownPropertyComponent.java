package org.leralix.tan.dataclass.territory.components;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import org.bukkit.Location;
import org.leralix.lib.position.Vector3D;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.PropertyData;
import org.leralix.tan.dataclass.territory.TerritoryData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages properties for a town. Operates on the propertyDataMap owned by TownData
 * (Gson serialization safety — data stays on the data class).
 */
public class TownPropertyComponent {
  private static final Logger LOGGER = LoggerFactory.getLogger(TownPropertyComponent.class);
  private final String townId;
  private final Map<String, PropertyData> propertyDataMap;

  public TownPropertyComponent(String townId, Map<String, PropertyData> propertyDataMap) {
    this.townId = townId;
    this.propertyDataMap = propertyDataMap;
  }

  public Map<String, PropertyData> getPropertyDataMap() {
    return this.propertyDataMap;
  }

  public Collection<PropertyData> getProperties() {
    return getPropertyDataMap().values();
  }

  public String nextPropertyID() {
    if (getPropertyDataMap().isEmpty()) {
      return "P0";
    }
    int maxID = -1;
    for (PropertyData propertyData : getPropertyDataMap().values()) {
      try {
        String totalID = propertyData.getTotalID();
        String[] parts = totalID.split("P");
        if (parts.length > 1) {
          int currentID = Integer.parseInt(parts[1]);
          if (currentID > maxID) {
            maxID = currentID;
          }
        }
      } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
        LOGGER.warn("Malformed property ID: {}", propertyData.getTotalID());
      }
    }
    return "P" + (maxID + 1);
  }

  private PropertyData createAndStoreProperty(Vector3D p1, Vector3D p2, Object owner) {
    String propertyID = nextPropertyID();
    String id = this.townId + "_" + propertyID;
    PropertyData newProperty;
    if (owner instanceof TerritoryData) {
      newProperty = new PropertyData(id, p1, p2, (TerritoryData) owner);
    } else if (owner instanceof ITanPlayer) {
      newProperty = new PropertyData(id, p1, p2, (ITanPlayer) owner);
    } else {
      throw new IllegalArgumentException("Unsupported owner type");
    }
    this.propertyDataMap.put(propertyID, newProperty);
    return newProperty;
  }

  public PropertyData registerNewProperty(Vector3D p1, Vector3D p2, TerritoryData owner) {
    return createAndStoreProperty(p1, p2, owner);
  }

  public PropertyData registerNewProperty(Vector3D p1, Vector3D p2, ITanPlayer owner) {
    PropertyData newProperty = createAndStoreProperty(p1, p2, owner);
    owner.addProperty(newProperty);
    return newProperty;
  }

  public PropertyData getProperty(String id) {
    return getPropertyDataMap().get(id);
  }

  public PropertyData getProperty(Location location) {
    for (PropertyData propertyData : getProperties()) {
      if (propertyData.containsLocation(location)) {
        return propertyData;
      }
    }
    return null;
  }

  public void removeProperty(PropertyData propertyData) {
    this.propertyDataMap.remove(propertyData.getPropertyID());
  }

  public void removeAllProperties() {
    Iterator<PropertyData> iterator = getProperties().iterator();
    while (iterator.hasNext()) {
      PropertyData propertyData = iterator.next();
      propertyData.delete();
      iterator.remove();
    }
  }
}
