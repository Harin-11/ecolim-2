package com.madrigalsolu.ecolim.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.madrigalsolu.ecolim.model.Residuo;

import java.util.ArrayList;
import java.util.List;

/**
 * Base de datos SQLite local para almacenamiento sin conexión (offline-first).
 * Almacena registros de recolección de residuos y credenciales de usuario.
 */
public class ResiduoDBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "ecolim_local.db";
    private static final int DATABASE_VERSION = 2;

    // Tabla Residuos
    public static final String TABLE_RESIDUOS = "residuos";
    public static final String COL_ID = "id";
    public static final String COL_TIPO = "tipo_residuo";
    public static final String COL_CANTIDAD = "cantidad_kg";
    public static final String COL_UBICACION = "ubicacion";
    public static final String COL_FECHA = "fecha";
    public static final String COL_HORA = "hora";
    public static final String COL_OBSERVACIONES = "observaciones";
    public static final String COL_SINCRONIZADO = "sincronizado";
    public static final String COL_FIREBASE_KEY = "firebase_key";

    // Tabla Usuarios
    public static final String TABLE_USUARIOS = "usuarios";
    public static final String COL_USER_ID = "id";
    public static final String COL_USER_NOMBRES = "nombres";
    public static final String COL_USER_APELLIDOS = "apellidos";
    public static final String COL_USER_DNI = "dni";
    public static final String COL_USER_CORREO = "correo";
    public static final String COL_USER_PASSWORD = "password";
    public static final String COL_USER_FECHA = "fecha_registro";

    private static final String SQL_CREATE_TABLE_RESIDUOS =
            "CREATE TABLE " + TABLE_RESIDUOS + " (" +
                    COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_TIPO + " TEXT NOT NULL, " +
                    COL_CANTIDAD + " REAL NOT NULL, " +
                    COL_UBICACION + " TEXT, " +
                    COL_FECHA + " TEXT NOT NULL, " +
                    COL_HORA + " TEXT NOT NULL, " +
                    COL_OBSERVACIONES + " TEXT, " +
                    COL_SINCRONIZADO + " INTEGER DEFAULT 0, " +
                    COL_FIREBASE_KEY + " TEXT" +
                    ");";

    private static final String SQL_CREATE_TABLE_USUARIOS =
            "CREATE TABLE " + TABLE_USUARIOS + " (" +
                    COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_USER_NOMBRES + " TEXT NOT NULL, " +
                    COL_USER_APELLIDOS + " TEXT, " +
                    COL_USER_DNI + " TEXT, " +
                    COL_USER_CORREO + " TEXT UNIQUE NOT NULL, " +
                    COL_USER_PASSWORD + " TEXT NOT NULL, " +
                    COL_USER_FECHA + " TEXT NOT NULL" +
                    ");";

    public ResiduoDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE_RESIDUOS);
        db.execSQL(SQL_CREATE_TABLE_USUARIOS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RESIDUOS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USUARIOS);
        onCreate(db);
    }

    // ==========================================
    // OPERACIONES DE RESIDUOS
    // ==========================================

    public long insertarResiduo(Residuo r) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TIPO, r.getTipoResiduo());
        values.put(COL_CANTIDAD, r.getCantidadKg());
        values.put(COL_UBICACION, r.getUbicacion());
        values.put(COL_FECHA, r.getFecha());
        values.put(COL_HORA, r.getHora());
        values.put(COL_OBSERVACIONES, r.getObservaciones());
        values.put(COL_SINCRONIZADO, r.getSincronizado());
        values.put(COL_FIREBASE_KEY, r.getFirebaseKey());
        long id = db.insert(TABLE_RESIDUOS, null, values);
        db.close();
        return id;
    }

    public List<Residuo> listarTodos() {
        return listarConFiltro(null, null, null);
    }

    public List<Residuo> listarConFiltro(String tipoResiduo, String fechaDesde, String fechaHasta) {
        List<Residuo> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        StringBuilder selection = new StringBuilder("1=1");
        List<String> args = new ArrayList<>();

        if (tipoResiduo != null && !tipoResiduo.equalsIgnoreCase("Todos")) {
            selection.append(" AND ").append(COL_TIPO).append(" = ?");
            args.add(tipoResiduo);
        }
        if (fechaDesde != null && !fechaDesde.isEmpty()) {
            selection.append(" AND ").append(COL_FECHA).append(" >= ?");
            args.add(fechaDesde);
        }
        if (fechaHasta != null && !fechaHasta.isEmpty()) {
            selection.append(" AND ").append(COL_FECHA).append(" <= ?");
            args.add(fechaHasta);
        }

        Cursor cursor = db.query(TABLE_RESIDUOS, null, selection.toString(),
                args.toArray(new String[0]), null, null,
                COL_FECHA + " DESC, " + COL_HORA + " DESC");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                lista.add(cursorToResiduo(cursor));
            }
            cursor.close();
        }
        db.close();
        return lista;
    }

    public List<Residuo> listarPendientesSincronizar() {
        List<Residuo> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_RESIDUOS, null,
                COL_SINCRONIZADO + " = 0", null, null, null, COL_ID + " ASC");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                lista.add(cursorToResiduo(cursor));
            }
            cursor.close();
        }
        db.close();
        return lista;
    }

    public void marcarComoSincronizado(long id, String firebaseKey) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_SINCRONIZADO, 1);
        if (firebaseKey != null) {
            values.put(COL_FIREBASE_KEY, firebaseKey);
        }
        db.update(TABLE_RESIDUOS, values, COL_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public int eliminarResiduo(long id) {
        SQLiteDatabase db = getWritableDatabase();
        int deleted = db.delete(TABLE_RESIDUOS, COL_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
        return deleted;
    }

    public double obtenerTotalKg() {
        double total = 0;
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(" + COL_CANTIDAD + ") FROM " + TABLE_RESIDUOS, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                total = cursor.getDouble(0);
            }
            cursor.close();
        }
        db.close();
        return total;
    }

    public int obtenerConteoTotal() {
        int count = 0;
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_RESIDUOS, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            cursor.close();
        }
        db.close();
        return count;
    }


    private Residuo cursorToResiduo(Cursor cursor) {
        Residuo r = new Residuo();
        r.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)));
        r.setTipoResiduo(cursor.getString(cursor.getColumnIndexOrThrow(COL_TIPO)));
        r.setCantidadKg(cursor.getDouble(cursor.getColumnIndexOrThrow(COL_CANTIDAD)));
        r.setUbicacion(cursor.getString(cursor.getColumnIndexOrThrow(COL_UBICACION)));
        r.setFecha(cursor.getString(cursor.getColumnIndexOrThrow(COL_FECHA)));
        r.setHora(cursor.getString(cursor.getColumnIndexOrThrow(COL_HORA)));
        r.setObservaciones(cursor.getString(cursor.getColumnIndexOrThrow(COL_OBSERVACIONES)));
        r.setSincronizado(cursor.getInt(cursor.getColumnIndexOrThrow(COL_SINCRONIZADO)));
        int keyIndex = cursor.getColumnIndex(COL_FIREBASE_KEY);
        if (keyIndex != -1) {
            r.setFirebaseKey(cursor.getString(keyIndex));
        }
        return r;
    }

    // ==========================================
    // OPERACIONES DE USUARIOS (GESTION502)
    // ==========================================

    public boolean registrarUsuario(String nombres, String apellidos, String dni, String correo, String password) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_USER_NOMBRES, nombres);
        values.put(COL_USER_APELLIDOS, apellidos != null ? apellidos : "");
        values.put(COL_USER_DNI, dni != null ? dni : "");
        values.put(COL_USER_CORREO, correo.toLowerCase().trim());
        values.put(COL_USER_PASSWORD, password);
        values.put(COL_USER_FECHA, java.text.DateFormat.getDateTimeInstance().format(new java.util.Date()));
        long id = db.insertWithOnConflict(TABLE_USUARIOS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
        return id != -1;
    }

    public boolean validarCredenciales(String correo, String password) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COL_USER_ID + " FROM " + TABLE_USUARIOS +
                        " WHERE LOWER(" + COL_USER_CORREO + ") = ? AND " + COL_USER_PASSWORD + " = ?",
                new String[]{correo.toLowerCase().trim(), password});
        boolean existe = (cursor != null && cursor.getCount() > 0);
        if (cursor != null) cursor.close();
        db.close();
        return existe;
    }

    public String obtenerNombreCompleto(String correo) {
        String nombre = "Operario Ecolim";
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COL_USER_NOMBRES + ", " + COL_USER_APELLIDOS +
                        " FROM " + TABLE_USUARIOS + " WHERE LOWER(" + COL_USER_CORREO + ") = ?",
                new String[]{correo.toLowerCase().trim()});
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                String n = cursor.getString(0);
                String a = cursor.getString(1);
                nombre = (n + (a != null && !a.isEmpty() ? " " + a : "")).trim();
            }
            cursor.close();
        }
        db.close();
        return nombre;
    }

    public String obtenerDni(String correo) {
        String dni = "";
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COL_USER_DNI +
                        " FROM " + TABLE_USUARIOS + " WHERE LOWER(" + COL_USER_CORREO + ") = ?",
                new String[]{correo.toLowerCase().trim()});
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                dni = cursor.getString(0);
            }
            cursor.close();
        }
        db.close();
        return dni != null ? dni : "";
    }
}
