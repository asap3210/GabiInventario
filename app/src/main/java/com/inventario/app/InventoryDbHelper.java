package com.inventario.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Gestiona la base de datos local SQLite.
 * Tabla "inventario": cada fila relaciona un OBJETO con una SALA y guarda timestamps.
 * El código de objeto es UNIQUE, lo que permite detectar duplicados.
 */
public class InventoryDbHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "inventario.db";
    public static final int DB_VERSION = 1;

    public static final String TABLE = "inventario";
    public static final String COL_ID = "_id";
    public static final String COL_OBJECT = "object_code";
    public static final String COL_ROOM = "room_code";
    public static final String COL_FIRST = "first_seen";
    public static final String COL_LAST = "last_seen";
    public static final String COL_COUNT = "scan_count";

    public InventoryDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_OBJECT + " TEXT NOT NULL UNIQUE, " +
                COL_ROOM + " TEXT NOT NULL, " +
                COL_FIRST + " TEXT NOT NULL, " +
                COL_LAST + " TEXT NOT NULL, " +
                COL_COUNT + " INTEGER NOT NULL DEFAULT 1" +
                ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }

    /** Devuelve la fila existente para un código de objeto, o null si no existe. */
    public InventoryItem findByObject(String objectCode) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE, null, COL_OBJECT + "=?",
                new String[]{objectCode}, null, null, null);
        InventoryItem item = null;
        if (c.moveToFirst()) {
            item = cursorToItem(c);
        }
        c.close();
        return item;
    }

    /** Inserta un objeto nuevo en una sala. Devuelve el id, o -1 si falla (p. ej. duplicado). */
    public long insertObject(String objectCode, String roomCode, String timestamp) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(COL_OBJECT, objectCode);
        v.put(COL_ROOM, roomCode);
        v.put(COL_FIRST, timestamp);
        v.put(COL_LAST, timestamp);
        v.put(COL_COUNT, 1);
        return db.insert(TABLE, null, v);
    }

    /** Actualiza la sala de un objeto ya existente y refresca el último timestamp y el contador. */
    public void updateObjectRoom(String objectCode, String newRoom, String timestamp) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("UPDATE " + TABLE + " SET " +
                        COL_ROOM + "=?, " + COL_LAST + "=?, " +
                        COL_COUNT + "=" + COL_COUNT + "+1 WHERE " + COL_OBJECT + "=?",
                new Object[]{newRoom, timestamp, objectCode});
    }

    /** Devuelve todas las filas, las más recientes primero. */
    public List<InventoryItem> getAll() {
        List<InventoryItem> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE, null, null, null, null, null, COL_LAST + " DESC");
        while (c.moveToNext()) {
            list.add(cursorToItem(c));
        }
        c.close();
        return list;
    }

    public int count() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE, null);
        int n = 0;
        if (c.moveToFirst()) n = c.getInt(0);
        c.close();
        return n;
    }

    public void deleteAll() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE, null, null);
    }

    private InventoryItem cursorToItem(Cursor c) {
        return new InventoryItem(
                c.getLong(c.getColumnIndexOrThrow(COL_ID)),
                c.getString(c.getColumnIndexOrThrow(COL_OBJECT)),
                c.getString(c.getColumnIndexOrThrow(COL_ROOM)),
                c.getString(c.getColumnIndexOrThrow(COL_FIRST)),
                c.getString(c.getColumnIndexOrThrow(COL_LAST)),
                c.getInt(c.getColumnIndexOrThrow(COL_COUNT))
        );
    }
}
