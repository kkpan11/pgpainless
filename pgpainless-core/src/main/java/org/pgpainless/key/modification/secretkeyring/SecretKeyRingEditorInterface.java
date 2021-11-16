// SPDX-FileCopyrightText: 2020 Paul Schaub <vanitasvitae@fsfe.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.pgpainless.key.modification.secretkeyring;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureSubpacketVector;
import org.pgpainless.algorithm.KeyFlag;
import org.pgpainless.key.OpenPgpFingerprint;
import org.pgpainless.key.generation.KeySpec;
import org.pgpainless.key.protection.KeyRingProtectionSettings;
import org.pgpainless.key.protection.SecretKeyRingProtector;
import org.pgpainless.key.util.RevocationAttributes;
import org.pgpainless.key.util.UserId;
import org.pgpainless.signature.subpackets.RevocationSignatureSubpackets;
import org.pgpainless.signature.subpackets.SelfSignatureSubpackets;
import org.pgpainless.util.Passphrase;

public interface SecretKeyRingEditorInterface {

    /**
     * Add a user-id to the key ring.
     *
     * @param userId user-id
     * @param secretKeyRingProtector protector to unlock the secret key
     * @return the builder
     */
    default SecretKeyRingEditorInterface addUserId(UserId userId, SecretKeyRingProtector secretKeyRingProtector) throws PGPException {
        return addUserId(userId.toString(), secretKeyRingProtector);
    }

    /**
     * Add a user-id to the key ring.
     *
     * @param userId user-id
     * @param secretKeyRingProtector protector to unlock the secret key
     * @return the builder
     */
    SecretKeyRingEditorInterface addUserId(String userId, SecretKeyRingProtector secretKeyRingProtector) throws PGPException;

    SecretKeyRingEditorInterface addUserId(
            String userId,
            @Nullable SelfSignatureSubpackets.Callback signatureSubpacketCallback,
            SecretKeyRingProtector protector) throws PGPException;

    /**
     * Add a subkey to the key ring.
     * The subkey will be generated from the provided {@link KeySpec}.
     *
     * @param keySpec key specification
     * @param subKeyPassphrase passphrase to encrypt the sub key
     * @param secretKeyRingProtector protector to unlock the secret key of the key ring
     * @return the builder
     */
    SecretKeyRingEditorInterface addSubKey(@Nonnull KeySpec keySpec,
                                           @Nullable Passphrase subKeyPassphrase,
                                           SecretKeyRingProtector secretKeyRingProtector)
            throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, PGPException;

    @Deprecated
    SecretKeyRingEditorInterface addSubKey(PGPSecretKey subKey,
                                           PGPSignatureSubpacketVector hashedSubpackets,
                                           PGPSignatureSubpacketVector unhashedSubpackets,
                                           SecretKeyRingProtector subKeyProtector, SecretKeyRingProtector keyRingProtector)
            throws PGPException;

    SecretKeyRingEditorInterface addSubKey(PGPSecretKey subkey,
                                           @Nullable SelfSignatureSubpackets.Callback bindingSignatureCallback,
                                           @Nullable SelfSignatureSubpackets.Callback backSignatureCallback,
                                           SecretKeyRingProtector subkeyProtector,
                                           SecretKeyRingProtector primaryKeyProtector,
                                           KeyFlag keyFlag,
                                           KeyFlag... additionalKeyFlags) throws PGPException, IOException;

    /**
     * Revoke the key ring.
     * The revocation will be a hard revocation, rendering the whole key invalid for any past or future signatures.
     *
     * @param secretKeyRingProtector protector of the primary key
     * @return the builder
     */
    default SecretKeyRingEditorInterface revoke(SecretKeyRingProtector secretKeyRingProtector)
            throws PGPException {
        return revoke(secretKeyRingProtector, (RevocationAttributes) null);
    }

    /**
     * Revoke the key ring using the provided revocation attributes.
     * The attributes define, whether or not the revocation was a hard revocation or not.
     *
     * @param secretKeyRingProtector protector of the primary key
     * @param revocationAttributes reason for the revocation
     * @return the builder
     */
    SecretKeyRingEditorInterface revoke(SecretKeyRingProtector secretKeyRingProtector,
                                        @Nullable RevocationAttributes revocationAttributes)
            throws PGPException;

    SecretKeyRingEditorInterface revoke(SecretKeyRingProtector secretKeyRingProtector,
                                        @Nullable RevocationSignatureSubpackets.Callback subpacketsCallback) throws PGPException;

