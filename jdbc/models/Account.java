package jdbc.models;

import java.util.Objects;

/**
 * Domain entity representing a Bank Account used in ACID transaction demonstrations.
 */
public class Account {

    private int accountNumber;
    private String holderName;
    private double balance;

    public Account() {
    }

    public Account(int accountNumber, String holderName, double balance) {
        this.accountNumber = accountNumber;
        this.holderName = holderName;
        this.balance = balance;
    }

    public int getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(int accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getHolderName() {
        return holderName;
    }

    public void setHolderName(String holderName) {
        this.holderName = holderName;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account account)) return false;
        return accountNumber == account.accountNumber &&
                Double.compare(account.balance, balance) == 0 &&
                Objects.equals(holderName, account.holderName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountNumber, holderName, balance);
    }

    @Override
    public String toString() {
        return String.format("Account[accNo=%d, holder='%s', balance=%.2f]",
                accountNumber, holderName, balance);
    }
}
