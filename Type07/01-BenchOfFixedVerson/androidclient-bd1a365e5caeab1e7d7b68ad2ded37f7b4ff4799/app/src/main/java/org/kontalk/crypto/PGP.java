/*
 * Kontalk Android client
 * Copyright (C) 2017 Kontalk Devteam <devteam@kontalk.org>

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.kontalk.crypto;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.spec.ECGenParameterSpec;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

import org.jxmpp.util.XmppStringUtils;
import org.spongycastle.bcpg.ArmoredInputStream;
import org.spongycastle.bcpg.BCPGInputStream;
import org.spongycastle.bcpg.BCPGKey;
import org.spongycastle.bcpg.DSASecretBCPGKey;
import org.spongycastle.bcpg.ECSecretBCPGKey;
import org.spongycastle.bcpg.ElGamalSecretBCPGKey;
import org.spongycastle.bcpg.HashAlgorithmTags;
import org.spongycastle.bcpg.PublicKeyPacket;
import org.spongycastle.bcpg.RSASecretBCPGKey;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.openpgp.PGPEncryptedData;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPKeyPair;
import org.spongycastle.openpgp.PGPKeyRingGenerator;
import org.spongycastle.openpgp.PGPObjectFactory;
import org.spongycastle.openpgp.PGPPrivateKey;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.PGPSignatureGenerator;
import org.spongycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.spongycastle.openpgp.PGPSignatureSubpacketVector;
import org.spongycastle.openpgp.PGPUtil;
import org.spongycastle.openpgp.operator.KeyFingerPrintCalculator;
import org.spongycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.spongycastle.openpgp.operator.PBESecretKeyEncryptor;
import org.spongycastle.openpgp.operator.PGPDigestCalculator;
import org.spongycastle.openpgp.operator.PGPDigestCalculatorProvider;
import org.spongycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPKeyConverter;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPKeyPair;
import org.spongycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcePBESecretKeyEncryptorBuilder;

import android.os.Parcel;

import org.kontalk.util.MessageUtils;


/** Some PGP utility method, mainly for use by {@link PersonalKey}. */
public class PGP {

    /** Security provider: Spongy Castle. */
    public static Provider PROVIDER;

    /** Default EC curve used. */
    private static final String EC_CURVE = "P-256";

    /** Default RSA key length used. */
    private static final int RSA_KEY_LENGTH = 2048;

    /** Singleton for converting a PGP key to a JCA key. */
    private static JcaPGPKeyConverter sKeyConverter;

    static KeyFingerPrintCalculator sFingerprintCalculator =
        new BcKeyFingerprintCalculator();

    private PGP() {
    }

    public static final class PGPDecryptedKeyPairRing {
        /* Authentication key. */
        PGPKeyPair authKey;
        /* Signing key. */
        PGPKeyPair signKey;
        /* Encryption key. */
        PGPKeyPair encryptKey;

        public PGPDecryptedKeyPairRing(PGPKeyPair auth, PGPKeyPair sign, PGPKeyPair encrypt) {
            this.authKey = auth;
            this.signKey = sign;
            this.encryptKey = encrypt;
        }
    }

    public static final class PGPKeyPairRing {
        public PGPPublicKeyRing publicKey;
        public PGPSecretKeyRing secretKey;

        PGPKeyPairRing(PGPPublicKeyRing publicKey, PGPSecretKeyRing secretKey) {
            this.publicKey = publicKey;
            this.secretKey = secretKey;
        }

        public static PGPKeyPairRing load(byte[] privateKeyData, byte[] publicKeyData)
            throws IOException, PGPException {
            PGPPublicKeyRing publicKey = new PGPPublicKeyRing(publicKeyData, sFingerprintCalculator);
            PGPSecretKeyRing secretKey = new PGPSecretKeyRing(privateKeyData, sFingerprintCalculator);
            return new PGPKeyPairRing(publicKey, secretKey);
        }

