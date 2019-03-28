import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class ServerCommunicationManager extends CommunicationManager{

    // SINGLETON
    // ----------------------------------------------
    private static ServerCommunicationManager instance;

    public static ServerCommunicationManager getInstance() {
        if (instance == null) {
            instance = new ServerCommunicationManager();
        }
        return instance;
    }

    private ServerCommunicationManager() {}
    // ----------------------------------------------

    /**
     *  Collection that for each client public key keeps WTS and SIG
     */
    private HashMap<PublicKey, Object[]> clientCommunicationInfo = new HashMap<>();

    public void updateClientCommunicationInfo(PublicKey pk, long writeID, byte[] signature) {
        this.clientCommunicationInfo.put(pk, new Object[]{writeID, signature});
    }

    public Object[] read(SecurityManager secMan, String methodName, Object[] message) {
        return (new Object[]{false});
    }

    public Object[] readDeliverEnd(Object[] receivedMessage, Object[] responseMessage) {
        ArrayList<Object> tempMessage = new ArrayList<>(Arrays.asList(receivedMessage));
        long readID = (long) tempMessage.get(tempMessage.size()-1);
        PublicKey pk = (PublicKey) tempMessage.get(0);

        if (!this.clientCommunicationInfo.containsKey(pk)) {
            this.updateClientCommunicationInfo(pk, 0, null);
        }

        if (this.clientCommunicationInfo.get(pk) != null) {
            long wid = (long) this.clientCommunicationInfo.get(pk)[0];
            byte[] signature = (byte[]) this.clientCommunicationInfo.get(pk)[1];

            responseMessage= this.joinToMessage(responseMessage, readID);
            responseMessage = this.joinToMessage(responseMessage, wid);
            responseMessage = this.joinToMessage(responseMessage, signature);
        }
        return responseMessage;
    }

    public Object[] write(SecurityManager secMan, String methodName, Object[] message) {
        return (new Object[]{false});
    }

    public boolean writeDeliverBegin(Object[] message) {
        boolean hasSucceeded = false;
        ArrayList<Object> tempMessage = new ArrayList<>(Arrays.asList(message));
        byte[] signature = (byte[]) tempMessage.get(tempMessage.size()-1);
        long wid = (long) tempMessage.get(tempMessage.size()-2);
        PublicKey pk = (PublicKey) tempMessage.get(0);

        if (!this.clientCommunicationInfo.containsKey(pk)) {
            this.updateClientCommunicationInfo(pk, 0, null);
        }
        if (wid > ((long)this.clientCommunicationInfo.get(pk)[0])) {
            this.updateClientCommunicationInfo(pk, wid, signature);
            hasSucceeded = true;
        }
        return hasSucceeded;
    }

    public Object[] writeDeliverEnd(Object[] receivedMessage, Object[] responseMessage) {
        ArrayList<Object> tempMessage = new ArrayList<>(Arrays.asList(receivedMessage));
        long wid = (long) tempMessage.get(tempMessage.size()-2);
        return this.joinToMessage(responseMessage, wid);
    }

    // HELPER
    public void removeSignature(PublicKey publicKey){
        this.updateClientCommunicationInfo(publicKey, 0, null);
    }

}
