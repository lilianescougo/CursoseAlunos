package com.liliane.cursosealunos;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class CursosOnlineDatabaseHelper extends SQLiteOpenHelper {

    private static CursosOnlineDatabaseHelper sInstance;

    //Tag usada para Log
    String TAG = "CursosOnlineDatabaseHelper";

    // Informações do banco de dados
    private static final String DATABASE_NAME = "CursosOnline";
    private static final int DATABASE_VERSION = 1;

    // Nome das tabelas
    private static final String TABLE_ALUNO = "Aluno";
    private static final String TABLE_CURSO = "Curso";

    // Nome das colunas da tabela Aluno
    private static final String KEY_ALUNO_ID = "alunoId";
    private static final String KEY_ALUNO_CURSO_ID_FK = "cursoId";
    private static final String KEY_ALUNO_NOME = "nomeAluno";
    private static final String KEY_ALUNO_EMAIL = "emailAluno";
    private static final String KEY_ALUNO_TELEFONE = "telefoneAluno";

    // Nome das colunas da tabela Curso
    private static final String KEY_CURSO_ID = "cursoId";
    private static final String KEY_CURSO_NOME = "nomeCurso";
    private static final String KEY_CURSO_QTD_HORAS = "qtdeHoras";

    public static synchronized CursosOnlineDatabaseHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new CursosOnlineDatabaseHelper(context.getApplicationContext());
        }
        return sInstance;
    }

    private CursosOnlineDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_ALUNO_TABLE = "CREATE TABLE " + TABLE_ALUNO +
                "(" +
                KEY_ALUNO_ID + " INTEGER PRIMARY KEY," +
                KEY_ALUNO_CURSO_ID_FK + " INTEGER REFERENCES " + TABLE_CURSO + "," +
                KEY_ALUNO_NOME + " TEXT" + "," +
                KEY_ALUNO_EMAIL + " TEXT" + "," +
                KEY_ALUNO_TELEFONE + " TEXT" +
                ")";

        String CREATE_CURSO_TABLE = "CREATE TABLE " + TABLE_CURSO +
                "(" +
                KEY_CURSO_ID + " INTEGER PRIMARY KEY," +
                KEY_CURSO_NOME + " TEXT," +
                KEY_CURSO_QTD_HORAS + " INTEGER" +
                ")";

        db.execSQL(CREATE_ALUNO_TABLE);
        db.execSQL(CREATE_CURSO_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion != newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_ALUNO);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_CURSO);
            onCreate(db);
        }
    }

    public void addAluno(Aluno aluno) {
        SQLiteDatabase db = getWritableDatabase();

        db.beginTransaction();
        try {
            long cursoId = addOrUpdateCurso(aluno.curso);

            ContentValues values = new ContentValues();
            values.put(KEY_ALUNO_CURSO_ID_FK, cursoId);
            values.put(KEY_ALUNO_NOME, aluno.nomeAluno);
            values.put(KEY_ALUNO_EMAIL, aluno.emailAluno);
            values.put(KEY_ALUNO_TELEFONE, aluno.telefoneAluno);

            db.insertOrThrow(TABLE_ALUNO, null, values);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.d(TAG, "Error while trying to add post to database");
        } finally {
            db.endTransaction();
        }
    }
    public long addOrUpdateCurso(Curso curso) {
        SQLiteDatabase db = getWritableDatabase();
        long cursoId = -1;

        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(KEY_CURSO_NOME, curso.nomeCurso);
            values.put(KEY_CURSO_QTD_HORAS, curso.qtdeHoras);

            // O nome de curso é único, então primeiro tenta atualizar o curso se já estiver no BD
            int rows = db.update(TABLE_CURSO, values, KEY_CURSO_NOME + "= ?", new String[]{curso.nomeCurso});
            if (rows == 1) {
                String usersSelectQuery = String.format("SELECT %s FROM %s WHERE %s = ?",
                        KEY_CURSO_ID, TABLE_CURSO, KEY_CURSO_NOME);
                Cursor cursor = db.rawQuery(usersSelectQuery, new String[]{String.valueOf(curso.nomeCurso)});
                try {
                    if (cursor.moveToFirst()) {
                        cursoId = cursor.getInt(0);
                        db.setTransactionSuccessful();
                    }
                } finally {
                    if (cursor != null && !cursor.isClosed()) {
                        cursor.close();
                    }
                }
            } else {
                // Curso com esse nome ainda não existe, então inserimos
                cursoId = db.insertOrThrow(TABLE_CURSO, null, values);
                db.setTransactionSuccessful();
            }
        } catch (Exception e) {
            Log.d(TAG, "Error while trying to add or update user");
        } finally {
            db.endTransaction();
        }
        return cursoId;
    }

    public List<Aluno> getAllAlunos() {
        List<Aluno> alunos = new ArrayList<>();
        String ALUNOS_SELECT_QUERY =
                String.format("SELECT * FROM %s LEFT OUTER JOIN %s ON %s.%s = %s.%s",
                        TABLE_ALUNO,
                        TABLE_CURSO,
                        TABLE_ALUNO, KEY_ALUNO_CURSO_ID_FK,
                        TABLE_CURSO, KEY_CURSO_ID);

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(ALUNOS_SELECT_QUERY, null);
        try {
            if (cursor.moveToFirst()) {
                do {
                    Curso newCurso = new Curso();
                    newCurso.nomeCurso = cursor.getString(cursor.getColumnIndex(KEY_CURSO_NOME));
                    newCurso.qtdeHoras = cursor.getInt(cursor.getColumnIndex(KEY_CURSO_QTD_HORAS));

                    Aluno newAluno = new Aluno();
                    newAluno.nomeAluno = cursor.getString(cursor.getColumnIndex(KEY_ALUNO_NOME));
                    newAluno.emailAluno = cursor.getString(cursor.getColumnIndex(KEY_ALUNO_EMAIL));
                    newAluno.telefoneAluno = cursor.getString(cursor.getColumnIndex(KEY_ALUNO_TELEFONE));
                    newAluno.curso = newCurso;
                    alunos.add(newAluno);
                } while(cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.d(TAG, "Erro ao pegar alunos do banco de dados");
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        return alunos;
    }


}
