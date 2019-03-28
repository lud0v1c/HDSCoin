import transactions.Transaction;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.*;

public class Server extends UnicastRemoteObject implements RemoteServerInterface{

    /**
     * Paths to all persistent files in server. Includes paths to backup files
     */

    private String serverDataPath = ServerApplication.appFolderPath + "src/main/" + "data";
    private final String clientAccountsPath = "clientAccounts.ser";
    private final String clientAccountsPathBackup = "clientAccountsB.ser";
    private final String clientNoncePath = "clientNonces.ser";
    private final String clientNoncePathBackup = "clientNoncesB.ser";



    /**
     *  To simplify user experience we assigned a int to every client,
     *  so the user only has to input the client ID and not the long
     *  public key. We store this information in the HashMap clientsIDs
     */
    private HashMap<PublicKey, Integer> clientAccounts = new HashMap<>();
    private HashMap<PublicKey, Ledger> clientLedgers = new HashMap<>();
    private static final int initialBalance = 100;
    private ServerSecurityManager secMan;
    private ServerCommunicationManager comMan;
    private int serverPort;
    private boolean[] byzantineAttacks = new boolean[4];
    private List<Integer> ports = new ArrayList<>();


    @SuppressWarnings("unchecked")
    protected Server(ServerSecurityManager sec, ServerCommunicationManager com, int ID) throws RemoteException {

        this.secMan = sec;
        this.comMan = com;
        this.serverPort = ID;
        ports.add(2000);
        ports.add(2001);
        ports.add(2002);
        ports.add(2003);

        if (new File(this.serverDataPath +this.serverPort + File.separator + this.clientAccountsPath).isFile()){
            this.clientAccounts = (HashMap<PublicKey, Integer>) this.retrieveData(this.serverDataPath
                    +this.serverPort + File.separator +this.clientAccountsPath);

            for (Map.Entry clientID : this.clientAccounts.entrySet()) {
                Ledger clientLedger = (Ledger) this.retrieveData(this.serverDataPath+this.serverPort + File.separator
                         + ((int) clientID.getValue()));
                this.clientLedgers.put(clientLedger.getID(), clientLedger);
            }
        }

        if (new File(this.serverDataPath +this.serverPort + File.separator + this.clientNoncePath).isFile()){
            secMan.initNoncesCollection((HashMap<PublicKey, List<byte[]>>) this.retrieveData(this.serverDataPath+
                    this.serverPort + File.separator+ this.clientNoncePath));
        }
    }

    // BYZANTINE METHODS

    public boolean getByzantineStatus(int attack) {
        return this.byzantineAttacks[attack];
    }

    public void setByzantine(int attack) {
        this.byzantineAttacks[attack] = !this.byzantineAttacks[attack];
    }

    private Ledger getTamperedLedger(PublicKey clientID) {
        Ledger ledger = searchClientLedger(clientID);
        if (this.byzantineAttacks[0]) {
            ledger.byzSwitchTransactions();
        }
        if (byzantineAttacks[1]) {
            ledger.byzRemoveLastTransaction();
        }
        if (byzantineAttacks[2]) {
            ledger.byzSwitchSignatures();
        }
        return ledger;
    }

    private synchronized TransactionList getTamperedPendingTransactions(PublicKey clientID) {
        TransactionList result = null;
        for (Map.Entry clientLedger : this.clientLedgers.entrySet()){
            if (!clientLedger.getKey().equals(clientID)){
                TransactionList potentialPendingTransactions =
                        ((Ledger)clientLedger.getValue()).getTransactionList().searchTransactionsByDstClient(clientID);
                if (potentialPendingTransactions != null) {
                    if (result == null) { result = new TransactionList(); }
                    result.update(potentialPendingTransactions);
                }
            }
        }
        return result;
    }

    // REMOTE METHODS ------------------------------------------------------------------------------------------------

