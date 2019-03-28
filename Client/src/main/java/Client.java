import javafx.util.Pair;
import transactions.Transaction;
import java.security.*;
import java.util.UUID;


public class Client {

    private PublicKey ID;
    private ClientCommunicationManager comMan;
    private ClientSecurityManager secMan;
    private static final int initialBalance = 100;

    protected Client(PublicKey id, ClientCommunicationManager com, ClientSecurityManager sec) {
        this.ID = id;
        this.comMan = com;
        this.secMan = sec;
    }

    /***
     * Format of messages send to the server: M{public key, ledger, client number, nonce}, H{hash(M)}, signature(H)
     */
    public boolean register(int clientNum) throws Exception
    {
        Ledger ledger = new Ledger(this.ID, Client.initialBalance);

        byte[] nonce = secMan.getNonce();
        Object dataTmp[] = {this.ID, ledger, clientNum, nonce};
        byte[] hash = secMan.hashBytes(dataTmp);
        byte[] signature = secMan.signMessage(hash, secMan.getPrivateKey());
        Object[] message = {this.ID, ledger, clientNum, nonce, hash, signature};

        Object[] response = this.comMan.write(secMan,"register", message);
        return (boolean) response[0];
    }


    /***
     * Format of messages send to the server: M{public key,ledger,nonce} + H{hash(M)} + signature(H)
     */
    public boolean sendAmount(Pair<PublicKey, Integer> pair, int destination, int amount) throws Exception
    {

        boolean hasSucceed = false;

        if (!this.ID.equals(pair.getKey())){

            Object[] data = {this.ID};
            byte[] hash = secMan.hashBytes(data);
            Object[] message = {this.ID, hash};

            Object[] response = this.comMan.read(secMan, "audit", message);
            Ledger ledger = (Ledger) response[0];

            byte[] previousTransaction;
            if (ledger.getTransactionList().isEmpty()){ previousTransaction = null; }
            else { previousTransaction= secMan.hashBytes(ledger.getTransactionList().getLastUpdate()); }

            ledger.setBalance(ledger.getBalance()-amount);
            Transaction newTransaction = new Transaction(new Pair<>(this.ID, destination), pair, amount, previousTransaction);
            this.assignTransactionID(newTransaction);
            newTransaction.setSrcSignature(secMan.signMessage(secMan.serialize(newTransaction.getBasicTransaction()),
                    secMan.getPrivateKey()));
            ledger.updateTransactionList(newTransaction);

            byte[] nonce = secMan.getNonce();
            Object[] data2 = {this.ID, ledger, nonce};
            byte[] hash2 = secMan.hashBytes(data2);
            byte[] signature = secMan.signMessage(hash2, secMan.getPrivateKey());
            Object[] message2 = {this.ID, ledger, nonce, hash2, signature};

            Object[] response2 = this.comMan.write(secMan,"sendAmount", message2);
            hasSucceed = (boolean) response2[0];
        }

        return hasSucceed;
    }

    /***
     * Format of messages send to the server: M{public key} + H{hash(M)}
     */
    public Object[] checkAccount() throws Exception
    {
        Object[] data = {this.ID};
        byte[] hash = secMan.hashBytes(data);
        Object[] message = {this.ID, hash};

        Object[] response = this.comMan.read(secMan,"checkAccount", message);
        return response;
    }

    /***
     * Format of messages send to the server: M{public key} + H{hash(M)}
     */
    public Object[] checkAccount(PublicKey dstClientID) throws Exception
    {
        Object[] data = {dstClientID};
        byte[] hash = secMan.hashBytes(data);
        Object[] message = {dstClientID, hash};

        Object[] response = this.comMan.read(secMan,"checkAccount", message);
        return response;
    }

    /***
     * Format of messages send to the server: M{public key,transaction,nonce} + H{hash(M)} + signature(H)
     */
    public boolean receiveAmount(String transactionID) throws Exception
    {

        boolean hasSucceed = false;

        Object[] data = {this.ID};
        byte[] hash = secMan.hashBytes(data);
        Object[] message = {this.ID, hash};

        Object[] response = this.comMan.read(secMan,"audit", message);
        Ledger ledger = (Ledger) response[0];

        byte[] nonce = secMan.getNonce();
        Object[] data2 = {this.ID, transactionID, nonce};
        byte[] hash2 = secMan.hashBytes(data2);
        byte[] signature = secMan.signMessage(hash2, secMan.getPrivateKey());
        Object[] message2 = {this.ID, transactionID, nonce, hash2, signature};

        Object[] response2 = this.comMan.read(secMan,"getTransaction", message2);
        Transaction transaction = (Transaction) response2[0];

        if (transaction != null){

            byte[] previousTransaction;
            if (ledger.getTransactionList().isEmpty()){ previousTransaction = null; }
            else { previousTransaction= secMan.hashBytes(ledger.getTransactionList().getLastUpdate()); }

            ledger.setBalance(ledger.getBalance()+transaction.getAmount());
            transaction.setDstSignature(secMan.signMessage(secMan.serialize(transaction.getIntermediateTransaction()),
                    secMan.getPrivateKey()));
            transaction.setHash(previousTransaction);
            ledger.updateTransactionList(transaction);

            byte[] nonce2 = secMan.getNonce();
            Object[] data3 = {this.ID, ledger, nonce2};
            byte[] hash3 = secMan.hashBytes(data3);
            byte[] signature2 = secMan.signMessage(hash3, secMan.getPrivateKey());
            Object[] message3 = {this.ID, ledger, nonce2, hash3, signature2};

            Object[] response3 = this.comMan.write(secMan,"receiveAmount", message3);
            hasSucceed = (boolean) response3[0];
        }
        return hasSucceed;
    }

    /***
     * Format of messages send to the server: M{public key} + H{hash(M)}
     */
    public Ledger audit() throws Exception
    {
        Object[] data = {this.ID};
        byte[] hash = secMan.hashBytes(data);
        Object[] message = {this.ID, hash};

        Object[] response = this.comMan.read(secMan,"audit", message);
        return (Ledger)response[0];
    }

    /***
     * Format of messages send to the server: M{public key} + H{hash(M)}
     */
    public Ledger audit(PublicKey dstClientID) throws Exception
    {
        Object[] data = {dstClientID};
        byte[] hash = secMan.hashBytes(data);
        Object[] message = {dstClientID, hash};

        Object[] response = this.comMan.read(secMan,"audit", message);
        return (Ledger)response[0];
    }


    // Helpers
    // ---------------------------------------------------------------------------------------------
    private synchronized void assignTransactionID(Transaction transaction) {
        String transID;
        transID = UUID.randomUUID().toString();
        transaction.setID(transID);
    }

}