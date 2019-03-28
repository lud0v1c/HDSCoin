import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;



public class ClientSecurityManager extends SecurityManager {

    // SINGLETON
    // ----------------------------------------------
    private static ClientSecurityManager instance = null;

    protected ClientSecurityManager() { }

    public static ClientSecurityManager getInstance() {
        if (instance == null) {
            instance = new ClientSecurityManager();
        }
        return instance;
    }
    // ----------------------------------------------

    public PublicKey getClientPubKey(int clientID) throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException{
        keyStore.load(new FileInputStream(ksPath), password);
        String alias = "client" + Integer.toString(clientID);
        if (keyStore.containsAlias(alias)) { return keyStore.getCertificate(alias).getPublicKey(); }
        else { return null; }
    }

    public byte[] getNonce() {
        byte[] nonce = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(nonce);
        return nonce;
    }


}