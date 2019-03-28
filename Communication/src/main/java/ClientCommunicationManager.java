import transactions.Transaction;

import java.rmi.ConnectException;
import java.security.PublicKey;
import java.util.*;

public class ClientCommunicationManager extends CommunicationManager{

    // Singleton
    // ------------------------------------------------------------------------------------------------
    private static ClientCommunicationManager instance;

    public static ClientCommunicationManager getInstance(List<RemoteServerInterface> servers) {
        if (instance == null) {
            instance = new ClientCommunicationManager(servers);
        }
        return instance;
    }

    private ClientCommunicationManager(List<RemoteServerInterface> servers) {

        super(servers);
    }

    /// ------------------------------------------------------------------------------------------------

    public Object[] read(SecurityManager secMan, String methodName, Object[] message) throws Exception
    {
        Object[] finalReadResponse;
        int i = 0;
        try {

            PublicKey pk = (PublicKey) message[0];

            this.readID = this.readID + 1;
            message = this.joinToMessage(message, this.readID);

            List<Object[]> readList = new ArrayList<>();
            for ( ; i < this.remoteServers.size(); i++) {

                Object[] readResponse = this.doReadOperation(this.remoteServers.get(i), methodName, message);

                ArrayList<Object> tempReadResponse = new ArrayList<>(Arrays.asList(readResponse));
                if (tempReadResponse.size() >= 5){
                    byte[] signature = (byte[]) tempReadResponse.remove(tempReadResponse.size()-1);
                    long wid = (long) tempReadResponse.remove(tempReadResponse.size()-1);
                    long rid = (long) tempReadResponse.remove(tempReadResponse.size()-1);
                    readResponse = tempReadResponse.toArray();

                    if (rid == this.readID) {
                        if (!methodName.equals("getTransaction")) {
                            Ledger ledger = (Ledger) readResponse[0];
                            Object[] basicLedger = ledger.getBasicLedger();
                            Object[] signed = new Object[]{basicLedger, wid};

                            // If you start the program and do a read operation, signature is null
                            if(signature == null)
                            {
                                readList.add(readResponse);
                            }
                            else if(secMan.isSignatureValid(secMan.serialize(signed), signature, pk) &&
                                    checkOrder(secMan, ledger)){

                                readList.add(readResponse);
                            }

                        } else {
                            readList.add(readResponse);
                        }
                    }
                }

            }

            if (!methodName.equals("getTransaction")) {
                if (readList.size() > ((this.remoteServers.size()+this.faults)/2)) {
                    List<Boolean> acks = new ArrayList<>();
                    finalReadResponse = this.highestValue(readList);

                    List<byte[]> signatures = this.getQuorumSignature(finalReadResponse, readList, methodName);
                    Object[] wbWrite = {finalReadResponse, signatures, methodName};

                    i = 0;
                    for(; i < this.remoteServers.size(); i++){

                        Object[] wbResult = this.doWriteOperation(this.remoteServers.get(i), "writeBack", wbWrite);

                        if ((boolean) wbResult[0]) acks.add(true);

                    }

                    if (!(acks.size() > ((this.remoteServers.size()+this.faults)/2))) finalReadResponse = null;

                } else {
                    finalReadResponse = null;
                }
            } else {
                finalReadResponse = readList.get(0);
            }

            return finalReadResponse;

        } catch (ConnectException e) {

            System.out.println("WARNING : Server number " + i + " failed! Client operation was repeated ...");
            this.removeServer(i);
            return this.read(secMan, methodName, message);

        } catch (Exception e) {

            throw new Exception("Exception " + e.getClass().getSimpleName() + " occurred when attempted " +
                    "to connect with server in port " + i + "!", e);

        }
    }

