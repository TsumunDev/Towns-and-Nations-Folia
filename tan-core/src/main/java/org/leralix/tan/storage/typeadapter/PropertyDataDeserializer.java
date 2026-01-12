package org.leralix.tan.storage.typeadapter;
import com.google.gson.*;
import java.lang.reflect.Type;
import org.leralix.tan.dataclass.PropertyData;
import org.leralix.tan.dataclass.property.AbstractOwner;
public class PropertyDataDeserializer implements JsonDeserializer<PropertyData> {
  @Override
  public PropertyData deserialize(
      JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    return new GsonBuilder()
        .registerTypeAdapter(AbstractOwner.class, new OwnerDeserializer())
        .create()
        .fromJson(json, PropertyData.class);
  }
}