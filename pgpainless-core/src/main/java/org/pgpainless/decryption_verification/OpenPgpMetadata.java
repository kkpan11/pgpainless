/*
 * Copyright 2018 Paul Schaub.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.pgpainless.decryption_verification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPSignature;
import org.pgpainless.algorithm.CompressionAlgorithm;
import org.pgpainless.algorithm.StreamEncoding;
import org.pgpainless.algorithm.SymmetricKeyAlgorithm;
import org.pgpainless.key.OpenPgpV4Fingerprint;
import org.pgpainless.key.SubkeyIdentifier;
import org.pgpainless.signature.DetachedSignature;
import org.pgpainless.signature.OnePassSignature;

public class OpenPgpMetadata {

    private final Set<Long> recipientKeyIds;
    private final SubkeyIdentifier decryptionKey;
    private final List<OnePassSignature> onePassSignatures;
    private final List<DetachedSignature> detachedSignatures;
    private final SymmetricKeyAlgorithm symmetricKeyAlgorithm;
    private final CompressionAlgorithm compressionAlgorithm;
    private final String fileName;
    private final Date modificationDate;
    private final StreamEncoding fileEncoding;

    public OpenPgpMetadata(Set<Long> recipientKeyIds,
                           SubkeyIdentifier decryptionKey,
                           SymmetricKeyAlgorithm symmetricKeyAlgorithm,
                           CompressionAlgorithm algorithm,
                           List<OnePassSignature> onePassSignatures,
                           List<DetachedSignature> detachedSignatures,
                           String fileName,
                           Date modificationDate,
                           StreamEncoding fileEncoding) {

        this.recipientKeyIds = Collections.unmodifiableSet(recipientKeyIds);
        this.decryptionKey = decryptionKey;
        this.symmetricKeyAlgorithm = symmetricKeyAlgorithm;
        this.compressionAlgorithm = algorithm;
        this.detachedSignatures = Collections.unmodifiableList(detachedSignatures);
        this.onePassSignatures = Collections.unmodifiableList(onePassSignatures);
        this.fileName = fileName;
        this.modificationDate = modificationDate;
        this.fileEncoding = fileEncoding;
    }

    /**
     * Return a set of key-ids the messages was encrypted for.
     *
     * @return recipient ids
     */
    public @Nonnull Set<Long> getRecipientKeyIds() {
        return recipientKeyIds;
    }

    /**
     * Return true, if the message was encrypted.
     *
     * @return true if encrypted, false otherwise
     */
    public boolean isEncrypted() {
        return symmetricKeyAlgorithm != SymmetricKeyAlgorithm.NULL && !getRecipientKeyIds().isEmpty();
    }

    /**
     * Return the {@link SubkeyIdentifier} of the key that was used to decrypt the message.
     * This can be null if the message was decrypted using a {@link org.pgpainless.util.Passphrase}, or if it was not
     * encrypted at all (eg. signed only).
     *
     * @return subkey identifier of decryption key
     */
    public @Nullable SubkeyIdentifier getDecryptionKey() {
        return decryptionKey;
    }

    /**
     * Return the algorithm that was used to symmetrically encrypt the message.
     *
     * @return encryption algorithm
     */
    public @Nullable SymmetricKeyAlgorithm getSymmetricKeyAlgorithm() {
        return symmetricKeyAlgorithm;
    }

    /**
     * Return the {@link CompressionAlgorithm} that was used to compress the message.
     *
     * @return compression algorithm
     */
    public @Nullable CompressionAlgorithm getCompressionAlgorithm() {
        return compressionAlgorithm;
    }

    /**
     * Return a set of all signatures on the message.
     * Note: This method returns just the signatures. There is no guarantee that the signatures are verified or even correct.
     *
     * Use {@link #getVerifiedSignatures()} instead to get all verified signatures.
     * @return unverified and verified signatures
     */
    public @Nonnull Set<PGPSignature> getSignatures() {
        Set<PGPSignature> signatures = new HashSet<>();
        for (DetachedSignature detachedSignature : detachedSignatures) {
            signatures.add(detachedSignature.getSignature());
        }
        for (OnePassSignature onePassSignature : onePassSignatures) {
            signatures.add(onePassSignature.getSignature());
        }
        return signatures;
    }

    /**
     * Return true if the message contained at least one signature.
     *
     * Note: This method does not reflect, whether the signature on the message is correct.
     * Use {@link #isVerified()} instead to determine, if the message carries a verifiable signature.
     *
     * @return true if message contains at least one unverified or verified signature, false otherwise.
     */
    public boolean isSigned() {
        return !getSignatures().isEmpty();
    }

    /**
     * Return a map of all verified signatures on the message.
     * The map contains verified signatures as value, with the {@link SubkeyIdentifier} of the key that was used to verify
     * the signature as the maps keys.
     *
     * @return verified detached and one-pass signatures
     */
    public Map<SubkeyIdentifier, PGPSignature> getVerifiedSignatures() {
        Map<SubkeyIdentifier, PGPSignature> verifiedSignatures = new ConcurrentHashMap<>();
        for (DetachedSignature detachedSignature : detachedSignatures) {
            if (detachedSignature.isVerified()) {
                verifiedSignatures.put(detachedSignature.getSigningKeyIdentifier(), detachedSignature.getSignature());
            }
        }
        for (OnePassSignature onePassSignature : onePassSignatures) {
            if (onePassSignature.isVerified()) {
                verifiedSignatures.put(onePassSignature.getSigningKey(), onePassSignature.getSignature());
            }
        }

        return verifiedSignatures;
    }

    /**
     * Return true, if the message is signed and at least one signature on the message was verified successfully.
     *
     * @return true if message is verified, false otherwise
     */
    public boolean isVerified() {
        return !getVerifiedSignatures().isEmpty();
    }

    /**
     * Return true, if the message contains at least one verified signature made by a key in the
     * given certificate.
     *
     * @param certificate certificate
     * @return true if message was signed by the certificate (and the signature is valid), false otherwise
     */
    public boolean containsVerifiedSignatureFrom(PGPPublicKeyRing certificate) {
        for (PGPPublicKey key : certificate) {
            OpenPgpV4Fingerprint fingerprint = new OpenPgpV4Fingerprint(key);
            if (containsVerifiedSignatureFrom(fingerprint)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return true, if the message contains at least one valid signature made by the key with the given
     * fingerprint, false otherwise.
     *
     * The fingerprint might be of the signing subkey, or the primary key of the signing certificate.
     *
     * @param fingerprint fingerprint of primary key or signing subkey
     * @return true if validly signed, false otherwise
     */
    public boolean containsVerifiedSignatureFrom(OpenPgpV4Fingerprint fingerprint) {
        for (SubkeyIdentifier verifiedSigningKey : getVerifiedSignatures().keySet()) {
            if (verifiedSigningKey.getPrimaryKeyFingerprint().equals(fingerprint) ||
                    verifiedSigningKey.getSubkeyFingerprint().equals(fingerprint)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return the name of the encrypted / signed file.
     *
     * @return file name
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Return true, if the encrypted data is intended for your eyes only.
     *
     * @return true if for-your-eyes-only
     */
    public boolean isForYourEyesOnly() {
        return PGPLiteralData.CONSOLE.equals(getFileName());
    }

    /**
     * Return the modification date of the encrypted / signed file.
     *
     * @return modification date
     */
    public Date getModificationDate() {
        return modificationDate;
    }

    /**
     * Return the encoding format of the encrypted / signed file.
     *
     * @return encoding
     */
    public StreamEncoding getFileEncoding() {
        return fileEncoding;
    }

    public static Builder getBuilder() {
        return new Builder();
    }

    public static class Builder {

        private final Set<Long> recipientFingerprints = new HashSet<>();
        private SubkeyIdentifier decryptionKey;
        private final List<DetachedSignature> detachedSignatures = new ArrayList<>();
        private final List<OnePassSignature> onePassSignatures = new ArrayList<>();
        private SymmetricKeyAlgorithm symmetricKeyAlgorithm = SymmetricKeyAlgorithm.NULL;
        private CompressionAlgorithm compressionAlgorithm = CompressionAlgorithm.UNCOMPRESSED;
        private String fileName;
        private StreamEncoding fileEncoding;
        private Date modificationDate;

        public Builder addRecipientKeyId(Long keyId) {
            this.recipientFingerprints.add(keyId);
            return this;
        }

        public Builder setDecryptionKey(SubkeyIdentifier decryptionKey) {
            this.decryptionKey = decryptionKey;
            return this;
        }

        public Builder setCompressionAlgorithm(CompressionAlgorithm algorithm) {
            this.compressionAlgorithm = algorithm;
            return this;
        }

        public List<DetachedSignature> getDetachedSignatures() {
            return detachedSignatures;
        }

        public Builder setSymmetricKeyAlgorithm(SymmetricKeyAlgorithm symmetricKeyAlgorithm) {
            this.symmetricKeyAlgorithm = symmetricKeyAlgorithm;
            return this;
        }

        public Builder setFileName(@Nonnull String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Builder setModificationDate(Date modificationDate) {
            this.modificationDate = modificationDate;
            return this;
        }

        public Builder setFileEncoding(StreamEncoding encoding) {
            this.fileEncoding = encoding;
            return this;
        }

        public void addDetachedSignature(DetachedSignature signature) {
            this.detachedSignatures.add(signature);
        }

        public void addOnePassSignature(OnePassSignature onePassSignature) {
            this.onePassSignatures.add(onePassSignature);
        }

        public OpenPgpMetadata build() {
            return new OpenPgpMetadata(recipientFingerprints, decryptionKey,
                    symmetricKeyAlgorithm, compressionAlgorithm,
                    onePassSignatures, detachedSignatures, fileName, modificationDate, fileEncoding);
        }
    }
}
