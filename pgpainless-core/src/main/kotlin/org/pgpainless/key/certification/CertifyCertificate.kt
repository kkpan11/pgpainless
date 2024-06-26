// SPDX-FileCopyrightText: 2023 Paul Schaub <vanitasvitae@fsfe.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.pgpainless.key.certification

import java.util.*
import org.bouncycastle.openpgp.PGPException
import org.bouncycastle.openpgp.PGPPublicKeyRing
import org.bouncycastle.openpgp.PGPSecretKey
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.bouncycastle.openpgp.PGPSignature
import org.pgpainless.PGPainless
import org.pgpainless.algorithm.CertificationType
import org.pgpainless.algorithm.KeyFlag
import org.pgpainless.algorithm.Trustworthiness
import org.pgpainless.exception.KeyException
import org.pgpainless.exception.KeyException.ExpiredKeyException
import org.pgpainless.exception.KeyException.MissingSecretKeyException
import org.pgpainless.exception.KeyException.RevokedKeyException
import org.pgpainless.key.protection.SecretKeyRingProtector
import org.pgpainless.key.util.KeyRingUtils
import org.pgpainless.signature.builder.ThirdPartyCertificationSignatureBuilder
import org.pgpainless.signature.builder.ThirdPartyDirectKeySignatureBuilder
import org.pgpainless.signature.subpackets.CertificationSubpackets

/**
 * API for creating certifications and delegations (Signatures) on keys. This API can be used to
 * sign another persons OpenPGP key.
 *
 * A certification over a user-id is thereby used to attest, that the user believes that the user-id
 * really belongs to the owner of the certificate. A delegation over a key can be used to delegate
 * trust by marking the certificate as a trusted introducer.
 */
class CertifyCertificate {

    /**
     * Create a certification over a User-Id. By default, this method will use
     * [CertificationType.GENERIC] to create the signature.
     *
     * @param userId user-id to certify
     * @param certificate certificate
     * @return API
     */
    fun userIdOnCertificate(userId: String, certificate: PGPPublicKeyRing): CertificationOnUserId =
        userIdOnCertificate(userId, certificate, CertificationType.GENERIC)

    /**
     * Create a certification of the given [CertificationType] over a User-Id.
     *
     * @param userid user-id to certify
     * @param certificate certificate
     * @param certificationType type of signature
     * @return API
     */
    fun userIdOnCertificate(
        userId: String,
        certificate: PGPPublicKeyRing,
        certificationType: CertificationType
    ) = CertificationOnUserId(userId, certificate, certificationType)

    /**
     * Create a delegation (direct key signature) over a certificate. This can be used to mark a
     * certificate as a trusted introducer (see [certificate] method with [Trustworthiness]
     * argument).
     *
     * @param certificate certificate
     * @return API
     */
    fun certificate(certificate: PGPPublicKeyRing): DelegationOnCertificate =
        certificate(certificate, null)

    /**
     * Create a delegation (direct key signature) containing a
     * [org.bouncycastle.bcpg.sig.TrustSignature] packet over a certificate. This can be used to
     * mark a certificate as a trusted introducer.
     *
     * @param certificate certificate
     * @param trustworthiness trustworthiness of the certificate
     * @return API
     */
    fun certificate(certificate: PGPPublicKeyRing, trustworthiness: Trustworthiness?) =
        DelegationOnCertificate(certificate, trustworthiness)

    class CertificationOnUserId(
        val userId: String,
        val certificate: PGPPublicKeyRing,
        val certificationType: CertificationType
    ) {

        /**
         * Create the certification using the given key.
         *
         * @param certificationKey key used to create the certification
         * @param protector protector to unlock the certification key
         * @return API
         * @throws PGPException in case of an OpenPGP related error
         */
        fun withKey(
            certificationKey: PGPSecretKeyRing,
            protector: SecretKeyRingProtector
        ): CertificationOnUserIdWithSubpackets {

            val secretKey = getCertifyingSecretKey(certificationKey)
            val sigBuilder =
                ThirdPartyCertificationSignatureBuilder(
                    certificationType.asSignatureType(), secretKey, protector)

            return CertificationOnUserIdWithSubpackets(certificate, userId, sigBuilder)
        }
    }

