import sun.security.x509.*;

import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Scanner;
import java.util.UUID;


public class SecurityManager {

    protected KeyPair keypair;
    protected KeyStore keyStore;
    protected char[] password;
    protected String ksPath;

    protected PublicKey getPublicKey() {
        return keypair.getPublic();
    }

    protected PrivateKey getPrivateKey() {
        return keypair.getPrivate();
    }


    public void loadKeyPair(int ID, String path, String entity) throws IOException, GeneralSecurityException {
        checkKeyStorage(path);
        String alias = entity + Integer.toString(ID);

        Key key = keyStore.getKey(alias, password);
        if (key == null) {
            KeyPair newPair = createKeyPair();
            X509Certificate certificate = generateCertificate(newPair, ID);
            java.security.cert.Certificate[] certChain = new Certificate[1];
            certChain[0] = certificate;
            FileOutputStream fileOutputStream = new FileOutputStream(path + "keystore.ks");
            keyStore.setKeyEntry(alias, newPair.getPrivate(), password, certChain);
            keyStore.store(fileOutputStream, password);
            fileOutputStream.close();
            this.keypair = newPair;
        } else {
            PublicKey publicKey = keyStore.getCertificate(alias).getPublicKey();
            this.keypair = new KeyPair(publicKey, (PrivateKey) key);
        }
    }


    public byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(obj);
        return out.toByteArray();
    }

    public byte[] hashBytes(Object obj)
            throws IOException, NoSuchAlgorithmException
    {
        byte[] message = this.serialize(obj);
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(message);
        return messageDigest.digest();
    }

    public byte[] signMessage(byte[] message, PrivateKey privateKey) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(message);
        return signature.sign();
    }

    public boolean isSignatureValid(byte[] message, byte[] sign, PublicKey publicKey) throws NoSuchAlgorithmException,
            InvalidKeyException, SignatureException {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        signature.update(message);
        return signature.verify(sign);
    }

    // HELPERS ----------------------------------------------------------------------------------------------------

    protected void checkKeyStorage(String path) throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException {
        this.ksPath = path + "keystore.ks";
        keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        if (!(new File(ksPath).exists())) {
            String password = UUID.randomUUID().toString().replaceAll("-","");
            System.out.println("[SM] Generated KeyStorage password: " + password);

            FileOutputStream fileOutputStream = new FileOutputStream(ksPath);
            keyStore.load(null, null);
            keyStore.store(fileOutputStream, password.toCharArray());
            this.password = password.toCharArray();
        } else {
            Scanner scanner = new Scanner(System.in);
            System.out.println("[SM] Enter KeyStorage password: ");
            String prompt = scanner.nextLine();
            keyStore.load(new FileInputStream(ksPath), prompt.toCharArray());
            this.password = prompt.toCharArray();
        }
    }

    protected KeyPair createKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048, new SecureRandom());
        return keyGen.genKeyPair();
    }

    protected X509Certificate generateCertificate(KeyPair keyPair, int clientID) throws GeneralSecurityException, IOException {
        X500Name distinguishedName = new X500Name("client" + Integer.toString(clientID), "HDS", "IST", "PT");
        PrivateKey privateKey = keyPair.getPrivate();
        X509CertInfo info = new X509CertInfo();
        Date from = new Date();
        Date to = new Date(from.getTime() + 365 * 86400000L);
        CertificateValidity interval = new CertificateValidity(from, to);
        BigInteger sn = new BigInteger(64, new SecureRandom());
        AlgorithmId algo = new AlgorithmId(AlgorithmId.md5WithRSAEncryption_oid);

        info.set(X509CertInfo.VALIDITY, interval);
        info.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(sn));
        info.set(X509CertInfo.SUBJECT, distinguishedName);
        info.set(X509CertInfo.ISSUER, distinguishedName);
        info.set(X509CertInfo.KEY, new CertificateX509Key(keyPair.getPublic()));
        info.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));
        info.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId((algo)));

        X509CertImpl cert = new X509CertImpl(info);
        cert.sign(privateKey, "SHA1withRSA");

        algo = (AlgorithmId)cert.get(X509CertImpl.SIG_ALG);
        info.set(CertificateAlgorithmId.NAME + "." + CertificateAlgorithmId.ALGORITHM, algo);
        cert = new X509CertImpl(info);
        cert.sign(privateKey, "SHA1withRSA");
        return cert;
    }

}