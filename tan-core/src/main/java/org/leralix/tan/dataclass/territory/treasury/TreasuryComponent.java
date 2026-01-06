package org.leralix.tan.dataclass.territory.treasury;
public final class TreasuryComponent {
    private final double balance;
    public static final double DEFAULT_BALANCE = 0.0;
    public TreasuryComponent() {
        this(DEFAULT_BALANCE);
    }
    public TreasuryComponent(double balance) {
        this.balance = balance;
    }
    public TreasuryComponent addToBalance(double delta) {
        return new TreasuryComponent(this.balance + delta);
    }
    public double getBalance() {
        return balance;
    }
    public boolean hasSufficientFunds(double amount) {
        return balance >= amount;
    }
    public double getDeficit(double amount) {
        double deficit = amount - balance;
        return deficit > 0 ? deficit : 0.0;
    }
    public TreasuryComponent withdraw(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Withdrawal amount cannot be negative");
        }
        return new TreasuryComponent(balance - amount);
    }
    public TreasuryComponent deposit(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Deposit amount cannot be negative");
        }
        return new TreasuryComponent(balance + amount);
    }
    public TreasuryComponent withBalance(double newBalance) {
        return new TreasuryComponent(newBalance);
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TreasuryComponent other)) return false;
        return Double.compare(other.balance, balance) == 0;
    }
    @Override
    public int hashCode() {
        return Double.hashCode(balance);
    }
    @Override
    public String toString() {
        return "TreasuryComponent{" +
            "balance=" + balance +
            '}';
    }
    public String getFormattedBalance() {
        return String.format("%.2f", balance);
    }
    public static final class Builder {
        private double balance = DEFAULT_BALANCE;
        public Builder balance(double balance) {
            this.balance = balance;
            return this;
        }
        public TreasuryComponent build() {
            return new TreasuryComponent(balance);
        }
    }
    public static Builder builder() {
        return new Builder();
    }
}