    class CertificationOnUserIdWithSubpackets(
        val certificate: PGPPublicKeyRing,
        val userId: String,
        val sigBuilder: ThirdPartyCertificationSignatureBuilder
    ) {

        /**
         * Apply the given signature subpackets and build the certification.
         *
         * @param subpacketCallback callback to modify the signatures subpackets
         * @return result
         * @throws PGPException in case of an OpenPGP related error
         */
        fun buildWithSubpackets(
            subpacketCallback: CertificationSubpackets.Callback
        ): CertificationResult {
            sigBuilder.applyCallback(subpacketCallback)
            return build()
        }

        /**
         * Build the certification signature.
         *
         * @return result
         * @throws PGPException in case of an OpenPGP related error
         */
        fun build(): CertificationResult {
            val signature = sigBuilder.build(certificate, userId)
            val certifiedCertificate =
                KeyRingUtils.injectCertification(certificate, userId, signature)
            return CertificationResult(certifiedCertificate, signature)
        }
    }

    class DelegationOnCertificate(
        val certificate: PGPPublicKeyRing,
        val trustworthiness: Trustworthiness?
    ) {

        /**
         * Build the delegation using the given certification key.
         *
         * @param certificationKey key to create the certification with
         * @param protector protector to unlock the certification key
         * @return API
         * @throws PGPException in case of an OpenPGP related error
         */
        fun withKey(
            certificationKey: PGPSecretKeyRing,
            protector: SecretKeyRingProtector
        ): DelegationOnCertificateWithSubpackets {
            val secretKey = getCertifyingSecretKey(certificationKey)
            val sigBuilder = ThirdPartyDirectKeySignatureBuilder(secretKey, protector)
            if (trustworthiness != null) {
                sigBuilder.hashedSubpackets.setTrust(
                    true, trustworthiness.depth, trustworthiness.amount)
            }
            return DelegationOnCertificateWithSubpackets(certificate, sigBuilder)
        }
    }

    class DelegationOnCertificateWithSubpackets(
        val certificate: PGPPublicKeyRing,
        val sigBuilder: ThirdPartyDirectKeySignatureBuilder
    ) {

        /**
         * Apply the given signature subpackets and build the delegation signature.
         *
         * @param subpacketsCallback callback to modify the signatures subpackets
         * @return result
         * @throws PGPException in case of an OpenPGP related error
         */
        fun buildWithSubpackets(
            subpacketsCallback: CertificationSubpackets.Callback
        ): CertificationResult {
            sigBuilder.applyCallback(subpacketsCallback)
            return build()
        }

        /**
         * Build the delegation signature.
         *
         * @return result
         * @throws PGPException in case of an OpenPGP related error
         */
        fun build(): CertificationResult {
            val delegatedKey = certificate.publicKey
            val delegation = sigBuilder.build(delegatedKey)
            val delegatedCertificate =
                KeyRingUtils.injectCertification(certificate, delegatedKey, delegation)
            return CertificationResult(delegatedCertificate, delegation)
        }
    }

    /**
     * Result of a certification operation.
     *
     * @param certifiedCertificate certificate which now contains the newly created signature
     * @param certification the newly created signature
     */
    data class CertificationResult(
        val certifiedCertificate: PGPPublicKeyRing,
        val certification: PGPSignature
    )

    companion object {
        @JvmStatic
        private fun getCertifyingSecretKey(certificationKey: PGPSecretKeyRing): PGPSecretKey {
            val now = Date()
            val info = PGPainless.inspectKeyRing(certificationKey, now)

            val fingerprint = info.fingerprint
            val certificationPubKey = info.getPublicKey(fingerprint)
            requireNotNull(certificationPubKey) { "Primary key cannot be null." }
            if (!info.isKeyValidlyBound(certificationPubKey.keyID)) {
                throw RevokedKeyException(fingerprint)
            }

            if (!info.isUsableForThirdPartyCertification) {
                throw KeyException.UnacceptableThirdPartyCertificationKeyException(fingerprint)
            }

            val expirationDate = info.getExpirationDateForUse(KeyFlag.CERTIFY_OTHER)
            if (expirationDate != null && expirationDate < now) {
                throw ExpiredKeyException(fingerprint, expirationDate)
            }

            return certificationKey.getSecretKey(certificationPubKey.keyID)
                ?: throw MissingSecretKeyException(fingerprint, certificationPubKey.keyID)
        }
    }
}
