package com.example.transportapp.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.transportapp.core.database.cursor.SyncCursorDao
import com.example.transportapp.core.database.cursor.SyncCursorEntity
import com.example.transportapp.core.database.dao.BillingDao
import com.example.transportapp.core.database.dao.MastersDao
import com.example.transportapp.core.database.dao.ConsignmentDao
import com.example.transportapp.core.database.dao.NumberingDao
import com.example.transportapp.core.database.dao.OrgDao
import com.example.transportapp.core.database.dao.TripDao
import com.example.transportapp.core.database.entity.BrokerEntity
import com.example.transportapp.core.database.entity.BranchEntity
import com.example.transportapp.core.database.entity.ChargeHeadEntity
import com.example.transportapp.core.database.entity.ChargeLineEntity
import com.example.transportapp.core.database.entity.AttachmentEntity
import com.example.transportapp.core.database.entity.PodEntity
import com.example.transportapp.core.database.entity.CompanyEntity
import com.example.transportapp.core.database.entity.ConsignmentEntity
import com.example.transportapp.core.database.entity.ConsignmentFtsEntity
import com.example.transportapp.core.database.entity.ConsignmentItemEntity
import com.example.transportapp.core.database.entity.CreditNoteEntity
import com.example.transportapp.core.database.entity.DocSnapshotEntity
import com.example.transportapp.core.database.entity.DriverEntity
import com.example.transportapp.core.database.entity.FreightBillEntity
import com.example.transportapp.core.database.entity.GoodsEntity
import com.example.transportapp.core.database.entity.LorryHireEntity
import com.example.transportapp.core.database.entity.MembershipEntity
import com.example.transportapp.core.database.entity.NumberLeaseEntity
import com.example.transportapp.core.database.entity.NumberSeriesEntity
import com.example.transportapp.core.database.entity.PartyEntity
import com.example.transportapp.core.database.entity.PartyFtsEntity
import com.example.transportapp.core.database.entity.RateCardEntity
import com.example.transportapp.core.database.entity.ReceiptAllocationEntity
import com.example.transportapp.core.database.entity.ReceiptEntity
import com.example.transportapp.core.database.entity.RouteEntity
import com.example.transportapp.core.database.entity.StationEntity
import com.example.transportapp.core.database.entity.StatusEventEntity
import com.example.transportapp.core.database.entity.TripCostEntity
import com.example.transportapp.core.database.entity.TripEntity
import com.example.transportapp.core.database.entity.TripLegEntity
import com.example.transportapp.core.database.entity.VehicleEntity
import com.example.transportapp.core.database.outbox.OutboxDao
import com.example.transportapp.core.database.outbox.OutboxEntity
import com.example.transportapp.core.database.outbox.OutboxPrereqEntity
import com.example.transportapp.core.database.seed.SeedVersionDao
import com.example.transportapp.core.database.seed.SeedVersionEntity

/**
 * Phase 2 database (TransportApp.md §16.2). v1 = outbox skeleton (S1);
 * v2 = org tables (S2); v3 = the nine master tables + PARTY_FTS (S3);
 * v4 = charge-head/rate-card columns (S4); v5 = numbering + consignment aggregate (S5);
 * v6 = trip aggregate (S7); v7 = attachments + POD (S8); v8 = money (S9); v9 = TEMPLATE_E (S11);
 * v10 = COMPANY_SETTING_E (S14); v11 = CONSIGNMENT_E.amendment_reason (S15).
 */
