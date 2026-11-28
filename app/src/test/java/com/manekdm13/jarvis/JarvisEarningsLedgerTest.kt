package com.manekdm13.jarvis

import org.junit.Assert.*
import org.junit.Test

class JarvisEarningsLedgerTest {
    private fun entry(
        id: String = "task-1",
        status: JarvisEarningStatus = JarvisEarningStatus.PENDING,
        net: Double = 4.5
    ) = JarvisEarningEntry(
        id = id,
        platform = "Example",
        taskTitle = "Data review",
        occurredAtEpochMillis = 1_800_000_000_000,
        minutesSpent = 30,
        grossUsd = 5.0,
        feesUsd = 0.5,
        netUsd = net,
        status = status,
        payoutRoute = JarvisPayoutRoute.PAYONEER
    )

    @Test fun upsertPreventsDuplicateTaskRows() {
        val first = entry()
        val updated = entry(status = JarvisEarningStatus.PAID)
        val ledger = JarvisEarningsLedger.upsert(listOf(first), updated)
        assertEquals(1, ledger.size)
        assertEquals(JarvisEarningStatus.PAID, ledger.single().status)
    }

    @Test fun totalsSeparatePaidFromOutstanding() {
        val entries = listOf(
            entry(id = "paid", status = JarvisEarningStatus.PAID),
            entry(id = "pending", status = JarvisEarningStatus.PENDING),
            entry(id = "rejected", status = JarvisEarningStatus.REJECTED)
        )
        assertEquals(4.5, JarvisEarningsLedger.paidUsd(entries), 0.001)
        assertEquals(4.5, JarvisEarningsLedger.outstandingUsd(entries), 0.001)
    }

    @Test fun netMustReconcileToGrossLessFees() {
        assertNotNull(entry(net = 9.0).validate())
    }

    @Test fun rejectsNonFiniteMoney() {
        assertNotNull(entry(net = Double.NaN).validate())
    }

    @Test fun negativeTimeIsInvalid() {
        assertNotNull(entry().copy(minutesSpent = -1).validate())
    }
}
