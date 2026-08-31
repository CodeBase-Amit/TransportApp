package com.example.transportapp.core.database.envelope

/**
 * The sync envelope state carried on every synced row (TransportApp.md §16.2):
 * `sync_state` of one of synced / pending / conflicted.
 * SYNCED — server has acknowledged this row.
 * PENDING — this device changed the row and the change is queued in the outbox.
 * CONFLICTED — the sync phase set this; a resolvable item, never a silent overwrite (§17.3).
 */
enum class SyncState { SYNCED, PENDING, CONFLICTED }
