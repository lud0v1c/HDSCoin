import transactions.BasicTransaction;
import transactions.Transaction;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.List;
import java.util.Random;


public class Ledger implements Serializable{

    private PublicKey ID;
    private int balance;
    private TransactionList transactions;

    public Ledger (PublicKey clientID, int initBalance){
        this.ID = clientID;
        this.balance = initBalance;
        this.transactions = new TransactionList();
    }

    public PublicKey getID() { return this.ID; }
    public int getBalance() { return this.balance; }
    public void setBalance(int amount) { this.balance = amount; }
    public TransactionList getTransactionList() { return this.transactions; }
    public void updateTransactionList(Transaction transaction) { this.transactions.update(transaction);}

    public Object[] getBasicLedger() {
        List<BasicTransaction> basicTransactionList = this.getTransactionList().getBasicTransactions();
        Object[] result = new Object[]{this.ID, this.balance, basicTransactionList};
        return result;
    }

    // Byzantine behaviour - picks a random transaction and sets it as the most recent one
    public void byzSwitchTransactions() {
        if (this.transactions.getSize() < 2) { return; }
        Random generator = new Random();
        int a = generator.nextInt(transactions.getSize());
        int b = generator.nextInt(transactions.getSize());
        Transaction transa = this.transactions.getTransaction(a);
        Transaction transb = this.transactions.getTransaction(b);
        this.transactions.substitute(a, transb);
        this.transactions.substitute(b, transa);
    }

    public void byzRemoveLastTransaction() {
        if(this.transactions.getSize() > 0){ this.transactions.removeLast(); }
    }

    public void byzSwitchSignatures() {
        for (Transaction transaction : this.transactions.getList()) {
            byte[] ssign = transaction.getSrcSignature();
            byte[] dsign = transaction.getDstSignature();
            transaction.setSrcSignature(dsign);
            transaction.setDstSignature(ssign);
        }
    }

    public String toString(boolean withBalance, boolean withTransactions) {
        String s = "";
        s += "Ledger :\n";
        if (withBalance) { s += "\tBalance = " + this.balance + "\n"; }
        if (withTransactions) { s += this.transactions.toString(false); }
        return s;
    }

    public boolean equals(Ledger ledger){

        if (!ledger.getID().equals(this.getID())) return false;
        if (ledger.getBalance() != this.getBalance()) return false;
        if (!ledger.getTransactionList().equals(this.getTransactionList())) return false;
        return true;
    }
}