    /**
     * Revoke the subkey binding signature of a subkey.
     * The subkey with the provided fingerprint will be revoked.
     * If no suitable subkey is found, a {@link java.util.NoSuchElementException} will be thrown.
     *
     * @param fingerprint fingerprint of the subkey to be revoked
     * @param secretKeyRingProtector protector to unlock the secret key ring
     * @return the builder
     */
    default SecretKeyRingEditorInterface revokeSubKey(OpenPgpFingerprint fingerprint,
                                                      SecretKeyRingProtector secretKeyRingProtector)
            throws PGPException {
        return revokeSubKey(fingerprint, secretKeyRingProtector, null);
    }

    /**
     * Revoke the subkey binding signature of a subkey.
     * The subkey with the provided fingerprint will be revoked.
     * If no suitable subkey is found, a {@link java.util.NoSuchElementException} will be thrown.
     *
     * @param fingerprint fingerprint of the subkey to be revoked
     * @param secretKeyRingProtector protector to unlock the primary key
     * @param revocationAttributes reason for the revocation
     * @return the builder
     */
    default SecretKeyRingEditorInterface revokeSubKey(OpenPgpFingerprint fingerprint,
                                              SecretKeyRingProtector secretKeyRingProtector,
                                              RevocationAttributes revocationAttributes)
            throws PGPException {
        return revokeSubKey(fingerprint.getKeyId(),
                secretKeyRingProtector,
                revocationAttributes);
    }

    /**
     * Revoke the subkey binding signature of a subkey.
     * The subkey with the provided key-id will be revoked.
     * If no suitable subkey is found, q {@link java.util.NoSuchElementException} will be thrown.
     *
     * @param subKeyId id of the subkey
     * @param secretKeyRingProtector protector to unlock the primary key
     * @param revocationAttributes reason for the revocation
     * @return the builder
     */
    SecretKeyRingEditorInterface revokeSubKey(long subKeyId,
                                              SecretKeyRingProtector secretKeyRingProtector,
                                              RevocationAttributes revocationAttributes)
            throws PGPException;

    /**
     * Revoke the subkey binding signature of a subkey.
     * The subkey with the provided key-id will be revoked.
     * If no suitable subkey is found, q {@link java.util.NoSuchElementException} will be thrown.
     *
     * @param subKeyId id of the subkey
     * @param secretKeyRingProtector protector to unlock the secret key ring
     * @return the builder
     */
    default SecretKeyRingEditorInterface revokeSubKey(long subKeyId,
                                                      SecretKeyRingProtector secretKeyRingProtector)
            throws PGPException {
        return revokeSubKey(subKeyId, secretKeyRingProtector, (RevocationSignatureSubpackets.Callback) null);
    }

    SecretKeyRingEditorInterface revokeSubKey(long keyID,
                                              SecretKeyRingProtector secretKeyRingProtector,
                                              @Nullable RevocationSignatureSubpackets.Callback subpacketsCallback)
            throws PGPException;

    /**
     * Revoke the given userID.
     * The revocation will be a hard revocation, rendering the user-id invalid for any past or future signatures.
     *
     * @param userId userId to revoke
     * @param secretKeyRingProtector protector to unlock the primary key
     * @return the builder
     */
    default SecretKeyRingEditorInterface revokeUserId(String userId,
                                                      SecretKeyRingProtector secretKeyRingProtector)
            throws PGPException {
        return revokeUserId(userId, secretKeyRingProtector, (RevocationAttributes) null);
    }

    /**
     * Revoke the given userID using the provided revocation attributes.
     *
     * @param userId userId to revoke
     * @param secretKeyRingProtector protector to unlock the primary key
     * @param revocationAttributes reason for the revocation
     * @return the builder
     */
    SecretKeyRingEditorInterface revokeUserId(String userId,
                                              SecretKeyRingProtector secretKeyRingProtector,
                                              @Nullable RevocationAttributes revocationAttributes)
            throws PGPException;

    SecretKeyRingEditorInterface revokeUserId(String userId,
                                              SecretKeyRingProtector secretKeyRingProtector,
                                              @Nullable RevocationSignatureSubpackets.Callback subpacketCallback)
        throws PGPException;

    /**
     * Set the expiration date for the primary key of the key ring.
     * If the key is supposed to never expire, then an expiration date of null is expected.
     *
     * @param expiration new expiration date or null
     * @param secretKeyRingProtector to unlock the secret key
     * @return the builder
     */
    SecretKeyRingEditorInterface setExpirationDate(Date expiration,
                                                   SecretKeyRingProtector secretKeyRingProtector)
            throws PGPException;