        public static PGPKeyPairRing loadArmored(byte[] privateKeyData, byte[] publicKeyData)
                throws IOException, PGPException {
            ArmoredInputStream inPublic = new ArmoredInputStream(new ByteArrayInputStream(publicKeyData));
            PGPPublicKeyRing publicKey = new PGPPublicKeyRing(inPublic, sFingerprintCalculator);
            ArmoredInputStream inPrivate = new ArmoredInputStream(new ByteArrayInputStream(privateKeyData));
            PGPSecretKeyRing secretKey = new PGPSecretKeyRing(inPrivate, sFingerprintCalculator);
            return new PGPKeyPairRing(publicKey, secretKey);
        }
    }

    public static void registerProvider() {
        // create spongy castle provider
        // do not register it as can cause issues on some devices
        PROVIDER = new BouncyCastleProvider();
        try {
            // apply RNG fixes
            PRNGFixes.apply();
        }
        catch (Exception e) {
            throw new PRNGFixException("Unable to apply PRNG fix", e);
        }
    }

    public static final class PRNGFixException extends SecurityException {
        PRNGFixException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /** Creates an ECDSA/ECDH key pair. */
    public static PGPDecryptedKeyPairRing create(Date timestamp)
            throws NoSuchAlgorithmException, NoSuchProviderException, PGPException, InvalidAlgorithmParameterException {

        KeyPairGenerator gen;
        PGPKeyPair authKp, encryptKp, signKp;

        gen = KeyPairGenerator.getInstance("RSA", PROVIDER);
        gen.initialize(RSA_KEY_LENGTH);

        authKp = new JcaPGPKeyPair(PGPPublicKey.RSA_GENERAL, gen.generateKeyPair(), timestamp);

        gen = KeyPairGenerator.getInstance("ECDH", PROVIDER);
        gen.initialize(new ECGenParameterSpec(EC_CURVE));

        encryptKp = new JcaPGPKeyPair(PGPPublicKey.ECDH, gen.generateKeyPair(), timestamp);

        gen = KeyPairGenerator.getInstance("ECDSA", PROVIDER);
        gen.initialize(new ECGenParameterSpec(EC_CURVE));

        signKp = new JcaPGPKeyPair(PGPPublicKey.ECDSA, gen.generateKeyPair(), timestamp);

        return new PGPDecryptedKeyPairRing(authKp, signKp, encryptKp);
    }

    /** Creates public and secret keyring for a given keypair. */
    public static PGPKeyPairRing store(PGPDecryptedKeyPairRing pair,
            String id,
            String passphrase)
        throws PGPException, IOException {

        PGPSignatureSubpacketGenerator sbpktGen;

        // some hashed subpackets for the key
        sbpktGen = new PGPSignatureSubpacketGenerator();
        // the master key is used for authentication and certification
        sbpktGen.setKeyFlags(false, PGPKeyFlags.CAN_AUTHENTICATE | PGPKeyFlags.CAN_CERTIFY);
        sbpktGen.setPrimaryUserID(false, true);

        PGPDigestCalculator digestCalc = new JcaPGPDigestCalculatorProviderBuilder().build().get(HashAlgorithmTags.SHA1);
        PGPKeyRingGenerator keyRingGen = new PGPKeyRingGenerator(PGPSignature.POSITIVE_CERTIFICATION, pair.authKey,
            id, digestCalc, sbpktGen.generate(), null,
            new JcaPGPContentSignerBuilder(pair.authKey.getPublicKey().getAlgorithm(), HashAlgorithmTags.SHA256),
            new JcePBESecretKeyEncryptorBuilder(PGPEncryptedData.AES_256, digestCalc)
                .setProvider(PROVIDER).build(passphrase.toCharArray()));

        // add signing subkey
        sbpktGen = new PGPSignatureSubpacketGenerator();
        sbpktGen.setKeyFlags(false, PGPKeyFlags.CAN_SIGN);
        sbpktGen.setEmbeddedSignature(false, crossCertify(pair.signKey, pair.authKey.getPublicKey()));
        keyRingGen.addSubKey(pair.signKey, sbpktGen.generate(), null);

        // add encryption subkey
        sbpktGen = new PGPSignatureSubpacketGenerator();
        sbpktGen.setKeyFlags(false, PGPKeyFlags.CAN_ENCRYPT_COMMS);
        keyRingGen.addSubKey(pair.encryptKey, sbpktGen.generate(), null);

        PGPSecretKeyRing secRing = keyRingGen.generateSecretKeyRing();
        PGPPublicKeyRing pubRing = keyRingGen.generatePublicKeyRing();

        return new PGPKeyPairRing(pubRing, secRing);
    }

