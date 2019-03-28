import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class CommunicationManager {

    protected List<RemoteServerInterface> remoteServers;
    protected final int faults = 1;
    protected long readID = 0;
    protected long writeID = 0;

    protected CommunicationManager() {}

    protected CommunicationManager(List<RemoteServerInterface> servers) {
        this.remoteServers = servers;
    }

    protected Object[] doReadOperation(RemoteServerInterface server, String methodName, Object[] message)
            throws IOException, NoSuchAlgorithmException, SignatureException, InvalidKeyException
    {

        Object[] response;
        switch (methodName) {
            case "checkAccount":
                response = server.checkAccount(message);
                break;
            case "audit":
                response = server.audit(message);
                break;
            case "getTransaction":
                response = server.getTransaction(message);
                break;
            default:
                response = null;
                break;
        }
        return response;
    }

    protected Object[] doWriteOperation(RemoteServerInterface server, String methodName, Object[] message)
            throws IOException, NoSuchAlgorithmException, SignatureException, InvalidKeyException,
            CertificateException, KeyStoreException
    {

        Object[] response;
        switch (methodName) {
            case "register":
                response = server.register(message);
                break;
            case "sendAmount":
                response = server.sendAmount(message);
                break;
            case "receiveAmount":
                response = server.receiveAmount(message);
                break;
            case "writeBack":
                response = server.writeBackPhase(message);
                break;
            default:
                response = null;
                break;
        }
        return response;
    }

    protected Object[] joinToMessage(Object[] message, Object newObject) {
        ArrayList<Object> temp = new ArrayList<>(Arrays.asList(message));
        temp.add(newObject);
        return temp.toArray();
    }

    protected void removeServer(int index){
        this.remoteServers.remove(index);
    }

    abstract Object[] read(SecurityManager secMan, String methodName, Object[] message) throws Exception;
    abstract Object[] write(SecurityManager secMan, String methodName, Object[] message) throws Exception;

}