@Database(
    entities = [
        OutboxEntity::class,
        OutboxPrereqEntity::class,
        SyncCursorEntity::class,
        SeedVersionEntity::class,
        CompanyEntity::class,
        BranchEntity::class,
        MembershipEntity::class,
        PartyEntity::class,
        StationEntity::class,
        RouteEntity::class,
        GoodsEntity::class,
        VehicleEntity::class,
        DriverEntity::class,
        BrokerEntity::class,
        ChargeHeadEntity::class,
        RateCardEntity::class,
        NumberSeriesEntity::class,
        NumberLeaseEntity::class,
        ConsignmentEntity::class,
        ConsignmentItemEntity::class,
        ChargeLineEntity::class,
        StatusEventEntity::class,
        DocSnapshotEntity::class,
        AttachmentEntity::class,
        PodEntity::class,
        TripEntity::class,
        TripLegEntity::class,
        TripCostEntity::class,
        LorryHireEntity::class,
        FreightBillEntity::class,
        CreditNoteEntity::class,
        ReceiptEntity::class,
        ReceiptAllocationEntity::class,
        com.example.transportapp.core.database.entity.TemplateEntity::class,
        com.example.transportapp.core.database.entity.CompanySettingEntity::class,
        PartyFtsEntity::class,
        ConsignmentFtsEntity::class,
    ],
    version = 12,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class TransportDatabase : RoomDatabase() {

    abstract fun outboxDao(): OutboxDao
    abstract fun syncCursorDao(): SyncCursorDao
    abstract fun seedVersionDao(): SeedVersionDao
    abstract fun orgDao(): OrgDao
    abstract fun mastersDao(): MastersDao
    abstract fun numberingDao(): NumberingDao
    abstract fun consignmentDao(): ConsignmentDao
    abstract fun tripDao(): TripDao
    abstract fun billingDao(): BillingDao
    abstract fun dashboardDao(): com.example.transportapp.core.database.dao.DashboardDao
    abstract fun reportsDao(): com.example.transportapp.core.database.dao.ReportsDao
    abstract fun templateDao(): com.example.transportapp.core.database.dao.TemplateDao
    abstract fun settingsDao(): com.example.transportapp.core.database.dao.SettingsDao

    companion object {
        /** Must match the Room database name called out in the spec; do not rename casually. */
        const val NAME = "transport.db"

        /** S1 → S2: add the org tables (COMPANY_E, BRANCH_E, MEMBERSHIP_E). */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS COMPANY_E (
                        local_id TEXT NOT NULL,
                        server_id TEXT,
                        updated_at_local INTEGER NOT NULL,
                        updated_at_server INTEGER,
                        sync_state TEXT NOT NULL,
                        deleted_at INTEGER,
                        name TEXT NOT NULL,
                        legal_name TEXT,
                        address TEXT,
                        gstin TEXT,
                        pan TEXT,
                        transporter_id TEXT,
                        gst_treatment TEXT NOT NULL,
                        display_bilty_series TEXT,
                        PRIMARY KEY(local_id)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_COMPANY_E_name` ON COMPANY_E (`name`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS BRANCH_E (
                        local_id TEXT NOT NULL,
                        server_id TEXT,
                        updated_at_local INTEGER NOT NULL,
                        updated_at_server INTEGER,
                        sync_state TEXT NOT NULL,
                        deleted_at INTEGER,
                        company_id TEXT NOT NULL,
                        name TEXT NOT NULL,
                        code TEXT NOT NULL,
                        address TEXT,
                        is_head_office INTEGER NOT NULL,
                        PRIMARY KEY(local_id),
                        FOREIGN KEY(company_id) REFERENCES COMPANY_E(local_id)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_BRANCH_E_company_id` ON BRANCH_E (`company_id`)")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_BRANCH_E_company_id_name` ON BRANCH_E (`company_id`, `name`)",
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS MEMBERSHIP_E (
                        local_id TEXT NOT NULL,
                        server_id TEXT,
                        updated_at_local INTEGER NOT NULL,
                        updated_at_server INTEGER,
                        sync_state TEXT NOT NULL,
                        deleted_at INTEGER,
                        company_id TEXT NOT NULL,
                        user_name TEXT NOT NULL,
                        user_email TEXT NOT NULL,
                        role TEXT NOT NULL,
                        branch_scope TEXT NOT NULL,
                        status TEXT NOT NULL,
                        invited_by TEXT,
                        invited_expires_at INTEGER,
                        display_expires TEXT,
                        PRIMARY KEY(local_id),
                        FOREIGN KEY(company_id) REFERENCES COMPANY_E(local_id)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_MEMBERSHIP_E_company_id` ON MEMBERSHIP_E (`company_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_MEMBERSHIP_E_user_email` ON MEMBERSHIP_E (`user_email`)")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_MEMBERSHIP_E_company_id_user_email_status` ON MEMBERSHIP_E (`company_id`, `user_email`, `status`)",
                )
            }
        }

        /** S2 → S3: add the nine master tables + the PARTY_FTS index (§16.2). */
        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS PARTY_E (
                        local_id TEXT NOT NULL,
                        server_id TEXT,
                        updated_at_local INTEGER NOT NULL,
                        updated_at_server INTEGER,
                        sync_state TEXT NOT NULL,
                        deleted_at INTEGER,
                        company_id TEXT NOT NULL,
                        name TEXT NOT NULL,
                        phone TEXT NOT NULL,
                        email TEXT,
                        type TEXT NOT NULL,
                        street_address TEXT,
                        station TEXT,
                        pincode TEXT,
                        gstin TEXT,
                        usual_route_id TEXT,
                        usual_payment_mode TEXT,
                        display_bilty_count INTEGER NOT NULL,
                        PRIMARY KEY(local_id)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_PARTY_E_company_id` ON PARTY_E (`company_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_PARTY_E_company_id_name` ON PARTY_E (`company_id`, `name`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_PARTY_E_phone` ON PARTY_E (`phone`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS STATION_E (
                        local_id TEXT NOT NULL,
                        server_id TEXT,
                        updated_at_local INTEGER NOT NULL,
                        updated_at_server INTEGER,
                        sync_state TEXT NOT NULL,
                        deleted_at INTEGER,
                        company_id TEXT NOT NULL,
                        name TEXT NOT NULL,
                        state TEXT,
                        PRIMARY KEY(local_id)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_STATION_E_company_id_name` ON STATION_E (`company_id`, `name`)",
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS ROUTE_E (
                        local_id TEXT NOT NULL,
                        server_id TEXT,
                        updated_at_local INTEGER NOT NULL,
                        updated_at_server INTEGER,
                        sync_state TEXT NOT NULL,
                        deleted_at INTEGER,
                        company_id TEXT NOT NULL,
                        origin_station_id TEXT NOT NULL,
                        dest_station_id TEXT NOT NULL,
                        distance_km INTEGER NOT NULL,
                        transit_days INTEGER NOT NULL,
                        PRIMARY KEY(local_id),
                        FOREIGN KEY(origin_station_id) REFERENCES STATION_E(local_id)
                            ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(dest_station_id) REFERENCES STATION_E(local_id)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ROUTE_E_company_id` ON ROUTE_E (`company_id`)")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_ROUTE_E_company_id_origin_station_id_dest_station_id` ON ROUTE_E (`company_id`, `origin_station_id`, `dest_station_id`)",
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS GOODS_E (
                        local_id TEXT NOT NULL,
                        server_id TEXT,
                        updated_at_local INTEGER NOT NULL,
                        updated_at_server INTEGER,
                        sync_state TEXT NOT NULL,
                        deleted_at INTEGER,
                        company_id TEXT NOT NULL,
                        name TEXT NOT NULL,
                        PRIMARY KEY(local_id)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_GOODS_E_company_id_name` ON GOODS_E (`company_id`, `name`)",
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS VEHICLE_E (
                        local_id TEXT NOT NULL,
                        server_id TEXT,
                        updated_at_local INTEGER NOT NULL,
                        updated_at_server INTEGER,
                        sync_state TEXT NOT NULL,
                        deleted_at INTEGER,
                        company_id TEXT NOT NULL,
                        number TEXT NOT NULL,
                        capacity_kg INTEGER NOT NULL,
                        ownership TEXT NOT NULL,
                        PRIMARY KEY(local_id)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_VEHICLE_E_company_id` ON VEHICLE_E (`company_id`)")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_VEHICLE_E_company_id_number` ON VEHICLE_E (`company_id`, `number`)",
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS DRIVER_E (
                        local_id TEXT NOT NULL,
                        server_id TEXT,
                        updated_at_local INTEGER NOT NULL,
                        updated_at_server INTEGER,
                        sync_state TEXT NOT NULL,
                        deleted_at INTEGER,
                        company_id TEXT NOT NULL,
                        name TEXT NOT NULL,
                        licence TEXT,
                        phone TEXT,
                        PRIMARY KEY(local_id)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_DRIVER_E_company_id` ON DRIVER_E (`company_id`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS BROKER_E (
                        local_id TEXT NOT NULL,
                        server_id TEXT,
                        updated_at_local INTEGER NOT NULL,
                        updated_at_server INTEGER,
                        sync_state TEXT NOT NULL,
                        deleted_at INTEGER,
                        company_id TEXT NOT NULL,
                        name TEXT NOT NULL,
                        phone TEXT,
                        PRIMARY KEY(local_id)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_BROKER_E_company_id` ON BROKER_E (`company_id`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS CHARGE_HEAD_E (
                        local_id TEXT NOT NULL,
                        server_id TEXT,
                        updated_at_local INTEGER NOT NULL,
                        updated_at_server INTEGER,
                        sync_state TEXT NOT NULL,
                        deleted_at INTEGER,
                        company_id TEXT NOT NULL,
                        code TEXT NOT NULL,
                        label TEXT NOT NULL,
                        basis TEXT NOT NULL,
                        display_value TEXT,
                        taxable INTEGER NOT NULL,
                        auto_apply INTEGER NOT NULL,
                        sort_order INTEGER NOT NULL,
                        PRIMARY KEY(local_id)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_CHARGE_HEAD_E_company_id` ON CHARGE_HEAD_E (`company_id`)")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_CHARGE_HEAD_E_company_id_code` ON CHARGE_HEAD_E (`company_id`, `code`)",
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS RATE_CARD_E (
                        local_id TEXT NOT NULL,
                        server_id TEXT,
                        updated_at_local INTEGER NOT NULL,
                        updated_at_server INTEGER,
                        sync_state TEXT NOT NULL,
                        deleted_at INTEGER,
                        company_id TEXT NOT NULL,
                        party_id TEXT,
                        route_id TEXT,
                        goods_id TEXT,
                        basis TEXT NOT NULL,
                        rate_paise INTEGER NOT NULL,
                        min_qty_label TEXT,
                        note TEXT,
                        sort_order INTEGER NOT NULL,
                        PRIMARY KEY(local_id),
                        FOREIGN KEY(party_id) REFERENCES PARTY_E(local_id)
                            ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(route_id) REFERENCES ROUTE_E(local_id)
                            ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(goods_id) REFERENCES GOODS_E(local_id)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_RATE_CARD_E_company_id` ON RATE_CARD_E (`company_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_RATE_CARD_E_party_id` ON RATE_CARD_E (`party_id`)")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_RATE_CARD_E_company_id_party_id_route_id_goods_id` ON RATE_CARD_E (`company_id`, `party_id`, `route_id`, `goods_id`)",
                )

                // External-content FTS4 over PARTY_E (unicode61 tokenizer), with Room's own
                // content-sync triggers — a fresh install gets these from Room; migrated
                // databases must create them here or the index silently goes stale.
                db.execSQL(
                    "CREATE VIRTUAL TABLE IF NOT EXISTS `PARTY_FTS` USING FTS4(`local_id` TEXT NOT NULL, `name` TEXT NOT NULL, `phone` TEXT NOT NULL, content=`PARTY_E`)",
                )
                db.execSQL(
                    "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_PARTY_FTS_BEFORE_UPDATE BEFORE UPDATE ON `PARTY_E` BEGIN DELETE FROM `PARTY_FTS` WHERE `docid`=OLD.`rowid`; END",
                )
                db.execSQL(
                    "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_PARTY_FTS_BEFORE_DELETE BEFORE DELETE ON `PARTY_E` BEGIN INSERT INTO `PARTY_FTS`(`docid`, `local_id`, `name`, `phone`) VALUES (OLD.`rowid`, OLD.`local_id`, OLD.`name`, OLD.`phone`); INSERT INTO `PARTY_FTS`(`docid`, `local_id`, `name`, `phone`) VALUES (-OLD.`rowid`, OLD.`local_id`, OLD.`name`, OLD.`phone`); DELETE FROM `PARTY_FTS` WHERE `docid`=-OLD.`rowid` OR `docid`=OLD.`rowid`; END",
                )
                db.execSQL(
                    "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_PARTY_FTS_AFTER_UPDATE AFTER UPDATE ON `PARTY_E` BEGIN INSERT INTO `PARTY_FTS`(`docid`, `local_id`, `name`, `phone`) VALUES (NEW.`rowid`, NEW.`local_id`, NEW.`name`, NEW.`phone`); END",
                )
                db.execSQL(
                    "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_PARTY_FTS_AFTER_INSERT AFTER INSERT ON `PARTY_E` BEGIN INSERT INTO `PARTY_FTS`(`docid`, `local_id`, `name`, `phone`) VALUES (NEW.`rowid`, NEW.`local_id`, NEW.`name`, NEW.`phone`); END",
                )
            }
        }

        /**
         * S3 → S4: the calculation engine's missing columns (Phase2.md §3.2 promised
         * "default, bearer" on CHARGE_HEAD_E and "min/max freight" on RATE_CARD_E).
         * ALTER TABLE ADD COLUMN cannot add NOT NULL without a default, so the head
         * columns carry neutral defaults that the v4 seeder immediately rewrites.
         */
        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE CHARGE_HEAD_E ADD COLUMN default_value_paise INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE CHARGE_HEAD_E ADD COLUMN bearer TEXT NOT NULL DEFAULT 'CONSIGNOR'")
                db.execSQL("ALTER TABLE RATE_CARD_E ADD COLUMN min_freight_paise INTEGER")
                db.execSQL("ALTER TABLE RATE_CARD_E ADD COLUMN max_freight_paise INTEGER")
            }
        }

        /**
         * S4 → S5: numbering (§9) and the consignment aggregate (§16.1). The common sync
         * envelope + tenant column block is inlined in each CREATE to keep the migration
         * SQL exactly mirroring the entity schema (D14) — no extra indexes, no reordering.
         */
        val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            private val envelope = "local_id TEXT NOT NULL, server_id TEXT, updated_at_local INTEGER NOT NULL, updated_at_server INTEGER, sync_state TEXT NOT NULL, deleted_at INTEGER, company_id TEXT NOT NULL"
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS NUMBER_SERIES_E (
                        $envelope,
                        branch_id TEXT NOT NULL,
                        doc_type TEXT NOT NULL,
                        prefix TEXT NOT NULL,
                        fy_part TEXT NOT NULL,
                        digits INTEGER NOT NULL,
                        last_issued INTEGER NOT NULL,
                        reset_rule TEXT NOT NULL,
                        PRIMARY KEY(local_id)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_NUMBER_SERIES_E_company_id_branch_id_doc_type` ON NUMBER_SERIES_E (`company_id`, `branch_id`, `doc_type`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS NUMBER_LEASE_E (
                        local_id TEXT NOT NULL, server_id TEXT, updated_at_local INTEGER NOT NULL, updated_at_server INTEGER, sync_state TEXT NOT NULL, deleted_at INTEGER,
                        series_id TEXT NOT NULL,
                        device_id TEXT NOT NULL,
                        range_start INTEGER NOT NULL,
                        range_end INTEGER NOT NULL,
                        next_value INTEGER NOT NULL,
                        expires_at INTEGER NOT NULL,
                        PRIMARY KEY(local_id)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_NUMBER_LEASE_E_series_id` ON NUMBER_LEASE_E (`series_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_NUMBER_LEASE_E_series_id_device_id` ON NUMBER_LEASE_E (`series_id`, `device_id`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS CONSIGNMENT_E (
                        $envelope,
                        series_id TEXT NOT NULL,
                        bilty_no TEXT,
                        provisional_no TEXT,
                        status_projection TEXT NOT NULL,
                        booking_branch_id TEXT NOT NULL,
                        dest_branch_id TEXT,
                        consignor_id TEXT NOT NULL,
                        consignee_id TEXT NOT NULL,
                        route_id TEXT NOT NULL,
                        from_station_id TEXT NOT NULL,
                        to_station_id TEXT NOT NULL,
                        payment_mode TEXT NOT NULL,
                        risk TEXT NOT NULL,
                        delivery_type TEXT NOT NULL,
                        place_of_supply_state TEXT,
                        eway_bill_no TEXT,
                        private_mark TEXT,
                        packages INTEGER NOT NULL,
                        actual_weight_g INTEGER NOT NULL,
                        chargeable_weight_g INTEGER NOT NULL,
                        declared_value_paise INTEGER NOT NULL,
                        freight_paise INTEGER NOT NULL,
                        gst_paise INTEGER NOT NULL,
                        total_paise INTEGER NOT NULL,
                        booked_at INTEGER NOT NULL,
                        booked_by_name TEXT NOT NULL,
                        expected_arrival INTEGER NOT NULL,
                        party_names TEXT NOT NULL,
                        freight_bill_id TEXT,
                        amends_id TEXT,
                        PRIMARY KEY(local_id)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_CONSIGNMENT_E_company_id` ON CONSIGNMENT_E (`company_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_CONSIGNMENT_E_series_id` ON CONSIGNMENT_E (`series_id`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_CONSIGNMENT_E_company_id_bilty_no` ON CONSIGNMENT_E (`company_id`, `bilty_no`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_CONSIGNMENT_E_company_id_booking_branch_id_booked_at` ON CONSIGNMENT_E (`company_id`, `booking_branch_id`, `booked_at`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_CONSIGNMENT_E_company_id_status_projection_expected_arrival` ON CONSIGNMENT_E (`company_id`, `status_projection`, `expected_arrival`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_CONSIGNMENT_E_company_id_payment_mode_status_projection` ON CONSIGNMENT_E (`company_id`, `payment_mode`, `status_projection`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_CONSIGNMENT_E_company_id_consignor_id_freight_bill_id` ON CONSIGNMENT_E (`company_id`, `consignor_id`, `freight_bill_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_CONSIGNMENT_E_company_id_consignee_id_freight_bill_id` ON CONSIGNMENT_E (`company_id`, `consignee_id`, `freight_bill_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_CONSIGNMENT_E_route_id` ON CONSIGNMENT_E (`route_id`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS CONSIGNMENT_ITEM_E (
                        local_id TEXT NOT NULL, server_id TEXT, updated_at_local INTEGER NOT NULL, updated_at_server INTEGER, sync_state TEXT NOT NULL, deleted_at INTEGER,
                        consignment_id TEXT NOT NULL,
                        goods_id TEXT,
                        description TEXT NOT NULL,
                        packages INTEGER NOT NULL,
                        actual_weight_g INTEGER NOT NULL,
                        chargeable_weight_g INTEGER NOT NULL,
                        rate_paise INTEGER,
                        basis TEXT,
                        freight_paise INTEGER NOT NULL,
                        PRIMARY KEY(local_id),
                        FOREIGN KEY(consignment_id) REFERENCES CONSIGNMENT_E(local_id)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_CONSIGNMENT_ITEM_E_consignment_id` ON CONSIGNMENT_ITEM_E (`consignment_id`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS CHARGE_LINE_E (
                        local_id TEXT NOT NULL, server_id TEXT, updated_at_local INTEGER NOT NULL, updated_at_server INTEGER, sync_state TEXT NOT NULL, deleted_at INTEGER,
                        consignment_id TEXT NOT NULL,
                        head_code TEXT NOT NULL,
                        label TEXT NOT NULL,
                        basis TEXT NOT NULL,
                        input_value TEXT,
                        computed_paise INTEGER NOT NULL,
                        taxable INTEGER NOT NULL,
                        sort_order INTEGER NOT NULL,
                        PRIMARY KEY(local_id),
                        FOREIGN KEY(consignment_id) REFERENCES CONSIGNMENT_E(local_id)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_CHARGE_LINE_E_consignment_id` ON CHARGE_LINE_E (`consignment_id`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS STATUS_EVENT_E (
                        local_id TEXT NOT NULL, server_id TEXT, updated_at_local INTEGER NOT NULL, updated_at_server INTEGER, sync_state TEXT NOT NULL, deleted_at INTEGER, company_id TEXT NOT NULL,
                        consignment_id TEXT NOT NULL,
                        client_event_id TEXT NOT NULL,
                        event_type TEXT NOT NULL,
                        occurred_at INTEGER NOT NULL,
                        recorded_at INTEGER NOT NULL,
                        actor_member_id TEXT,
                        actor_name TEXT NOT NULL,
                        branch_id TEXT NOT NULL,
                        location TEXT,
                        photo_ref TEXT,
                        reason_code TEXT,
                        remark TEXT,
                        challan_ref TEXT,
                        PRIMARY KEY(local_id),
                        FOREIGN KEY(consignment_id) REFERENCES CONSIGNMENT_E(local_id)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_STATUS_EVENT_E_consignment_id_occurred_at` ON STATUS_EVENT_E (`consignment_id`, `occurred_at`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_STATUS_EVENT_E_company_id_client_event_id` ON STATUS_EVENT_E (`company_id`, `client_event_id`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS DOC_SNAPSHOT_E (
                        local_id TEXT NOT NULL, server_id TEXT, updated_at_local INTEGER NOT NULL, updated_at_server INTEGER, sync_state TEXT NOT NULL, deleted_at INTEGER,
                        consignment_id TEXT NOT NULL,
                        document_type TEXT NOT NULL,
                        template_id TEXT NOT NULL,
                        template_version TEXT NOT NULL,
                        version INTEGER NOT NULL,
                        payload_json TEXT NOT NULL,
                        content_hash TEXT NOT NULL,
                        copy_count INTEGER NOT NULL,
                        PRIMARY KEY(local_id),
                        FOREIGN KEY(consignment_id) REFERENCES CONSIGNMENT_E(local_id)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_DOC_SNAPSHOT_E_consignment_id_version` ON DOC_SNAPSHOT_E (`consignment_id`, `version`)")

                db.execSQL(
                    "CREATE VIRTUAL TABLE IF NOT EXISTS `CONSIGNMENT_FTS` USING FTS4(`local_id` TEXT NOT NULL, `bilty_no` TEXT NOT NULL, `party_names` TEXT NOT NULL, content=`CONSIGNMENT_E`)",
                )
                db.execSQL(
                    "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_CONSIGNMENT_FTS_BEFORE_UPDATE BEFORE UPDATE ON `CONSIGNMENT_E` BEGIN DELETE FROM `CONSIGNMENT_FTS` WHERE `docid`=OLD.`rowid`; END",
                )
                db.execSQL(
                    "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_CONSIGNMENT_FTS_BEFORE_DELETE BEFORE DELETE ON `CONSIGNMENT_E` BEGIN INSERT INTO `CONSIGNMENT_FTS`(`docid`, `local_id`, `bilty_no`, `party_names`) VALUES (OLD.`rowid`, OLD.`local_id`, OLD.`bilty_no`, OLD.`party_names`); INSERT INTO `CONSIGNMENT_FTS`(`docid`, `local_id`, `bilty_no`, `party_names`) VALUES (-OLD.`rowid`, OLD.`local_id`, OLD.`bilty_no`, OLD.`party_names`); DELETE FROM `CONSIGNMENT_FTS` WHERE `docid`=-OLD.`rowid` OR `docid`=OLD.`rowid`; END",
                )
                db.execSQL(
                    "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_CONSIGNMENT_FTS_AFTER_UPDATE AFTER UPDATE ON `CONSIGNMENT_E` BEGIN INSERT INTO `CONSIGNMENT_FTS`(`docid`, `local_id`, `bilty_no`, `party_names`) VALUES (NEW.`rowid`, NEW.`local_id`, NEW.`bilty_no`, NEW.`party_names`); END",
                )
                db.execSQL(
                    "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_CONSIGNMENT_FTS_AFTER_INSERT AFTER INSERT ON `CONSIGNMENT_E` BEGIN INSERT INTO `CONSIGNMENT_FTS`(`docid`, `local_id`, `bilty_no`, `party_names`) VALUES (NEW.`rowid`, NEW.`local_id`, NEW.`bilty_no`, NEW.`party_names`); END",
                )
            }
        }

        /** S5 → S7: the trip aggregate (§11). SQL mirrors the entity schema exactly (D14). */
        val MIGRATION_5_6: Migration = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS TRIP_E (
                        local_id TEXT NOT NULL, server_id TEXT, updated_at_local INTEGER NOT NULL, updated_at_server INTEGER, sync_state TEXT NOT NULL, deleted_at INTEGER, company_id TEXT NOT NULL,
                        series_id TEXT NOT NULL,
                        challan_no TEXT,
                        state TEXT NOT NULL,
                        vehicle_id TEXT NOT NULL,
                        driver_id TEXT NOT NULL,
                        origin_branch_id TEXT NOT NULL,
                        dest_station_id TEXT NOT NULL,
                        via_stations TEXT,
                        hire_paise INTEGER NOT NULL,
                        advance_paise INTEGER NOT NULL,
                        balance_paise INTEGER NOT NULL,
                        expected_arrival INTEGER NOT NULL,
                        created_at INTEGER NOT NULL,
                        created_by_name TEXT NOT NULL,
                        dispatched_at INTEGER,
                        closed_at INTEGER,
                        cancel_reason TEXT,
                        PRIMARY KEY(local_id)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_TRIP_E_company_id` ON TRIP_E (`company_id`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_TRIP_E_company_id_challan_no` ON TRIP_E (`company_id`, `challan_no`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_TRIP_E_vehicle_id` ON TRIP_E (`vehicle_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_TRIP_E_company_id_state` ON TRIP_E (`company_id`, `state`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS TRIP_LEG_E (
                        local_id TEXT NOT NULL, server_id TEXT, updated_at_local INTEGER NOT NULL, updated_at_server INTEGER, sync_state TEXT NOT NULL, deleted_at INTEGER,
                        trip_id TEXT NOT NULL,
                        consignment_id TEXT NOT NULL,
                        leg_no INTEGER NOT NULL,
                        loaded_at INTEGER NOT NULL,
                        PRIMARY KEY(local_id),
                        FOREIGN KEY(trip_id) REFERENCES TRIP_E(local_id)
                            ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(consignment_id) REFERENCES CONSIGNMENT_E(local_id)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_TRIP_LEG_E_trip_id_consignment_id` ON TRIP_LEG_E (`trip_id`, `consignment_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_TRIP_LEG_E_consignment_id` ON TRIP_LEG_E (`consignment_id`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS TRIP_COST_E (
                        local_id TEXT NOT NULL, server_id TEXT, updated_at_local INTEGER NOT NULL, updated_at_server INTEGER, sync_state TEXT NOT NULL, deleted_at INTEGER,
                        trip_id TEXT NOT NULL,
                        vehicle_id TEXT NOT NULL,
                        head TEXT NOT NULL,
                        incurred_on INTEGER NOT NULL,
                        amount_paise INTEGER NOT NULL,
                        payment_mode TEXT NOT NULL,
                        remark TEXT NOT NULL,
                        PRIMARY KEY(local_id),
                        FOREIGN KEY(trip_id) REFERENCES TRIP_E(local_id)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_TRIP_COST_E_trip_id` ON TRIP_COST_E (`trip_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_TRIP_COST_E_vehicle_id` ON TRIP_COST_E (`vehicle_id`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS LORRY_HIRE_E (
                        local_id TEXT NOT NULL, server_id TEXT, updated_at_local INTEGER NOT NULL, updated_at_server INTEGER, sync_state TEXT NOT NULL, deleted_at INTEGER,
                        trip_id TEXT NOT NULL,
                        owner_party_id TEXT,
                        broker_id TEXT,
                        hire_paise INTEGER NOT NULL,
                        advance_paise INTEGER NOT NULL,
                        deductions_paise INTEGER NOT NULL,
                        balance_paise INTEGER NOT NULL,
                        PRIMARY KEY(local_id),
                        FOREIGN KEY(trip_id) REFERENCES TRIP_E(local_id)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_LORRY_HIRE_E_trip_id` ON LORRY_HIRE_E (`trip_id`)")
            }
        }

        /** S7 → S8: attachments and POD (§4.1). SQL mirrors the entity schema exactly (D14). */
        val MIGRATION_6_7: Migration = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS ATTACHMENT_E (
                        local_id TEXT NOT NULL, server_id TEXT, updated_at_local INTEGER NOT NULL, updated_at_server INTEGER, sync_state TEXT NOT NULL, deleted_at INTEGER,
                        consignment_id TEXT NOT NULL,
                        kind TEXT NOT NULL,
                        file_ref TEXT NOT NULL,
                        size_bytes INTEGER NOT NULL,
                        caption TEXT,
                        PRIMARY KEY(local_id),
                        FOREIGN KEY(consignment_id) REFERENCES CONSIGNMENT_E(local_id)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ATTACHMENT_E_consignment_id` ON ATTACHMENT_E (`consignment_id`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS POD_E (
                        local_id TEXT NOT NULL, server_id TEXT, updated_at_local INTEGER NOT NULL, updated_at_server INTEGER, sync_state TEXT NOT NULL, deleted_at INTEGER,
                        consignment_id TEXT NOT NULL,
                        consignee_name TEXT NOT NULL,
                        signature_ref TEXT,
                        photo_ref TEXT,
                        pod_date INTEGER NOT NULL,
                        remarks TEXT,
                        PRIMARY KEY(local_id),
                        FOREIGN KEY(consignment_id) REFERENCES CONSIGNMENT_E(local_id)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_POD_E_consignment_id` ON POD_E (`consignment_id`)")
            }
        }

        /**
         * S8 → S9: money (§12, §16.1) — FREIGHT_BILL_E, CREDIT_NOTE_E, RECEIPT_E,
         * RECEIPT_ALLOCATION_E. SQL mirrors the entity schema exactly (D14).
         */
        val MIGRATION_7_8: Migration = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS FREIGHT_BILL_E (
                        local_id TEXT NOT NULL, server_id TEXT, updated_at_local INTEGER NOT NULL, updated_at_server INTEGER, sync_state TEXT NOT NULL, deleted_at INTEGER, company_id TEXT NOT NULL,
                        series_id TEXT NOT NULL,
                        bill_no TEXT,
                        state TEXT NOT NULL,
                        party_id TEXT NOT NULL,
                        period_start INTEGER NOT NULL,
                        period_end INTEGER NOT NULL,
                        due_at INTEGER,
                        freight_paise INTEGER NOT NULL,
                        other_charges_paise INTEGER NOT NULL,
                        taxable_paise INTEGER NOT NULL,
                        gst_paise INTEGER NOT NULL,
                        total_paise INTEGER NOT NULL,
                        gst_treatment TEXT NOT NULL,
                        notes TEXT,
                        issued_at INTEGER,
                        issued_by_name TEXT,
                        cancelled_at INTEGER,
                        PRIMARY KEY(local_id)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_FREIGHT_BILL_E_company_id` ON FREIGHT_BILL_E (`company_id`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_FREIGHT_BILL_E_company_id_bill_no` ON FREIGHT_BILL_E (`company_id`, `bill_no`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_FREIGHT_BILL_E_company_id_party_id_state` ON FREIGHT_BILL_E (`company_id`, `party_id`, `state`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_FREIGHT_BILL_E_company_id_state` ON FREIGHT_BILL_E (`company_id`, `state`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS CREDIT_NOTE_E (
                        local_id TEXT NOT NULL, server_id TEXT, updated_at_local INTEGER NOT NULL, updated_at_server INTEGER, sync_state TEXT NOT NULL, deleted_at INTEGER, company_id TEXT NOT NULL,
                        series_id TEXT NOT NULL,
                        note_no TEXT,
                        freight_bill_id TEXT NOT NULL,
                        party_id TEXT NOT NULL,
                        reason TEXT NOT NULL,
                        amount_paise INTEGER NOT NULL,
                        replacement_bill_id TEXT,
                        created_at INTEGER NOT NULL,
                        created_by_name TEXT NOT NULL,
                        PRIMARY KEY(local_id)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_CREDIT_NOTE_E_company_id` ON CREDIT_NOTE_E (`company_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_CREDIT_NOTE_E_freight_bill_id` ON CREDIT_NOTE_E (`freight_bill_id`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS RECEIPT_E (
                        local_id TEXT NOT NULL, server_id TEXT, updated_at_local INTEGER NOT NULL, updated_at_server INTEGER, sync_state TEXT NOT NULL, deleted_at INTEGER, company_id TEXT NOT NULL,
                        series_id TEXT NOT NULL,
                        receipt_no TEXT,
                        party_id TEXT NOT NULL,
                        amount_paise INTEGER NOT NULL,
                        instrument TEXT NOT NULL,
                        instrument_ref TEXT,
                        received_at INTEGER NOT NULL,
                        received_at_branch_id TEXT NOT NULL,
                        received_by_name TEXT NOT NULL,
                        notes TEXT,
                        PRIMARY KEY(local_id)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_RECEIPT_E_company_id` ON RECEIPT_E (`company_id`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_RECEIPT_E_company_id_receipt_no` ON RECEIPT_E (`company_id`, `receipt_no`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_RECEIPT_E_company_id_party_id_received_at` ON RECEIPT_E (`company_id`, `party_id`, `received_at`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS RECEIPT_ALLOCATION_E (
                        local_id TEXT NOT NULL, server_id TEXT, updated_at_local INTEGER NOT NULL, updated_at_server INTEGER, sync_state TEXT NOT NULL, deleted_at INTEGER, company_id TEXT NOT NULL,
                        receipt_id TEXT NOT NULL,
                        target_type TEXT NOT NULL,
                        bill_id TEXT,
                        consignment_id TEXT,
                        amount_paise INTEGER NOT NULL,
                        PRIMARY KEY(local_id)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_RECEIPT_ALLOCATION_E_receipt_id` ON RECEIPT_ALLOCATION_E (`receipt_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_RECEIPT_ALLOCATION_E_bill_id` ON RECEIPT_ALLOCATION_E (`bill_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_RECEIPT_ALLOCATION_E_consignment_id` ON RECEIPT_ALLOCATION_E (`consignment_id`)")
            }
        }

        /**
         * S9 → S11: TEMPLATE_E (Phase 3 S11) — templates as data, versions as rows.
         * SQL mirrors the entity schema exactly (D14).
         */
        val MIGRATION_8_9: Migration = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS TEMPLATE_E (
                        local_id TEXT NOT NULL, server_id TEXT, updated_at_local INTEGER NOT NULL, updated_at_server INTEGER, sync_state TEXT NOT NULL, deleted_at INTEGER, company_id TEXT NOT NULL,
                        template_key TEXT NOT NULL,
                        version INTEGER NOT NULL,
                        is_active INTEGER NOT NULL,
                        schema_version INTEGER NOT NULL,
                        content_json TEXT NOT NULL,
                        content_hash TEXT NOT NULL,
                        visibility TEXT NOT NULL,
                        created_by_name TEXT NOT NULL,
                        PRIMARY KEY(local_id)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_TEMPLATE_E_company_id` ON TEMPLATE_E (`company_id`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_TEMPLATE_E_company_id_template_key_version` ON TEMPLATE_E (`company_id`, `template_key`, `version`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_TEMPLATE_E_company_id_is_active` ON TEMPLATE_E (`company_id`, `is_active`)")
            }
        }

        /**
         * S11 → S14: COMPANY_SETTING_E (Phase 3 S14) — dated company calculation settings.
         * SQL mirrors the entity schema exactly (D14).
         */
        val MIGRATION_9_10: Migration = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS COMPANY_SETTING_E (
                        local_id TEXT NOT NULL, server_id TEXT, updated_at_local INTEGER NOT NULL, updated_at_server INTEGER, sync_state TEXT NOT NULL, deleted_at INTEGER, company_id TEXT NOT NULL,
                        effective_from INTEGER NOT NULL,
                        gst_rate_bp INTEGER NOT NULL,
                        weight_step_g INTEGER NOT NULL,
                        volumetric_divisor_g INTEGER,
                        gst_treatment TEXT NOT NULL,
                        rounding TEXT NOT NULL,
                        created_by_name TEXT NOT NULL,
                        PRIMARY KEY(local_id)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_COMPANY_SETTING_E_company_id` ON COMPANY_SETTING_E (`company_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_COMPANY_SETTING_E_company_id_effective_from` ON COMPANY_SETTING_E (`company_id`, `effective_from`)")
            }
        }

        /**
         * S14 - S15: CONSIGNMENT_E gains amendment_reason (§16.1 - an amendment is another
         * consignment row with its reason carried on the amendment itself).
         */
        val MIGRATION_10_11: Migration = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE CONSIGNMENT_E ADD COLUMN amendment_reason TEXT")
            }
        }

        /**
         * S22 (D60): COMPANY_E gains logo_ref — the relative file ref of the company's
         * logo, printed in letterheads. Null until the Owner uploads one.
         */
        val MIGRATION_11_12: Migration = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE COMPANY_E ADD COLUMN logo_ref TEXT")
            }
        }

        val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)
    }
}
