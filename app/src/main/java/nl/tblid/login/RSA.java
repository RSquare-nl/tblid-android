package nl.tblid.login;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyInfo;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.security.auth.x500.X500Principal;

public class RSA {
    public static final String KEY_ALGORITHM = "RSA";
    public static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    private static final String PUBLIC_KEY = "TBLIDPublicKey";
    private static final String PRIVATE_KEY = "TBLIDPrivateKey";

    private static final String DEFAULT_KEYSTORE = "AndroidKeyStore";
    private static final String DEFAULT_ALIAS = "TBLID_RSA";
    private final String keystoreName;
    private final String keyAlias;

    private Certificate certificate;
    private PrivateKey privateKey;

    private static final String TAG = RSA.class.getSimpleName();

    /**
     * Create a new Cloud IoT Authentication wrapper using the default keystore and alias.
     */
    public RSA() {
        this(DEFAULT_KEYSTORE, DEFAULT_ALIAS);
    }

    /**
     * Create a new Cloud IoT Authentication wrapper using the specified keystore and alias (instead
     * of the defaults)
     *
     * @param keystoreName The keystore to load
     * @param keyAlias the alias in the keystore for TBLID_RSA
     */
    public RSA(String keystoreName, String keyAlias) {
        this.keystoreName = keystoreName;
        this.keyAlias = keyAlias;
    }

    public void initialize() {
        try {
            KeyStore ks = KeyStore.getInstance(keystoreName);
            ks.load(null);

            certificate = ks.getCertificate(keyAlias);
            if (certificate == null) {
                // generate key
                Log.w(TAG, "No IoT Auth Certificate found, generating new cert");
                generateAuthenticationKey();
                certificate = ks.getCertificate(keyAlias);
            }
            Log.i(TAG, "loaded certificate: " + keyAlias);

            if (certificate instanceof X509Certificate) {
                X509Certificate x509Certificate = (X509Certificate) certificate;
                Log.d(TAG, "Subject: " + x509Certificate.getSubjectX500Principal().toString());
                Log.d(TAG, "Issuer: " + x509Certificate.getIssuerX500Principal().toString());
                //Log.d(TAG, "Signature: " + BaseEncoding.base16().lowerCase().withSeparator(":", 2)
                   //     .encode(x509Certificate.getSignature()));
            }

            Key key = ks.getKey(keyAlias, null);
            privateKey = (PrivateKey) key;
            boolean keyIsInSecureHardware = false;
            try {
                KeyFactory factory = KeyFactory.getInstance(privateKey.getAlgorithm(), keystoreName);
                KeyInfo keyInfo = factory.getKeySpec(privateKey, KeyInfo.class);
                keyIsInSecureHardware = keyInfo.isInsideSecureHardware();
                Log.d(TAG, "able to confirm if key is secured or not");
            } catch (GeneralSecurityException e) {
                // ignored
            }
            Log.i(TAG, "Key is in secure hardware? " + keyIsInSecureHardware);


        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Failed to open keystore", e);
        }

    }

    /**
     * Generate a new RSA key pair entry in the Android Keystore by
     * using the KeyPairGenerator API. This creates both a KeyPair
     * and a self-signed certificate, both with the same alias
     */
    private void generateAuthenticationKey() throws GeneralSecurityException {

        KeyPairGenerator kpg = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_RSA, keystoreName);
        kpg.initialize(new KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_SIGN)
                .setKeySize(2048)
                .setCertificateSubject(new X500Principal("CN=unused"))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                .build());

        kpg.generateKeyPair();
    }

    /**
     * Exports the authentication certificate to a file.
     *
     * @param destination the file to write the certificate to (PEM encoded)
     */
/*    public void exportPublicKey(File destination) throws IOException, GeneralSecurityException {
        FileOutputStream os = new FileOutputStream(destination);
        os.write(getCertificatePEM().getBytes());
        os.flush();
        os.close();
    }
*/
    public Certificate getCertificate() {
        return certificate;
    }

    /**
     * Returns the PEM-format encoded certificate
     */
    public String getCertificatePEM() throws GeneralSecurityException {
        StringBuilder sb = new StringBuilder();
        sb.append("-----BEGIN CERTIFICATE-----\n");
        sb.append(Base64.encodeToString(certificate.getEncoded(), Base64.DEFAULT));
        sb.append("-----END CERTIFICATE-----\n");
        return sb.toString();
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

}