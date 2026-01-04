package org.leralix.tan.dataclass.territory.components;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.bukkit.Location;
import org.leralix.lib.position.Vector3D;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.PropertyData;
import org.leralix.tan.dataclass.territory.TerritoryData;

public class TownPropertyComponent {

  private final String townId;
  private Map<String, PropertyData> propertyDataMap;

  public TownPropertyComponent(String townId) {
    this.townId = townId;
  }

  public Map<String, PropertyData> getPropertyDataMap() {
    if (this.propertyDataMap == null) {
      synchronized (this) {
        if (this.propertyDataMap == null) {
          this.propertyDataMap = new HashMap<>();
        }
      }
    }
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
        System.err.println("Warning: Malformed property ID: " + propertyData.getTotalID());
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