    /** Generates a cross-certification for a subkey. */
    private static PGPSignature crossCertify(PGPKeyPair signer, PGPPublicKey key) throws PGPException {
        PGPSignatureGenerator sGen = new PGPSignatureGenerator(
            new JcaPGPContentSignerBuilder(signer.getPublicKey().getAlgorithm(),
                PGPUtil.SHA256).setProvider(PROVIDER));
        sGen.init(PGPSignature.PRIMARYKEY_BINDING, signer.getPrivateKey());
        return sGen.generateCertification(key);
    }

    /** Revokes the given key. */
    public static PGPPublicKey revokeKey(PGPKeyPair secret)
            throws PGPException, IOException, SignatureException {

        PGPPrivateKey pgpPrivKey = secret.getPrivateKey();
        PGPPublicKey pgpPubKey = secret.getPublicKey();

        PGPSignatureGenerator       sGen = new PGPSignatureGenerator(
            new JcaPGPContentSignerBuilder(secret.getPublicKey().getAlgorithm(),
                PGPUtil.SHA256).setProvider(PROVIDER));

        sGen.init(PGPSignature.KEY_REVOCATION, pgpPrivKey);

        return PGPPublicKey.addCertification(pgpPubKey, sGen.generateCertification(pgpPubKey));
    }

    public static PGPDecryptedKeyPairRing fromParcel(Parcel in) throws PGPException, IOException {
        PGPKeyPair authKp = readKeyPairFromParcel(in);
        PGPKeyPair signKp = readKeyPairFromParcel(in);
        PGPKeyPair encryptKp = readKeyPairFromParcel(in);
        return new PGPDecryptedKeyPairRing(authKp, signKp, encryptKp);
    }

    public static void toParcel(PGPDecryptedKeyPairRing pair, Parcel dest)
            throws NoSuchProviderException, PGPException, IOException {
        writeKeyPairToParcel(pair.authKey, dest);
        writeKeyPairToParcel(pair.signKey, dest);
        writeKeyPairToParcel(pair.encryptKey, dest);
    }

    private static void writeKeyPairToParcel(PGPKeyPair keypair, Parcel dest) throws IOException {
        // write private key
        PGPPrivateKey priv = keypair.getPrivateKey();
        // key ID
        dest.writeLong(priv.getKeyID());
        // private key data packet
        byte[] privData = priv.getPrivateKeyDataPacket().getEncoded();
        dest.writeInt(privData.length);
        dest.writeByteArray(privData);
        // public key packet
        byte[] privPubKey = priv.getPublicKeyPacket().getEncoded();
        dest.writeInt(privPubKey.length);
        dest.writeByteArray(privPubKey);

        // write public key
        PGPPublicKey pub = keypair.getPublicKey();
        // whole public key
        byte[] pubPacket = pub.getEncoded();
        dest.writeInt(pubPacket.length);
        dest.writeByteArray(pubPacket);
    }