    public synchronized Object[] register(Object[] data)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, IOException
    {
        boolean hasSucceed = false;
        PublicKey clientID = (PublicKey) data[0];

        boolean writeSucceed = this.comMan.writeDeliverBegin(data);
        if (writeSucceed) {

            Ledger ledger = (Ledger)data[1];
            int ID = (int)data[2];
            byte[] random = (byte[]) data[3];
            byte[] receivedHash = (byte[]) data[4];
            byte[] signedHash = (byte[]) data[5];
            Object tmp[] = {clientID, ledger, ID, random};
            byte[] computedHash = secMan.hashBytes(tmp);


            if (secMan.decomposeMessage(receivedHash, computedHash, signedHash, clientID)){

                if (!secMan.containsPublicKey(clientID)){
                    secMan.updateNoncesCollection(clientID, new ArrayList<>());
                }

                if (secMan.hasNonce(clientID, random)){

                    if (!this.existsClient(clientID)){
                        if (ledger.getID().equals(clientID) && ledger.getBalance() == Server.initialBalance &&
                                ledger.getTransactionList().isEmpty()) {
                            this.updateClientAccounts(clientID, ID);
                            this.updateClientLedger(clientID, ledger);
                            hasSucceed = true;
                        }
                    }

                }
            }

        }

        if (!hasSucceed) this.comMan.removeSignature(clientID);

        ServerMessages.processRegisterResult(hasSucceed, this.clientAccounts.get(clientID));
        Object[] tmpResult = {hasSucceed};
        byte[] signature = secMan.signMessage(secMan.serialize(tmpResult),secMan.getPrivateKey());
        Object[] result = {hasSucceed, signature};
        result = this.comMan.writeDeliverEnd(data, result);
        return result;
    }


    public synchronized Object[] sendAmount(Object[] data)
            throws IOException, NoSuchAlgorithmException, SignatureException, InvalidKeyException
    {

        boolean hasSucceed = false;
        PublicKey clientID = (PublicKey) data[0];

        boolean writeSucceed = this.comMan.writeDeliverBegin(data);
        if (writeSucceed) {

            Ledger newLedger = (Ledger)data[1];
            byte[] random = (byte[]) data[2];
            byte[] receivedHash = (byte[]) data[3];
            byte[] signedHash = (byte[]) data[4];
            Object tmp[] = {clientID, newLedger, random};
            byte[] computedHash = secMan.hashBytes(tmp);

            if (secMan.decomposeMessage(receivedHash, computedHash,signedHash,clientID) &&
                    this.existsClient(clientID) && secMan.hasNonce(clientID, random)){

                Ledger oldLedger = this.searchClientLedger(clientID);
                Transaction newTransaction = newLedger.getTransactionList().getLastUpdate();
                if (this.existsClient(newTransaction.getDstClientID())) {
                    if (this.validateTransactionSignature(true, oldLedger, newTransaction)) {
                        if (this.validateLedgerUpdate(true, oldLedger, newLedger, newTransaction)) {
                            this.updateClientLedger(clientID, newLedger);
                            hasSucceed = true;
                        }
                    }
                }
            }

        }
        if (!hasSucceed) this.comMan.removeSignature(clientID);

        ServerMessages.processSendAmountResult(hasSucceed, this.clientAccounts.get(clientID));
        Object tmpResult[] = {hasSucceed};
        byte[] signature = secMan.signMessage(secMan.serialize(tmpResult),secMan.getPrivateKey());
        Object result[] = {hasSucceed, signature};
        result = this.comMan.writeDeliverEnd(data, result);
        return result;
    }


    public synchronized Object[] checkAccount(Object[] data)
            throws IOException, NoSuchAlgorithmException, SignatureException, InvalidKeyException
    {

        boolean hasSucceed = false;
        boolean toAttack = false;
        PublicKey clientID = (PublicKey) data[0];
        byte[] hash = (byte[]) data[1];
        Object tmp[] = {data[0]};
        byte[] computedHash = secMan.hashBytes(tmp);

        Object[] result = null;
        Ledger ledger;
        TransactionList pendingTransactions;

        for(boolean i : byzantineAttacks) {
            if (i) {toAttack = true;}
        }
        if(secMan.decomposeMessage(hash, computedHash, null, null) && this.existsClient(clientID)){
            if (toAttack) {
                ledger = this.getTamperedLedger(clientID);
                if (!byzantineAttacks[3])
                {
                    ServerMessages.printFakeLedger("checkaccount",
                            ledger.toString(true, true), this.clientAccounts.get(clientID));
                }
            } else {
                ledger = this.searchClientLedger(clientID);
            }

            if (byzantineAttacks[3]) {
                pendingTransactions = this.getTamperedPendingTransactions(clientID);
                ServerMessages.printFakeTransactionList(pendingTransactions.toString(true), this.clientAccounts.get(clientID));
            } else {
                pendingTransactions = this.computePendingTransactions(clientID);

            }

            Object tmpResult[] = {ledger, pendingTransactions};
            byte[] signature = secMan.signMessage(secMan.serialize(tmpResult),secMan.getPrivateKey());
            result = new Object[3]; result[0] = ledger; result[1] = pendingTransactions; result[2] = signature;
            hasSucceed = true;

        }

        ServerMessages.processCheckAccountResult(hasSucceed, this.clientAccounts.get(clientID));
        if (result != null){ result = this.comMan.readDeliverEnd(data, result); }
        return result;
    }


