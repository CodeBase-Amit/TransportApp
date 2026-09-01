package com.example.transportapp.core.database.seed

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert

/**
 * Gates the demo seeder (Phase2.md §3.5 / D8): a row id=0 carries the version of the dataset
 * currently in the database. Bumping [SeedVersion.CURRENT] re-seeds cleanly on next launch.
 */
@Entity(tableName = "seed_version")
data class SeedVersionEntity(
    @PrimaryKey val id: Int = 0,
    val version: Int,
    val seeded_at: Long,
) {
    object SeedVersion {
        /** v1 = org (S2); v2 = org + masters (S3); v3 = machine-readable charge heads, real
         *  station states and the company-default rate row (S4); v4 = numbering series +
         *  initial bilty lease + the register-fixture consignments 04183–04188 (S5); v5 =
         *  the Held/Returned event types corrected (S6); v6 = money fixtures — the unbilled
         *  TBB pool, issued bills FB/IND/2627/00298+00311 and receipts 00126–00128 (S9);
         *  v7 = the default BILTY template, engine-parsable and keyed to the DOC_SNAPSHOT
         *  payload keys (S11); v8 = the dated company calculation setting — 5% GST, the
         *  volumetric divisor live at 6000 (S14). Bump to re-seed on launch. */
        const val CURRENT = 8
    }
}

@Dao
interface SeedVersionDao {

    @Query("SELECT * FROM seed_version WHERE id = 0")
    suspend fun get(): SeedVersionEntity?

    @Upsert
    suspend fun upsert(entity: SeedVersionEntity)
}
