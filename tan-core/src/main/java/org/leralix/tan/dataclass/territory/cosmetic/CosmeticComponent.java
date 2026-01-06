package org.leralix.tan.dataclass.territory.cosmetic;
import java.util.Objects;
public final class CosmeticComponent {
    private final String description;
    private final ICustomIcon icon;
    private final Integer color;
    public static final String DEFAULT_DESCRIPTION = "A territory";
    public static final int DEFAULT_COLOR = 0x00FFFF;
    public CosmeticComponent() {
        this(DEFAULT_DESCRIPTION, null, DEFAULT_COLOR);
    }
    public CosmeticComponent(String description, ICustomIcon icon, Integer color) {
        this.description = description != null && !description.isBlank()
            ? description
            : DEFAULT_DESCRIPTION;
        this.icon = icon;
        this.color = color != null ? color : DEFAULT_COLOR;
    }
    public String getDescription() {
        return description;
    }
    public ICustomIcon getIcon() {
        return icon;
    }
    public Integer getColor() {
        return color;
    }
    public CosmeticComponent withDescription(String newDescription) {
        return new CosmeticComponent(newDescription, this.icon, this.color);
    }
    public CosmeticComponent withIcon(ICustomIcon newIcon) {
        return new CosmeticComponent(this.description, newIcon, this.color);
    }
    public CosmeticComponent withColor(Integer newColor) {
        return new CosmeticComponent(this.description, this.icon, newColor);
    }
    public boolean hasIcon() {
        return icon != null;
    }
    public String getColorAsHex() {
        return String.format("#%06X", (0xFFFFFF & color));
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CosmeticComponent that = (CosmeticComponent) o;
        return Objects.equals(description, that.description)
            && Objects.equals(icon, that.icon)
            && Objects.equals(color, that.color);
    }
    @Override
    public int hashCode() {
        return Objects.hash(description, icon, color);
    }
    @Override
    public String toString() {
        return "CosmeticComponent{" +
            "description='" + description + '\'' +
            ", icon=" + (icon != null ? "present" : "null") +
            ", color=" + getColorAsHex() +
            '}';
    }
    public static Builder builder() {
        return new Builder();
    }
    public static class Builder {
        private String description = DEFAULT_DESCRIPTION;
        private ICustomIcon icon;
        private Integer color = DEFAULT_COLOR;
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        public Builder icon(ICustomIcon icon) {
            this.icon = icon;
            return this;
        }
        public Builder color(Integer color) {
            this.color = color;
            return this;
        }
        public CosmeticComponent build() {
            return new CosmeticComponent(description, icon, color);
        }
    }
}