    private static PGPKeyPair readKeyPairFromParcel(Parcel in) throws IOException, PGPException {
        BCPGInputStream reader = null;
        long privID;
        BCPGKey privData;
        PublicKeyPacket privPubKey;

        // rebuild private key

        try {
            // key ID
            privID = in.readLong();

            // private key data packet
            int privDataLength = in.readInt();
            byte[] privDataData = new byte[privDataLength];
            in.readByteArray(privDataData);
            // object will be read later

            // public key packet
            int privPubKeyLength = in.readInt();
            byte[] privPubKeyData = new byte[privPubKeyLength];
            in.readByteArray(privPubKeyData);

            reader = new BCPGInputStream(new ByteArrayInputStream(privPubKeyData));
            privPubKey = (PublicKeyPacket) reader.readPacket();
            reader.close();

            reader = new BCPGInputStream(new ByteArrayInputStream(privDataData));

            // from PGPSecretKey
            // apparently there is no way to make this dynamic
            switch (privPubKey.getAlgorithm())
            {
                case PGPPublicKey.RSA_ENCRYPT:
                case PGPPublicKey.RSA_GENERAL:
                case PGPPublicKey.RSA_SIGN:
                    privData = new RSASecretBCPGKey(reader);
                    break;
                case PGPPublicKey.DSA:
                    privData = new DSASecretBCPGKey(reader);
                    break;
                case PGPPublicKey.ELGAMAL_ENCRYPT:
                case PGPPublicKey.ELGAMAL_GENERAL:
                    privData = new ElGamalSecretBCPGKey(reader);
                    break;
                case PGPPublicKey.ECDH:
                case PGPPublicKey.ECDSA:
                    privData = new ECSecretBCPGKey(reader);
                    break;
                default:
                    throw new PGPException("unknown public key algorithm encountered");
            }

            reader.close();
        }
        finally {
            try {
                if (reader != null)
                    reader.close();
            }
            catch (IOException ignored) {
            }
        }

        PGPPrivateKey priv = new PGPPrivateKey(privID, privPubKey, privData);

        // rebuild public key
        // public key data
        int pubKeyLength = in.readInt();
        byte[] pubKeyData = new byte[pubKeyLength];
        in.readByteArray(pubKeyData);

        PGPObjectFactory f = new PGPObjectFactory(pubKeyData, sFingerprintCalculator);
        PGPPublicKeyRing pubring = (PGPPublicKeyRing) f.nextObject();
        PGPPublicKey pub = pubring.getPublicKey();

        return new PGPKeyPair(pub, priv);
    }

    public static void serialize(PGPDecryptedKeyPairRing pair, ObjectOutputStream dest)
            throws PGPException, IOException {

        PrivateKey privAuth = convertPrivateKey(pair.authKey.getPrivateKey());
        PublicKey pubAuth = convertPublicKey(pair.authKey.getPublicKey());
        int algoAuth = pair.authKey.getPrivateKey().getPublicKeyPacket().getAlgorithm();
        Date dateAuth = pair.authKey.getPrivateKey().getPublicKeyPacket().getTime();

        PrivateKey privSign = convertPrivateKey(pair.signKey.getPrivateKey());
        PublicKey pubSign = convertPublicKey(pair.signKey.getPublicKey());
        int algoSign = pair.signKey.getPrivateKey().getPublicKeyPacket().getAlgorithm();
        Date dateSign = pair.signKey.getPrivateKey().getPublicKeyPacket().getTime();

        PrivateKey privEnc = convertPrivateKey(pair.encryptKey.getPrivateKey());
        PublicKey pubEnc = convertPublicKey(pair.encryptKey.getPublicKey());
        int algoEnc = pair.encryptKey.getPrivateKey().getPublicKeyPacket().getAlgorithm();
        Date dateEnc = pair.encryptKey.getPrivateKey().getPublicKeyPacket().getTime();

        dest.writeObject(privAuth);
        dest.writeObject(pubAuth);
        dest.writeInt(algoAuth);
        dest.writeLong(dateAuth.getTime());

        dest.writeObject(privSign);
        dest.writeObject(pubSign);
        dest.writeInt(algoSign);
        dest.writeLong(dateSign.getTime());

        dest.writeObject(privEnc);
        dest.writeObject(pubEnc);
        dest.writeInt(algoEnc);
        dest.writeLong(dateEnc.getTime());
    }

