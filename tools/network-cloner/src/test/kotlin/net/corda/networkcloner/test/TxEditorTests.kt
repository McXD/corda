package net.corda.networkcloner.test

import net.corda.networkcloner.util.IdentityFactory
import net.corda.networkcloner.util.toTransactionComponents
import org.junit.Ignore
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TxEditorTests : TestSupport() {

    @Test
    fun `Test app TxEditor can be loaded and applied`() {
        val nodeDatabase = getNodeDatabase("s2","source","client")
        val sourceTxByteArray = nodeDatabase.readMigrationData().transactions.first().transaction

        val serializer = getSerializer("s2")
        val sourceSignedTransaction = serializer.deserializeDbBlobIntoTransaction(sourceTxByteArray)

        val sourcePartyRepository = getPartyRepository("s2","source")
        val destPartyRepository = getPartyRepository("s2", "destination")
        val identities = IdentityFactory.getIdentities(sourcePartyRepository, destPartyRepository)

        val cordappRepository = getCordappsRepository("s2")
        val txEditors = cordappRepository.getTxEditors()
        assertEquals(1, txEditors.size)
        val txEditor = txEditors.single()
        val transactionComponents = sourceSignedTransaction.toTransactionComponents()

        val editedTransactionComponents = txEditor.edit(transactionComponents, identities)

        assertTrue(editedTransactionComponents.outputs.all {
            it.data.participants.intersect(identities.map { it.sourceParty }).isEmpty()
        }, "All outputs should be clear of any original (source) party in their participants list")

        assertTrue(editedTransactionComponents.outputs.all {
            it.data.participants.intersect(identities.map { it.destinationPartyAndPrivateKey.party }).isNotEmpty()
        }, "All outputs should have at least one participant from the destination list of parties")


    }

}