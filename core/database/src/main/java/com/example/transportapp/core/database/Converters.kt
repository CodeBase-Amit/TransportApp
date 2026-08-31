package com.example.transportapp.core.database

import androidx.room.TypeConverter
import com.example.transportapp.core.database.envelope.SyncState
import com.example.transportapp.core.database.outbox.OutboxEntityType
import com.example.transportapp.core.database.outbox.OutboxOp
import com.example.transportapp.core.database.outbox.OutboxState

/**
 * Enum columns are stored as their stable name string — the same values the sync phase
 * sends over the wire, so a converter rename can never desynchronise client and server.
 */
class Converters {

    @TypeConverter fun syncStateToName(value: SyncState): String = value.name
    @TypeConverter fun syncStateFromName(value: String): SyncState = SyncState.valueOf(value)

    @TypeConverter fun outboxOpToName(value: OutboxOp): String = value.name
    @TypeConverter fun outboxOpFromName(value: String): OutboxOp = OutboxOp.valueOf(value)

    @TypeConverter fun outboxEntityTypeToName(value: OutboxEntityType): String = value.name
    @TypeConverter fun outboxEntityTypeFromName(value: String): OutboxEntityType =
        runCatching { OutboxEntityType.valueOf(value) }.getOrDefault(OutboxEntityType.CONSIGNMENT)

    @TypeConverter fun outboxStateToName(value: OutboxState): String = value.name
    @TypeConverter fun outboxStateFromName(value: String): OutboxState = OutboxState.valueOf(value)
}