    public static PGPDecryptedKeyPairRing unserialize(ObjectInputStream in)
            throws IOException, ClassNotFoundException, PGPException {
        ensureKeyConverter();

        // TODO read byte data

        PrivateKey privAuth = (PrivateKey) in.readObject();
        PublicKey pubAuth = (PublicKey) in.readObject();
        int algoAuth = in.readInt();
        Date dateAuth = new Date(in.readLong());

        PGPPublicKey pubKeyAuth = sKeyConverter.getPGPPublicKey(algoAuth, pubAuth, dateAuth);
        PGPPrivateKey privKeyAuth = sKeyConverter.getPGPPrivateKey(pubKeyAuth, privAuth);
        PGPKeyPair authKp = new PGPKeyPair(pubKeyAuth, privKeyAuth);

        PrivateKey privSign = (PrivateKey) in.readObject();
        PublicKey pubSign = (PublicKey) in.readObject();
        int algoSign = in.readInt();
        Date dateSign = new Date(in.readLong());

        PGPPublicKey pubKeySign = sKeyConverter.getPGPPublicKey(algoSign, pubSign, dateSign);
        PGPPrivateKey privKeySign = sKeyConverter.getPGPPrivateKey(pubKeySign, privSign);
        PGPKeyPair signKp = new PGPKeyPair(pubKeySign, privKeySign);

        PrivateKey privEnc = (PrivateKey) in.readObject();
        PublicKey pubEnc = (PublicKey) in.readObject();
        int algoEnc = in.readInt();
        Date dateEnc = new Date(in.readLong());

        PGPPublicKey pubKeyEnc = sKeyConverter.getPGPPublicKey(algoEnc, pubEnc, dateEnc);
        PGPPrivateKey privKeyEnc = sKeyConverter.getPGPPrivateKey(pubKeyEnc, privEnc);
        PGPKeyPair encryptKp = new PGPKeyPair(pubKeyEnc, privKeyEnc);

        return new PGPDecryptedKeyPairRing(authKp, signKp, encryptKp);
    }

    public static String getFingerprint(PGPPublicKey publicKey) {
        return MessageUtils.bytesToHex(publicKey.getFingerprint()).toUpperCase(Locale.US);
    }

    public static String getFingerprint(byte[] publicKeyring) throws IOException, PGPException {
        return getFingerprint(getMasterKey(publicKeyring));
    }

    // FIXME very ugly method
    public static String formatFingerprint(String fingerprint) {
        StringBuilder fpr = new StringBuilder();
        int length = fingerprint.length();
        for (int i = 0; i < length; i += 4) {
            fpr.append(fingerprint.substring(i, i + 4));
            if (i < (length - 4)) {
                fpr.append(' ');
                if (i == (length / 2 - 4))
                    fpr.append(' ');
            }
        }
        return fpr.toString();
    }

    public static String createFingerprintURI(String fingerprint) {
        return "openpgp4fpr:" + fingerprint;
    }

    /** Returns the first user ID on the key that matches the given hostname. */
    // TODO return type should be PGPUserID
    public static String getUserId(PGPPublicKey key, String host) {
        String first = null;

        @SuppressWarnings("unchecked")
        Iterator<String> uids = key.getUserIDs();
        while (uids.hasNext()) {
            String uid = uids.next();
            // save the first if everything else fails
            if (first == null) {
                first = uid;
                // no host to verify, exit now
                if (host == null)
                    break;
            }

            if (uid != null) {
                // parse uid
                PGPUserID parsed = PGPUserID.parse(uid);
                if (parsed != null) {
                    String email = parsed.getEmail();
                    if (email != null) {
                        // check if email host name matches
                        if (host.equalsIgnoreCase(XmppStringUtils.parseDomain(email))) {
                            return uid;
                        }
                    }
                }
            }
        }

        return first;
    }

