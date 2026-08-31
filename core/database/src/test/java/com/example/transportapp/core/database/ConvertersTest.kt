package com.example.transportapp.core.database

import com.example.transportapp.core.database.envelope.SyncState
import com.example.transportapp.core.database.outbox.OutboxEntityType
import com.example.transportapp.core.database.outbox.OutboxOp
import com.example.transportapp.core.database.outbox.OutboxState
import org.junit.Assert.assertEquals
import org.junit.Test

/** Converter round-trips (Phase2.md S1 tests). Column values are stable enum names. */
class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `sync state round-trips`() {
        SyncState.entries.forEach { state ->
            assertEquals(state, converters.syncStateFromName(converters.syncStateToName(state)))
        }
    }

    @Test
    fun `outbox op round-trips`() {
        OutboxOp.entries.forEach { op ->
            assertEquals(op, converters.outboxOpFromName(converters.outboxOpToName(op)))
        }
    }

    @Test
    fun `outbox entity type round-trips`() {
        OutboxEntityType.entries.forEach { type ->
            assertEquals(type, converters.outboxEntityTypeFromName(converters.outboxEntityTypeToName(type)))
        }
    }

    @Test
    fun `unknown entity type degrades to a safe default, never throws`() {
        // "Every inbound boundary is untrusted" — a newer server must not crash an older app.
        assertEquals(OutboxEntityType.CONSIGNMENT, converters.outboxEntityTypeFromName("SOMETHING_NEW"))
    }

    @Test
    fun `outbox state round-trips`() {
        OutboxState.entries.forEach { state ->
            assertEquals(state, converters.outboxStateFromName(converters.outboxStateToName(state)))
        }
    }
}