    public synchronized Object[] receiveAmount(Object[] data)
            throws IOException, NoSuchAlgorithmException,SignatureException, InvalidKeyException
    {

        boolean hasSucceed = false;
        PublicKey clientID = (PublicKey) data[0];

        boolean writeSucceed = this.comMan.writeDeliverBegin(data);
        if (writeSucceed) {

            Ledger newLedger = (Ledger)data[1];
            byte[] random = (byte[]) data[2];
            byte[] receivedHash = (byte[]) data[3];
            byte[] signedHash = (byte[]) data[4];
            Object tmp[] = {clientID, newLedger, random};
            byte[] computedHash = secMan.hashBytes(tmp);


            if (secMan.decomposeMessage(receivedHash, computedHash, signedHash, clientID) &&
                    this.existsClient(clientID) && secMan.hasNonce(clientID, random)){

                Ledger oldLedger = this.searchClientLedger(clientID);
                Transaction newTransaction = newLedger.getTransactionList().getLastUpdate();
                if (this.isPendingTransaction(clientID, newTransaction)) {
                    if (this.validateTransactionSignature(false, oldLedger, newTransaction)) {
                        if (this.validateLedgerUpdate(false, oldLedger, newLedger, newTransaction)) {
                            this.updateClientLedger(clientID, newLedger);
                            Ledger srcLedger = this.searchClientLedger(newTransaction.getSrcClientID());
                            srcLedger.getTransactionList().substitute(newTransaction);
                            this.persistData(this.serverDataPath+this.serverPort + File.separator + this.clientAccounts.get(srcLedger.getID()),
                                    this.serverDataPath +this.serverPort + File.separator
                                            + this.clientAccounts.get(srcLedger.getID())+ "Backup",srcLedger);
                            hasSucceed = true;
                        }
                    }
                }
            }

        }

        if (!hasSucceed) this.comMan.removeSignature(clientID);

        ServerMessages.processReceiveAmountResult(hasSucceed, this.clientAccounts.get(clientID));
        Object tmpResult[] = {hasSucceed};
        byte[] signature = secMan.signMessage(secMan.serialize(tmpResult),secMan.getPrivateKey());
        Object result[] = {hasSucceed, signature};
        result = this.comMan.writeDeliverEnd(data, result);
        return result;
    }


    public synchronized Object[] audit(Object[] data)
            throws IOException, NoSuchAlgorithmException, SignatureException, InvalidKeyException
    {

        boolean hasSucceed = false;
        boolean toAttack = false;
        PublicKey clientID = (PublicKey) data[0];
        byte[] hash = (byte[]) data[1];
        Object tmp[] = {data[0]};
        byte[] computedHash = secMan.hashBytes(tmp);

        Object[] result = null;
        byte[] signature;

        for(boolean i : byzantineAttacks) {
            if (i) {toAttack = true;}
        }
        if(secMan.decomposeMessage(hash, computedHash, null, null) && this.existsClient(clientID)){

            if (toAttack) {
                Ledger tamperedLedger = this.getTamperedLedger(clientID);
                Object[] signed = {tamperedLedger};
                signature = secMan.signMessage(secMan.serialize(signed),secMan.getPrivateKey());
                result = new Object[2]; result[0] = tamperedLedger; result[1] = signature;
                ServerMessages.printFakeLedger("audit", tamperedLedger.toString(false, true), this.clientAccounts.get(clientID));
            } else {
                Object[] signed = {this.searchClientLedger(clientID)};
                signature = secMan.signMessage(secMan.serialize(signed),secMan.getPrivateKey());
                result = new Object[2]; result[0] = this.searchClientLedger(clientID); result[1] = signature;
            }


            hasSucceed = true;
        }


        ServerMessages.processAuditResult(hasSucceed, this.clientAccounts.get(clientID));
        if (result != null){ result = this.comMan.readDeliverEnd(data, result); }
        return result;
    }


