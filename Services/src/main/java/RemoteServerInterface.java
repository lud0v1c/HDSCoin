import java.io.IOException;
import java.rmi.Remote;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.CertificateException;


public interface RemoteServerInterface extends Remote {

    Object[] register(Object[] data) throws IOException, NoSuchAlgorithmException, InvalidKeyException, SignatureException, CertificateException, KeyStoreException;
    Object[] sendAmount(Object[] data) throws IOException, NoSuchAlgorithmException, SignatureException, InvalidKeyException;
    Object[] checkAccount(Object[] data) throws IOException, NoSuchAlgorithmException, SignatureException, InvalidKeyException;
    Object[] receiveAmount(Object[] data) throws IOException, NoSuchAlgorithmException, SignatureException, InvalidKeyException;
    Object[] audit(Object[] data) throws IOException, NoSuchAlgorithmException, SignatureException, InvalidKeyException;

    Object[] getTransaction(Object[] data) throws IOException, NoSuchAlgorithmException, SignatureException, InvalidKeyException;
    Object[] writeBackPhase(Object[] data) throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException, SignatureException, InvalidKeyException;

}