    /** Returns the first user ID on the key that matches the given hostname. */
    // TODO return type should be PGPUserID
    public static String getUserId(byte[] publicKeyring, String host) throws IOException, PGPException {
        PGPPublicKey pk = getMasterKey(publicKeyring);
        return getUserId(pk, host);
    }

    public static PGPUserID parseUserId(byte[] publicKeyring, String host) throws IOException, PGPException {
        return parseUserId(getMasterKey(publicKeyring), host);
    }

    public static PGPUserID parseUserId(PGPPublicKey key, String host) throws IOException, PGPException {
        String uid = getUserId(key, host);
        return PGPUserID.parse(uid);
    }

    public static int getKeyFlags(PGPPublicKey key) {
        @SuppressWarnings("unchecked")
        Iterator<PGPSignature> sigs = key.getSignatures();
        while (sigs.hasNext()) {
            PGPSignature sig = sigs.next();
            if (sig != null) {
                PGPSignatureSubpacketVector subpackets = sig.getHashedSubPackets();
                if (subpackets != null) {
                    return subpackets.getKeyFlags();
                }
            }
        }
        return 0;
    }

    /** Returns the first master key found in the given public keyring. */
    public static PGPPublicKey getMasterKey(PGPPublicKeyRing publicKeyring) {
        @SuppressWarnings("unchecked")
        Iterator<PGPPublicKey> iter = publicKeyring.getPublicKeys();
        while (iter.hasNext()) {
            PGPPublicKey pk = iter.next();
            if (pk.isMasterKey())
                return pk;
        }

        return null;
    }

    /** Returns the first master key found in the given public keyring. */
    public static PGPPublicKey getMasterKey(byte[] publicKeyring) throws IOException, PGPException {
        return getMasterKey(readPublicKeyring(publicKeyring));
    }

    public static PGPPublicKey getSigningKey(PGPPublicKeyRing publicKeyring) {
        @SuppressWarnings("unchecked")
        Iterator<PGPPublicKey> iter = publicKeyring.getPublicKeys();
        while (iter.hasNext()) {
            PGPPublicKey pk = iter.next();
            if (!pk.isMasterKey()) {
                int keyFlags = getKeyFlags(pk);
                if ((keyFlags & PGPKeyFlags.CAN_SIGN) == PGPKeyFlags.CAN_SIGN)
                    return pk;
            }
        }

        // legacy key format support
        return getLegacySigningKey(publicKeyring);
    }

    public static PGPPublicKey getEncryptionKey(PGPPublicKeyRing publicKeyring) {
        @SuppressWarnings("unchecked")
        Iterator<PGPPublicKey> iter = publicKeyring.getPublicKeys();
        while (iter.hasNext()) {
            PGPPublicKey pk = iter.next();
            if (!pk.isMasterKey()) {
                int keyFlags = getKeyFlags(pk);
                if ((keyFlags & PGPKeyFlags.CAN_ENCRYPT_COMMS) == PGPKeyFlags.CAN_ENCRYPT_COMMS)
                    return pk;

            }
        }

        // legacy key format support
        return getLegacyEncryptionKey(publicKeyring);
    }

    private static PGPPublicKey getLegacyEncryptionKey(PGPPublicKeyRing publicKeyring) {
        @SuppressWarnings("unchecked")
        Iterator<PGPPublicKey> iter = publicKeyring.getPublicKeys();
        while (iter.hasNext()) {
            PGPPublicKey pk = iter.next();
            if (!pk.isMasterKey() && pk.isEncryptionKey())
                return pk;
        }

        return null;
    }

