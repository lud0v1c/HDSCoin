package transactions;

import javafx.util.Pair;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.Base64;


/**
 *  The IntermediateTransaction class represents the
 *  intermediate form of a transaction, being an extension
 *  of the BasicTransaction due to having the signature
 *  of the transaction source client.
 *
 *  The IntermediateTransaction class cannot be accessed outside
 *  the transactions package.
 */
public class IntermediateTransaction implements Serializable {

    private final BasicTransaction transaction;
    private byte[] srcSignature;


    protected IntermediateTransaction (Pair<PublicKey, Integer> srcClientId, Pair<PublicKey,
            Integer> dstClientId, int coins)
    {
        this.transaction = new BasicTransaction(srcClientId, dstClientId, coins);
    }

    protected String getID() { return this.transaction.getID(); }

    protected void setID(String ID) { this.transaction.setID(ID); }

    protected PublicKey getSrcClientID() { return this.transaction.getSrcClientID(); }

    protected PublicKey getDstClientID() { return this.transaction.getDstClientID(); }

    protected int getAmount() { return this.transaction.getAmount(); }

    protected BasicTransaction getBasicTransaction() { return this.transaction; }

    protected byte[] getSrcSignature() { return this.srcSignature; }

    protected void setSrcSignature(byte[] signature) { this.srcSignature =  signature; }


    protected boolean compareTo(Transaction transaction) {
        return this.transaction.compareTo(transaction);
    }


    public String toString() {
        String s = this.transaction.toString();
        boolean srcSigned = (this.srcSignature != null);
        s += "\t\t-> SENDER\t \tSigned? " + (srcSigned ? "Yes ; " : "No  ; ");
        if (srcSigned) {
            s += "Signature = " + new String(Base64.getEncoder().encode(this.srcSignature), 0, 45);
            s += "[...]";
        }
        s += " ;\n";
        return s;
    }
}