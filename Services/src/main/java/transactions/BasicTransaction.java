package transactions;

import javafx.util.Pair;

import java.io.Serializable;
import java.security.PublicKey;


/**
 *  The BasicTransaction class represents the most basic
 *  form of a transaction, just with the core attributes
 *  of a transaction (id, source, destination, amount),
 *  without any signature.
 *
 *  The BasicTransaction class cannot be accessed outside
 *  the transactions package.
 */
public class BasicTransaction implements Serializable {

    private String ID;
    private Pair<PublicKey, Integer> srcClientID;
    private Pair<PublicKey, Integer> dstClientID;
    private int amount;


    protected BasicTransaction (Pair<PublicKey, Integer> srcClientId, Pair<PublicKey,
            Integer> dstClientId, int coins)
    {
        this.srcClientID = srcClientId;
        this.dstClientID = dstClientId;
        this.amount = coins;

    }

    protected String getID() { return this.ID; }

    protected void setID(String ID) { this.ID = ID; }

    protected PublicKey getSrcClientID() { return this.srcClientID.getKey(); }

    protected PublicKey getDstClientID() { return this.dstClientID.getKey(); }

    protected int getAmount() { return this.amount; }


    protected boolean compareTo(Transaction transaction) {
        return this.ID.matches(transaction.getID());
    }


    public String toString() {
        String s = "";
        s += "TransactionID = " + this.ID + " ; ";
        s += "Sender = " + this.srcClientID.getValue() + " ; ";
        s += "Receiver = " + this.dstClientID.getValue() + " ; ";
        s += "Amount = " + this.amount + " ;\n";
        return s;
    }
}