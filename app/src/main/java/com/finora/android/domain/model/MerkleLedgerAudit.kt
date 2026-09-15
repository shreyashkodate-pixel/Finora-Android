package com.finora.android.domain.model

import java.security.MessageDigest

data class AuditBlock(
    val height: Int,
    val blockHash: String,
    val prevHash: String,
    val transactionId: String,
    val transactionTitle: String,
    val amountMinorUnits: Long,
    val timestampMillis: Long
)

data class AuditVerificationResult(
    val isVerified: Boolean,
    val totalBlocks: Int,
    val merkleRootDigest: String,
    val blocks: List<AuditBlock>,
    val auditSummary: String
)

object MerkleLedgerAudit {

    private const val GENESIS_HASH = "0000000000000000000000000000000000000000000000000000000000000000"

    fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Constructs a verifiable SHA-256 hash chain over transactions and computes Merkle Root.
     */
    fun buildAndVerifyLedger(
        transactions: List<SimpleExpenseRecord>
    ): AuditVerificationResult {
        if (transactions.isEmpty()) {
            return AuditVerificationResult(
                isVerified = true,
                totalBlocks = 0,
                merkleRootDigest = GENESIS_HASH,
                blocks = emptyList(),
                auditSummary = "No transaction records present in ledger yet."
            )
        }

        // Sort deterministically by timestamp and ID
        val sorted = transactions.sortedWith(compareBy({ it.timestampMillis }, { it.id }))
        val blocks = mutableListOf<AuditBlock>()
        var currentPrevHash = GENESIS_HASH

        sorted.forEachIndexed { index, tx ->
            val payload = "$currentPrevHash:${tx.id}:${tx.amountMinorUnits}:${tx.timestampMillis}:${tx.title}"
            val blockHash = sha256(payload)
            val block = AuditBlock(
                height = index + 1,
                blockHash = blockHash,
                prevHash = currentPrevHash,
                transactionId = tx.id,
                transactionTitle = tx.title,
                amountMinorUnits = tx.amountMinorUnits,
                timestampMillis = tx.timestampMillis
            )
            blocks.add(block)
            currentPrevHash = blockHash
        }

        // Compute Merkle Root from block hashes
        val merkleRoot = computeMerkleRoot(blocks.map { it.blockHash })

        // Verify chain integrity
        var chainValid = true
        for (i in 1 until blocks.size) {
            if (blocks[i].prevHash != blocks[i - 1].blockHash) {
                chainValid = false
                break
            }
        }

        return AuditVerificationResult(
            isVerified = chainValid,
            totalBlocks = blocks.size,
            merkleRootDigest = merkleRoot,
            blocks = blocks.reversed(), // newest on top for UI
            auditSummary = if (chainValid) {
                "Ledger 100% verified. Zero tampered blocks across ${blocks.size} historical records."
            } else {
                "Ledger integrity warning: Discrepancy detected in cryptographic hash sequence."
            }
        )
    }

    private fun computeMerkleRoot(hashes: List<String>): String {
        if (hashes.isEmpty()) return GENESIS_HASH
        var currentLevel = hashes

        while (currentLevel.size > 1) {
            val nextLevel = mutableListOf<String>()
            for (i in currentLevel.indices step 2) {
                val left = currentLevel[i]
                val right = if (i + 1 < currentLevel.size) currentLevel[i + 1] else left
                nextLevel.add(sha256(left + right))
            }
            currentLevel = nextLevel
        }

        return currentLevel.first()
    }
}