    public synchronized Object[] getTransaction(Object[] data)
            throws IOException, NoSuchAlgorithmException, SignatureException, InvalidKeyException
    {

        PublicKey clientID = (PublicKey) data[0];
        String transactionID = (String) data[1];
        byte[] random = (byte[]) data[2];
        byte[] receivedHash = (byte[]) data[3];
        byte[] signedHash = (byte[]) data[4];
        Object tmp[] = {clientID,transactionID,random};
        byte[] computedHash = secMan.hashBytes(tmp);

        try {

            if (secMan.decomposeMessage(receivedHash, computedHash, signedHash, clientID) &&
                    secMan.hasNonce(clientID, random)){

                Object[] result = new Object[2];
                UUID.fromString(transactionID);
                Object tmpResult[] = {this.searchTransaction(transactionID)};
                byte[] signature = secMan.signMessage(secMan.serialize(tmpResult),secMan.getPrivateKey());
                result[0] = this.searchTransaction(transactionID); result[1] = signature;

                result = this.comMan.readDeliverEnd(data, result);
                return result;

            }
            return null;

        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public synchronized Object[] writeBackPhase(Object[] data)
            throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException,
            SignatureException, InvalidKeyException
    {
        Object[] result = {false};
        Object[] response = (Object[]) data[0];
        List<byte[]> signatures = (List<byte[]>)data[1];
        String method = (String) data[2];
        List<Boolean> valid = new ArrayList<>();

        if (method.equals("audit")){
            Ledger tmp = (Ledger) response[0];
            Object[] signed = {tmp};
            for (int port : this.ports) {
                for (byte[] sign : signatures){
                    if (secMan.isSignatureValid(secMan.serialize(signed), sign, secMan.getServerPubKey(port))){
                        valid.add(true);
                    }
                }
            }
        }

        if (method.equals("checkAccount")){
            Ledger tmp = (Ledger) response[0];
            TransactionList trans = (TransactionList) response[1];
            Object[] signed = {tmp, trans};
            for (int port : this.ports) {
                for (byte[] sign : signatures){
                    if (secMan.isSignatureValid(secMan.serialize(signed), sign, secMan.getServerPubKey(port))){
                        valid.add(true);
                    }
                }
            }
        }

        if (valid.size() >= 3) {
            Ledger ledger = (Ledger) response[0];
            this.updateClientLedger(ledger.getID(), ledger);
            result[0] = true;
        }

        return result;
    }

    // HELPERS ----------------------------------------------------------------------------------------------------

    private boolean existsClient(PublicKey clientID){
        return this.clientAccounts.containsKey(clientID);
    }

    private void updateClientAccounts(PublicKey clientID, int ID) {
        this.clientAccounts.put(clientID, ID);
        this.persistData(this.serverDataPath+this.serverPort + File.separator + this.clientAccountsPath,
                this.serverDataPath+this.serverPort + File.separator+this.clientAccountsPathBackup, this.clientAccounts);
        this.persistData(this.serverDataPath +this.serverPort + File.separator+ this.clientNoncePath,
                this.serverDataPath +this.serverPort + File.separator+ this.clientNoncePathBackup, secMan.getNoncesCollection());

    }

    private Ledger searchClientLedger(PublicKey clientID) {
        return this.clientLedgers.get(clientID);
    }

    private void updateClientLedger(PublicKey clientID, Ledger ledger) {
        this.clientLedgers.put(clientID, ledger);
        this.persistData(this.serverDataPath +this.serverPort + File.separator+ this.clientAccounts.get(ledger.getID()),
                this.serverDataPath+this.serverPort + File.separator+ this.clientAccounts.get(ledger.getID())+ "Backup",ledger);
    }

    private synchronized boolean validateLedgerUpdate(boolean isSend, Ledger oldLedger, Ledger newLedger, Transaction newTransaction) {

        boolean result;
        TransactionList oldTransactionList = oldLedger.getTransactionList();
        TransactionList newTransactionList = newLedger.getTransactionList();
        TransactionList difference = TransactionList.getTransactionsDifference(newTransactionList, oldTransactionList);
        result = newLedger.getID().equals(oldLedger.getID()) && newLedger.getBalance() >= 0 &&
                difference.getList().size() == 1;

        if (isSend) {
            result = result && newLedger.getBalance() == (oldLedger.getBalance()-newTransaction.getAmount()) &&
                    newTransaction.getSrcClientID().equals(newLedger.getID());
        } else {
            result = result && newLedger.getBalance() == (oldLedger.getBalance()+newTransaction.getAmount()) &&
                    newTransaction.getDstClientID().equals(newLedger.getID());
                    ;
        }
        return result;
    }

    private synchronized boolean validateTransactionSignature(boolean isSend, Ledger oldLedger, Transaction newTransaction)
            throws IOException, NoSuchAlgorithmException, InvalidKeyException, SignatureException
    {
        boolean result;
        if (isSend) {
            result = secMan.isSignatureValid(secMan.serialize(newTransaction.getBasicTransaction()),
                    newTransaction.getSrcSignature(), oldLedger.getID());
        } else {
            result = secMan.isSignatureValid(secMan.serialize(newTransaction.getIntermediateTransaction()),
                    newTransaction.getDstSignature(), oldLedger.getID());
        }
        return result;
    }

    private synchronized Transaction searchTransaction(String transactionID) {
        Transaction result = null;
        for (Map.Entry clientLedger : this.clientLedgers.entrySet()){
            result = ((Ledger)clientLedger.getValue()).getTransactionList().searchTransaction(transactionID);
            if (result != null) { break; }
        }
        return result;
    }

    private synchronized boolean isPendingTransaction(PublicKey clientID, Transaction transaction) {
        return clientID.equals(transaction.getDstClientID()) &&
                this.searchClientLedger(clientID).getTransactionList().searchTransaction(transaction.getID()) == null;
    }

    private synchronized TransactionList computePendingTransactions(PublicKey clientID) {
        TransactionList result = null;
        TransactionList clientTransactions = this.searchClientLedger(clientID).getTransactionList();
        for (Map.Entry clientLedger : this.clientLedgers.entrySet()){
            if (!clientLedger.getKey().equals(clientID)){
                TransactionList potentialPendingTransactions =
                        ((Ledger)clientLedger.getValue()).getTransactionList().searchTransactionsByDstClient(clientID);
                if (potentialPendingTransactions != null) {
                    TransactionList pendingTransactions =
                            TransactionList.getTransactionsDifference(potentialPendingTransactions, clientTransactions);
                    if (pendingTransactions != null) {
                        if (result == null) { result = new TransactionList(); }
                        result.update(pendingTransactions);
                    }
                }
            }
        }
        return result;
    }

    private void persistData(String path, String backupPath, Object obj) {
        try {

            // We need to create the original part in the first time we use the program
            if (!new File(path).exists()){
                FileOutputStream fileOut = new FileOutputStream(path);
                ObjectOutputStream objOut = new ObjectOutputStream(fileOut);
                objOut.writeObject(obj);
                objOut.close();
                fileOut.close();
            }

            // Write information to backup file
            FileOutputStream fileBackup = new FileOutputStream(backupPath);
            ObjectOutputStream objOutB = new ObjectOutputStream(fileBackup);
            objOutB.writeObject(obj);
            objOutB.close();
            fileBackup.close();

            // Rename backup file atomically
            Files.move(Paths.get(backupPath), Paths.get(path), StandardCopyOption.ATOMIC_MOVE);


        } catch (IOException e) {
            System.out.println("Error persisting system info");
            System.out.println(e.getMessage());
            System.exit(1);
        }
    }

    @SuppressWarnings("unchecked")
    private Object retrieveData(String path) {

        Object result = null;
        try {
            FileInputStream fileIn = new FileInputStream(path);
            ObjectInputStream objIn = new ObjectInputStream(fileIn);
            result = objIn.readObject();
            objIn.close();
            fileIn.close();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error retrieving persisted system info");
            System.out.println(e.getMessage());
            System.exit(1);
        }
        return result;
    }
}