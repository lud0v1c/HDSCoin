package transactions;

import javafx.util.Pair;
import java.io.Serializable;
import java.security.PublicKey;
import java.util.Base64;


/**
 *  The Transaction class represents the completed form of a
 *  transaction, being an extension of the FinalTransaction
 *  due to having the hash of the previous transaction in the
 *  client ledger, as means, to provided order guarantees.
 *
 *  The Transaction is the only class in transactions package
 *  visible to the rest of the application.
 */
public class Transaction implements Serializable{

    private final IntermediateTransaction transaction;
    private byte[] dstSignature;
    private byte[] hash;

    public Transaction (Pair<PublicKey, Integer> srcClientId, Pair<PublicKey, Integer> dstClientId,
                        int coins, byte[] previousTransaction)
    {
        this.transaction = new IntermediateTransaction(srcClientId, dstClientId, coins);
        this.hash = previousTransaction;
    }

    public String getID() { return this.transaction.getID(); }

    public void setID(String ID) { this.transaction.setID(ID); }

    public PublicKey getSrcClientID() { return this.transaction.getSrcClientID(); }

    public PublicKey getDstClientID() { return this.transaction.getDstClientID(); }

    public int getAmount() { return this.transaction.getAmount(); }

    public BasicTransaction getBasicTransaction() { return this.transaction.getBasicTransaction(); }

    public byte[] getSrcSignature() { return this.transaction.getSrcSignature(); }

    public void setSrcSignature(byte[] signature) { this.transaction.setSrcSignature(signature); }

    public IntermediateTransaction getIntermediateTransaction() { return this.transaction; }

    public byte[] getDstSignature() { return this.dstSignature; }

    public void setDstSignature(byte[] signature) { this.dstSignature =  signature; }

    public byte[] getHash() { return this.hash; }

    public void setHash(byte[] previousTransaction) { this.hash = previousTransaction; }


    public boolean compareTo(Transaction transaction) {
        return this.transaction.compareTo(transaction);
    }


    public String toString() {
        String s = this.transaction.toString();
        boolean dstSigned = (this.dstSignature != null);
        s += "\t\t-> RECEIVER  \tSigned? " + (dstSigned ? "Yes ; " : "No ; ");
        if (dstSigned) {
            s += "Signature = " + new String(Base64.getEncoder().encode(this.dstSignature), 0, 45);
            s += "[...]";
        }
        s += " .\n";
        return s;
    }

}