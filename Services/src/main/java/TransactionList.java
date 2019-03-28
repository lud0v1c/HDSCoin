import transactions.BasicTransaction;
import transactions.Transaction;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


public class TransactionList implements Serializable{

    private LinkedList<Transaction> list = new LinkedList<>();

    public LinkedList<Transaction> getList() { return this.list; }

    public int getSize() { return this.list.size(); }

    public boolean isEmpty() {
        return this.list.isEmpty();
    }

    public Transaction getTransaction(int index) { return this.list.get(index); }

    public synchronized Transaction searchTransaction(String transactionID) {
        Transaction result = null;
        for (Transaction transaction : this.list) {
            if (transaction.getID().equals(transactionID)) {
                result = transaction;
                break;
            }
        }
        return result;
    }

    public List<BasicTransaction> getBasicTransactions() {
        List<BasicTransaction> basicTransactionList = new ArrayList<>();
        for (Transaction transaction : this.list){
            basicTransactionList.add(transaction.getBasicTransaction());
        }
        return basicTransactionList;
    }

    public synchronized TransactionList searchTransactionsByDstClient(PublicKey clientID) {
        TransactionList result = null;
        for (Transaction transaction : this.list){
            if (transaction.getDstClientID().equals(clientID)){
                if (result == null) { result = new TransactionList(); }
                result.update(transaction);
            }
        }
        return result;
    }

    public synchronized void update(Transaction transaction) {
        this.list.addLast(transaction);
    }

    public synchronized void update(TransactionList transactionList) {
        this.list.addAll(transactionList.getList());
    }

    public synchronized void substitute(Transaction transaction) {
        int index = -1;
        boolean hasTransaction = false;
        for (Transaction trans : this.list){
            index++;
            if (trans.getID().equals(transaction.getID())){
                hasTransaction = true;
                break;
            }
        }
        if (hasTransaction) {
            this.list.set(index, transaction);
        }
    }

    public synchronized void substitute(int index, Transaction transaction) {
        for(int i = 0; i < this.list.size(); i++) {
            if (i == index) {
                this.list.set(i, transaction);
                break;
            }
        }
    }

    public synchronized void removeLast(){
        this.list.remove(this.list.size() -1);
    }

    public Transaction getLastUpdate() { return this.list.getLast(); }

    public static synchronized TransactionList getTransactionsDifference(TransactionList a, TransactionList b) {
        TransactionList result = null;
        LinkedList<Transaction> aList = a.getList();
        LinkedList<Transaction> bList = b.getList();
        for (Transaction aTrans : aList){
            boolean hasTrans = false;
            for (Transaction bTrans : bList){
                if (aTrans.compareTo(bTrans)){
                    hasTrans = true;
                    break;
                }
            }
            if (!hasTrans){
                if (result == null) { result = new TransactionList(); }
                result.update(aTrans);
            }
        }
        return result;
    }


    public String toString(boolean hasPending){
        StringBuilder sb = new StringBuilder();
        if (hasPending) { sb.append("Pending Transactions :\n"); }
        else { sb.append("Completed Transactions :\n");}
        for(Transaction transaction : list){
            sb.append("\t> ");
            sb.append(transaction.toString());
        }
        return sb.toString();
    }

    public boolean equals(TransactionList transactions){

        if (this.getList().size() != transactions.getList().size()) return false;

        for (int i = 0; i < this.getList().size(); i++){
            if (!this.getList().get(i).compareTo(transactions.getList().get(i))) return false;
            if (!Arrays.equals(this.getList().get(i).getSrcSignature(), transactions.getList().get(i).getSrcSignature()))
            {
                return false;
            }
            if (!Arrays.equals(this.getList().get(i).getDstSignature(), transactions.getList().get(i).getDstSignature()))
            {
                return false;
            }
        }
        return true;
    }
}