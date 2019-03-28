
import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.*;


public class ServerSecurityManager extends SecurityManager {

    // SINGLETON
    // ----------------------------------------------
    private static ServerSecurityManager instance = null;

    protected ServerSecurityManager() {
    }

    public static ServerSecurityManager getInstance() {
        if (instance == null) {
            instance = new ServerSecurityManager();
        }
        return instance;
    }
    // ----------------------------------------------


    /**
     *  The noncesCollection structure maps each client with the
     *  correspondent list of NONCES.
     *
     *  The noncesCollection structure belongs to the SecurityManager
     *  class given that it is strictly necessary for security reasons.
     */
    private HashMap<PublicKey, List<byte[]>> noncesCollection = new HashMap<>();

    public PublicKey getServerPubKey(int serverID) throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException{
        keyStore.load(new FileInputStream(ksPath), password);
        String alias = "server" + Integer.toString(serverID);
        return keyStore.getCertificate(alias).getPublicKey();
    }

    public HashMap<PublicKey, List<byte[]>> getNoncesCollection() {
        return this.noncesCollection;
    }

    public void initNoncesCollection(HashMap<PublicKey, List<byte[]>> collection) {
        this.noncesCollection = collection;
    }

    public void updateNoncesCollection(PublicKey pk, List<byte[]> nonceList) {
        this.noncesCollection.put(pk, nonceList);
    }

    public boolean containsPublicKey(PublicKey pk) {
        return noncesCollection.containsKey(pk);
    }

    public boolean hasNonce(PublicKey pk, byte[] nonce){
        List<byte[]> nonceList = noncesCollection.get(pk);
        for (byte[] aux : nonceList) {
            if (Arrays.equals(aux, nonce)){
                return false;
            }
        }
        return true;
    }

    public boolean decomposeMessage(byte[] receivedHash, byte[] computedHash, byte[] signedHash, PublicKey clientID)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException
    {
        boolean result = true;
        if(receivedHash.length == 0 && !isSignatureValid(receivedHash, signedHash, clientID)){
            System.out.println("Signature Verification");
            result = false;
        }

        if (!Arrays.equals(computedHash,receivedHash)){
            System.out.println("Hash Verification failed");
            result = false;
        }

        return result;
    }


}