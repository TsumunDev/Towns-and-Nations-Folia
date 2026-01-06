package org.leralix.tan.dataclass.territory.tax;
import java.util.Objects;
public final class TaxComponent {
    private final double baseTax;
    private final double propertyRentTax;
    private final double propertyBuyTax;
    private final double propertyCreateTax;
    public static final double DEFAULT_BASE_TAX = 1.0;
    public static final double DEFAULT_PROPERTY_RENT_TAX = 0.1;
    public static final double DEFAULT_PROPERTY_BUY_TAX = 0.1;
    public static final double DEFAULT_PROPERTY_CREATE_TAX = 0.5;
    public TaxComponent() {
        this(DEFAULT_BASE_TAX, DEFAULT_PROPERTY_RENT_TAX, DEFAULT_PROPERTY_BUY_TAX, DEFAULT_PROPERTY_CREATE_TAX);
    }
    public TaxComponent(double baseTax, double propertyRentTax, double propertyBuyTax, double propertyCreateTax) {
        this.baseTax = baseTax;
        this.propertyRentTax = normalizeTaxRate(propertyRentTax);
        this.propertyBuyTax = normalizeTaxRate(propertyBuyTax);
        this.propertyCreateTax = propertyCreateTax;
    }
    public TaxComponent withBaseTax(double newBaseTax) {
        return new TaxComponent(newBaseTax, propertyRentTax, propertyBuyTax, propertyCreateTax);
    }
    public TaxComponent addToBaseTax(double delta) {
        return new TaxComponent(baseTax + delta, propertyRentTax, propertyBuyTax, propertyCreateTax);
    }
    private static double normalizeTaxRate(double rate) {
        return Math.max(0.0, Math.min(1.0, rate));
    }
    public double getBaseTax() {
        return baseTax;
    }
    public double getPropertyRentTax() {
        return propertyRentTax;
    }
    public double getPropertyBuyTax() {
        return propertyBuyTax;
    }
    public double getPropertyCreateTax() {
        return propertyCreateTax;
    }
    public TaxComponent setBaseTax(double newBaseTax) {
        return new TaxComponent(newBaseTax, propertyRentTax, propertyBuyTax, propertyCreateTax);
    }
    public TaxComponent setTaxOnRentingProperty(double amount) {
        return new TaxComponent(baseTax, normalizeTaxRate(amount), propertyBuyTax, propertyCreateTax);
    }
    public TaxComponent setTaxOnBuyingProperty(double amount) {
        return new TaxComponent(baseTax, propertyRentTax, normalizeTaxRate(amount), propertyCreateTax);
    }
    public TaxComponent setTaxOnCreatingProperty(double amount) {
        return new TaxComponent(baseTax, propertyRentTax, propertyBuyTax, amount);
    }
    public static double calculateTax(double amount, double taxRate) {
        return amount * Math.max(0.0, Math.min(1.0, taxRate));
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaxComponent other)) return false;
        return Double.compare(other.baseTax, baseTax) == 0
            && Double.compare(other.propertyRentTax, propertyRentTax) == 0
            && Double.compare(other.propertyBuyTax, propertyBuyTax) == 0
            && Double.compare(other.propertyCreateTax, propertyCreateTax) == 0;
    }
    @Override
    public int hashCode() {
        return Objects.hash(baseTax, propertyRentTax, propertyBuyTax, propertyCreateTax);
    }
    @Override
    public String toString() {
        return "TaxComponent{" +
            "baseTax=" + baseTax +
            ", propertyRentTax=" + propertyRentTax +
            ", propertyBuyTax=" + propertyBuyTax +
            ", propertyCreateTax=" + propertyCreateTax +
            '}';
    }
    public static final class Builder {
        private double baseTax = DEFAULT_BASE_TAX;
        private double propertyRentTax = DEFAULT_PROPERTY_RENT_TAX;
        private double propertyBuyTax = DEFAULT_PROPERTY_BUY_TAX;
        private double propertyCreateTax = DEFAULT_PROPERTY_CREATE_TAX;
        public Builder baseTax(double baseTax) {
            this.baseTax = baseTax;
            return this;
        }
        public Builder propertyRentTax(double propertyRentTax) {
            this.propertyRentTax = propertyRentTax;
            return this;
        }
        public Builder propertyBuyTax(double propertyBuyTax) {
            this.propertyBuyTax = propertyBuyTax;
            return this;
        }
        public Builder propertyCreateTax(double propertyCreateTax) {
            this.propertyCreateTax = propertyCreateTax;
            return this;
        }
        public TaxComponent build() {
            return new TaxComponent(baseTax, propertyRentTax, propertyBuyTax, propertyCreateTax);
        }
    }
    public static Builder builder() {
        return new Builder();
    }
}