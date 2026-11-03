package com.example.powerbill.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {Consumer.class, ElectricityBill.class, Payment.class},
          version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract BillDao billDao();
    public abstract PaymentDao paymentDao();

    private static volatile AppDatabase INSTANCE;

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE consumers ADD COLUMN notes TEXT");
            db.execSQL("ALTER TABLE electricity_bills ADD COLUMN notes TEXT");
            db.execSQL("ALTER TABLE electricity_bills ADD COLUMN billImageUri TEXT");
            // Avoid migration failure if older data already has duplicates; report and resolve them first.
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_electricity_bills_consumerId_billingMonth " +
                       "ON electricity_bills(consumerId, billingMonth)");
        }
    };

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE payments ADD COLUMN transactionReference TEXT");
            db.execSQL("ALTER TABLE payments ADD COLUMN notes TEXT");
            db.execSQL("ALTER TABLE payments ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE payments ADD COLUMN voided INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE payments ADD COLUMN voidReason TEXT");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_payments_transactionReference ON payments(transactionReference)");
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class, "powerbill.db"
                    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build();
                }
            }
        }
        return INSTANCE;
    }
}