    public Object[] write(SecurityManager secMan, String methodName, Object[] message) throws Exception
    {
        Object[] finalWriteResponse;
        int i = 0;
        try {

            this.writeID = this.writeID + 1;
            message = this.joinToMessage(message, this.writeID);

            Object[] basicLedger = ((Ledger) message[1]).getBasicLedger();
            Object[] toSign = new Object[]{basicLedger, this.writeID};
            byte[] signature = secMan.signMessage(secMan.serialize(toSign), secMan.getPrivateKey());
            message = this.joinToMessage(message, signature);

            List<Object[]> ackList = new ArrayList<>();
            List<Object[]> nonAckList = new ArrayList<>();
            for ( ; i < this.remoteServers.size(); i++) {

                Object[] writeResponse = this.doWriteOperation(this.remoteServers.get(i), methodName, message);

                if ((boolean) writeResponse[0]) {
                    ackList.add(writeResponse);
                } else {
                    nonAckList.add(writeResponse);
                }
            }

            if (ackList.size() > ((this.remoteServers.size()+this.faults)/2)) {
                finalWriteResponse = ackList.get(0);
            } else {
                finalWriteResponse = nonAckList.get(0);
            }
            return finalWriteResponse;

        } catch (ConnectException e) {

            System.out.println("WARNING : Server number " + i + " failed! Client operation was repeated ...");
            this.removeServer(i);
            return this.read(secMan, methodName, message);

        } catch (Exception e) {

            throw new Exception("Exception " + e.getClass().getSimpleName() + " occurred when attempted " +
                    "to connect with server in port " + i + "!", e);

        }
    }

    // Helpers
    // ------------------------------------------------------------------------------------------------------

    private boolean checkOrder(SecurityManager secMan, Ledger ledger) throws Exception {
        boolean result = true;
        TransactionList transactionList = ledger.getTransactionList();
        for (int i = 0, j = 1 ; j < transactionList.getSize() ; i++, j++) {
            Transaction previous = transactionList.getTransaction(i);
            Transaction current = transactionList.getTransaction(j);

            byte[] previousHash = secMan.hashBytes(previous);
            if (!Arrays.equals(previousHash, current.getHash())){
                result = false;
                break;
            }
        }
        return result;
    }

    private Object[] highestValue(List<Object[]> readList) {
        Object[] finalReadResponse = null;

        // Get for each response the size of the corresponding transaction list
        HashMap<Object[], Integer> readResponseSizeInfo = new HashMap<>();
        for (Object[] readResponse : readList){
            readResponseSizeInfo.put(readResponse,
                    ((Ledger)readResponse[0]).getTransactionList().getSize());
        }

        // Get for each transaction list size the number of its occurrences
        HashMap<Integer, Integer> readResponseOccurrenceInfo = new HashMap<>();
        for (Object[] readResponse : readList){
            int size = ((Ledger)readResponse[0]).getTransactionList().getSize();
            if (!readResponseOccurrenceInfo.containsKey(size)) {
                readResponseOccurrenceInfo.put(size, 1);
            } else {
                readResponseOccurrenceInfo.put(size, (readResponseOccurrenceInfo.get(size))+1);
            }
        }

        // Get transaction list sizes with occurrences greater than (N+f)/2
        HashMap<Integer, Integer> readResponseOccurrenceBestInfo = new HashMap<>();
        for (Map.Entry<Integer, Integer> occurrences : readResponseOccurrenceInfo.entrySet()) {
            if (occurrences.getValue() > ((this.remoteServers.size()+this.faults)/2)) {
                readResponseOccurrenceBestInfo.put(occurrences.getKey(), occurrences.getValue());
            }
        }

        // Find the transaction list that satisfies the above condition and has maximum size
        int maximumSize = Collections.max(readResponseOccurrenceBestInfo.keySet());

        // Find response with the transaction list that satisfies both previous conditions
        for (Map.Entry<Object[], Integer> readResponseInfo : readResponseSizeInfo.entrySet()) {
            if (readResponseInfo.getValue() == maximumSize){
                finalReadResponse = readResponseInfo.getKey();
                break;
            }
        }

        return finalReadResponse;
    }

    private List<byte[]> getQuorumSignature(Object[] finalResponse, List<Object[]> readList, String method){

        List<byte[]> result = new ArrayList<>();
        int i = 0;

        Ledger ledger = (Ledger) finalResponse[0];
        for (; i < readList.size(); i++){
            Ledger replyLedger = (Ledger) readList.get(i)[0];
            if (ledger.equals(replyLedger)){

                if (method.equals("audit")){
                    result.add((byte[]) finalResponse[1]);
                }

                else if (method.equals("checkAccount")){
                    result.add((byte[]) finalResponse[2]);
                }
            }

        }

        return result;
    }

}