    private static PGPPublicKey getLegacySigningKey(PGPPublicKeyRing publicKeyring) {
        @SuppressWarnings("unchecked")
        Iterator<PGPPublicKey> iter = publicKeyring.getPublicKeys();
        while (iter.hasNext()) {
            PGPPublicKey pk = iter.next();
            if (pk.isMasterKey())
                return pk;
        }

        return null;
    }

    public static PGPPublicKeyRing readPublicKeyring(byte[] publicKeyring) throws IOException, PGPException {
        PGPObjectFactory reader = new PGPObjectFactory(publicKeyring, sFingerprintCalculator);
        Object o = reader.nextObject();
        while (o != null) {
            if (o instanceof PGPPublicKeyRing)
                return (PGPPublicKeyRing) o;

            o = reader.nextObject();
        }

        throw new PGPException("invalid keyring data.");
    }

    private static void ensureKeyConverter() {
        if (sKeyConverter == null)
            sKeyConverter = new JcaPGPKeyConverter().setProvider(PGP.PROVIDER);
    }

    public static PrivateKey convertPrivateKey(PGPPrivateKey key) throws PGPException {
        ensureKeyConverter();
        return sKeyConverter.getPrivateKey(key);
    }

    @SuppressWarnings("unchecked")
    public static PrivateKey convertPrivateKey(byte[] privateKeyData, String passphrase)
            throws PGPException, IOException {

        PGPDigestCalculatorProvider digestCalc = new JcaPGPDigestCalculatorProviderBuilder().build();
        PBESecretKeyDecryptor decryptor = new JcePBESecretKeyDecryptorBuilder(digestCalc)
            .setProvider(PGP.PROVIDER)
            .build(passphrase.toCharArray());

        // load the secret key ring
        PGPSecretKeyRing secRing = new PGPSecretKeyRing(privateKeyData, sFingerprintCalculator);

        // search and decrypt the master (signing key)
        // secret keys
        Iterator<PGPSecretKey> skeys = secRing.getSecretKeys();
        while (skeys.hasNext()) {
            PGPSecretKey key = skeys.next();
            PGPSecretKey sec = secRing.getSecretKey();

            if (key.isMasterKey())
                return convertPrivateKey(sec.extractPrivateKey(decryptor));
        }

        throw new PGPException("no suitable private key found.");
    }

    public static PublicKey convertPublicKey(PGPPublicKey key) throws PGPException {
        ensureKeyConverter();
        return sKeyConverter.getPublicKey(key);
    }

    public static PGPSecretKeyRing copySecretKeyRingWithNewPassword(byte[] privateKeyData,
            String oldPassphrase, String newPassphrase) throws PGPException, IOException {

        // load the secret key ring
        PGPSecretKeyRing secRing = new PGPSecretKeyRing(privateKeyData, sFingerprintCalculator);

        return copySecretKeyRingWithNewPassword(secRing, oldPassphrase, newPassphrase);
    }

    public static PGPSecretKeyRing copySecretKeyRingWithNewPassword(PGPSecretKeyRing secRing,
            String oldPassphrase, String newPassphrase) throws PGPException {

        PGPDigestCalculatorProvider digestCalcProv = new JcaPGPDigestCalculatorProviderBuilder().build();
        PBESecretKeyDecryptor decryptor = new JcePBESecretKeyDecryptorBuilder(digestCalcProv)
            .setProvider(PGP.PROVIDER)
            .build(oldPassphrase.toCharArray());

        PGPDigestCalculator digestCalc = new JcaPGPDigestCalculatorProviderBuilder().build().get(HashAlgorithmTags.SHA256);
        PBESecretKeyEncryptor encryptor = new JcePBESecretKeyEncryptorBuilder(PGPEncryptedData.AES_256, digestCalc)
            .setProvider(PROVIDER).build(newPassphrase.toCharArray());

        return PGPSecretKeyRing.copyWithNewPassword(secRing, decryptor, encryptor);
    }

}