    /**
     * Set key expiration time.
     *
     * @param fingerprint key that will have its expiration date adjusted
     * @param expiration target expiration time or @{code null} for no expiration
     * @param secretKeyRingProtector protector to unlock the priary key
     * @return the builder
     */
    SecretKeyRingEditorInterface setExpirationDate(OpenPgpFingerprint fingerprint,
                                                   Date expiration,
                                                   SecretKeyRingProtector secretKeyRingProtector)
            throws PGPException;

    /**
     * Create a detached revocation certificate, which can be used to revoke the specified key.
     *
     * @param secretKeyRingProtector protector to unlock the primary key.
     * @param revocationAttributes reason for the revocation
     * @return revocation certificate
     */
    PGPSignature createRevocationCertificate(SecretKeyRingProtector secretKeyRingProtector,
                                             RevocationAttributes revocationAttributes)
            throws PGPException;

    PGPSignature createRevocationCertificate(long subkeyId,
                                             SecretKeyRingProtector secretKeyRingProtector,
                                             RevocationAttributes revocationAttributes)
        throws PGPException;

    default PGPSignature createRevocationCertificate(OpenPgpFingerprint subkeyFingerprint,
                                             SecretKeyRingProtector secretKeyRingProtector,
                                             RevocationAttributes revocationAttributes)
        throws PGPException {
        return createRevocationCertificate(subkeyFingerprint.getKeyId(), secretKeyRingProtector, revocationAttributes);
    }

    /**
     * Change the passphrase of the whole key ring.
     *
     * @param oldPassphrase old passphrase or null, if the key was unprotected
     * @return next builder step
     */
    default WithKeyRingEncryptionSettings changePassphraseFromOldPassphrase(@Nullable Passphrase oldPassphrase) {
        return changePassphraseFromOldPassphrase(oldPassphrase, KeyRingProtectionSettings.secureDefaultSettings());
    }

    /**
     * Change the passphrase of the whole key ring.
     *
     * @param oldPassphrase old passphrase or null, if the key was unprotected
     * @param oldProtectionSettings custom settings for the old passphrase
     * @return next builder step
     */
    WithKeyRingEncryptionSettings changePassphraseFromOldPassphrase(@Nullable Passphrase oldPassphrase,
                                                                    @Nonnull KeyRingProtectionSettings oldProtectionSettings);

    /**
     * Change the passphrase of a single subkey in the key ring.
     *
     * Note: While it is a valid use-case to have different passphrases per subKey,
     *  this is one of the reasons why OpenPGP sucks in practice.
     *
     * @param keyId id of the subkey
     * @param oldPassphrase old passphrase
     * @return next builder step
     */
    default WithKeyRingEncryptionSettings changeSubKeyPassphraseFromOldPassphrase(@Nonnull Long keyId,
                                                                                  @Nullable Passphrase oldPassphrase) {
        return changeSubKeyPassphraseFromOldPassphrase(keyId, oldPassphrase, KeyRingProtectionSettings.secureDefaultSettings());
    }

    WithKeyRingEncryptionSettings changeSubKeyPassphraseFromOldPassphrase(@Nonnull Long keyId,
                                                                          @Nullable Passphrase oldPassphrase,
                                                                          @Nonnull KeyRingProtectionSettings oldProtectionSettings);

    interface WithKeyRingEncryptionSettings {

        /**
         * Set secure default settings for the symmetric passphrase encryption.
         * Note that this obviously has no effect if you decide to set {@link WithPassphrase#toNoPassphrase()}.
         *
         * @return next builder step
         */
        WithPassphrase withSecureDefaultSettings();

        /**
         * Set custom settings for the symmetric passphrase encryption.
         *
         * @param settings custom settings
         * @return next builder step
         */
        WithPassphrase withCustomSettings(KeyRingProtectionSettings settings);

    }

    interface WithPassphrase {

        /**
         * Set the passphrase.
         *
         * @param passphrase passphrase
         * @return editor builder
         */
        SecretKeyRingEditorInterface toNewPassphrase(Passphrase passphrase) throws PGPException;

        /**
         * Leave the key unprotected.
         *
         * @return editor builder
         */
        SecretKeyRingEditorInterface toNoPassphrase() throws PGPException;
    }

    /**
     * Return the {@link PGPSecretKeyRing}.
     * @return the key
     */
    PGPSecretKeyRing done();

}
