package de.vanitasvitae.crypto.pgpainless;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import de.vanitasvitae.crypto.pgpainless.algorithm.CompressionAlgorithm;
import de.vanitasvitae.crypto.pgpainless.algorithm.SymmetricKeyAlgorithm;

public class PainlessResult {

    private final Set<Long> recipientKeyIds;
    private final Long decryptionKeyId;
    private final SymmetricKeyAlgorithm symmetricKeyAlgorithm;
    private final CompressionAlgorithm compressionAlgorithm;
    private final boolean integrityProtected;
    private final Set<Long> signatureKeyIds;
    private final Set<Long> verifiedSignatureKeyIds;

    public PainlessResult(Set<Long> recipientKeyIds,
                          Long decryptionKeyId,
                          SymmetricKeyAlgorithm symmetricKeyAlgorithm,
                          CompressionAlgorithm algorithm,
                          boolean integrityProtected,
                          Set<Long> signatureKeyIds,
                          Set<Long> verifiedSignatureKeyIds) {

        this.recipientKeyIds = Collections.unmodifiableSet(recipientKeyIds);
        this.decryptionKeyId = decryptionKeyId;
        this.symmetricKeyAlgorithm = symmetricKeyAlgorithm;
        this.compressionAlgorithm = algorithm;
        this.integrityProtected = integrityProtected;
        this.signatureKeyIds = Collections.unmodifiableSet(signatureKeyIds);
        this.verifiedSignatureKeyIds = Collections.unmodifiableSet(verifiedSignatureKeyIds);
    }

    public Set<Long> getRecipientKeyIds() {
        return recipientKeyIds;
    }

    public Long getDecryptionKeyId() {
        return decryptionKeyId;
    }

    public SymmetricKeyAlgorithm getSymmetricKeyAlgorithm() {
        return symmetricKeyAlgorithm;
    }

    public CompressionAlgorithm getCompressionAlgorithm() {
        return compressionAlgorithm;
    }

    public boolean isIntegrityProtected() {
        return integrityProtected;
    }

    public Set<Long> getSignatureKeyIds() {
        return signatureKeyIds;
    }

    public Set<Long> getVerifiedSignatureKeyIds() {
        return verifiedSignatureKeyIds;
    }

    public static Builder getBuilder() {
        return new Builder();
    }

    public static class Builder {

        private final Set<Long> recipientKeyIds = new HashSet<>();
        private Long decryptionKeyId;
        private SymmetricKeyAlgorithm symmetricKeyAlgorithm = SymmetricKeyAlgorithm.NULL;
        private CompressionAlgorithm compressionAlgorithm = CompressionAlgorithm.UNCOMPRESSED;
        private boolean integrityProtected = false;
        private final Set<Long> signatureKeyIds = new HashSet<>();
        private final Set<Long> verifiedSignatureKeyIds = new HashSet<>();

        public Builder addRecipientKeyId(long id) {
            this.recipientKeyIds.add(id);
            return this;
        }

        public Builder setDecryptionKeyId(long id) {
            this.decryptionKeyId = id;
            return this;
        }

        public Builder setCompressionAlgorithm(CompressionAlgorithm algorithm) {
            this.compressionAlgorithm = algorithm;
            return this;
        }

        public Builder addSignatureKeyId(long id) {
            this.signatureKeyIds.add(id);
            return this;
        }

        public Builder addVerifiedSignatureKeyId(long id) {
            this.verifiedSignatureKeyIds.add(id);
            return this;
        }

        public Builder setSymmetricKeyAlgorithm(SymmetricKeyAlgorithm symmetricKeyAlgorithm) {
            this.symmetricKeyAlgorithm = symmetricKeyAlgorithm;
            return this;
        }

        public Builder setIntegrityProtected(boolean integrityProtected) {
            this.integrityProtected = integrityProtected;
            return this;
        }

        public PainlessResult build() {
            return new PainlessResult(recipientKeyIds, decryptionKeyId, symmetricKeyAlgorithm, compressionAlgorithm, integrityProtected, signatureKeyIds, verifiedSignatureKeyIds);
        }
    }

    public static class ResultAndInputStream {
        private final PainlessResult.Builder resultBuilder;
        private final PainlessStream.In inputStream;

        public ResultAndInputStream(PainlessResult.Builder resultBuilder, PainlessStream.In inputStream) {
            this.resultBuilder = resultBuilder;
            this.inputStream = inputStream;
        }

        public PainlessResult getResult() {
            if (!inputStream.isClosed()) {
                throw new IllegalStateException("InputStream must be closed before the PainlessResult can be accessed.");
            }
            return resultBuilder.build();
        }

        public PainlessStream.In getInputStream() {
            return inputStream;
        }
    }
}
