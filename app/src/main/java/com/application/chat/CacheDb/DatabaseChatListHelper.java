package com.application.chat.CacheDb;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabaseChatListHelper extends SQLiteOpenHelper {
    private static final String DB_NAME="ListItemsDb.db";
    private static final int VERSION=2;
    private static final String TABLE_NAME="ChatTable";
    private static final String columnID="id";
    private static final String columnUserId="user_id";
    private static DatabaseChatListHelper instance;
    private static ExecutorService exe;
    private static SQLiteDatabase readDb,writeDb,writeDb1;

    public DatabaseChatListHelper(@Nullable Context context) {
        super(context, DB_NAME, null, VERSION);
        exe= Executors.newFixedThreadPool(3);
    }
    public static synchronized DatabaseChatListHelper getInstance(Context context){
        if(instance==null){
            instance=new DatabaseChatListHelper(context);
        }
        return instance;
    }
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        try{
            String sql="CREATE TABLE "+TABLE_NAME+" ("
                    +columnID+" INTEGER PRIMARY KEY AUTOINCREMENT, "
                    +columnUserId+" TEXT)";
            sqLiteDatabase.execSQL(sql);
        }catch (SQLException e){
            Log.e("SqliteCreateTableFailor",e.getMessage());
        }
    }
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        exe.execute(()->{
            try {
                String sql="DROP TABLE IF EXISTS "+TABLE_NAME;
                sqLiteDatabase.execSQL(sql);
            }
            catch (SQLException e){
                Log.e("SqliteDropTableFailor",e.getMessage());
            }
            finally {
                onCreate(sqLiteDatabase);
            }
        });
    }
    public String getColumnUserId(){
        return columnUserId;
    }

    public SQLiteDatabase getReadDb() {
        return readDb;
    }

    public void addToCache(String uid){
        writeDb = this.getWritableDatabase();
        exe.execute(()->{
            try {
                String sql="INSERT INTO "+TABLE_NAME+" ("+columnUserId+")VALUES(?)";
                writeDb.execSQL(sql,new String[]{uid});
            }catch (SQLException e){
                Log.e("SqliteWriteFailor",e.getMessage());
            }finally {
                writeDb.close();
            }
        });
    }
    public Cursor getAllUserId(){
        readDb=this.getReadableDatabase();
        try{
            String sql = "SELECT " + columnUserId + " FROM " + TABLE_NAME;
            return readDb.rawQuery(sql, null);
        } catch (SQLException e) {
            Log.e("SqliteReadFailor", e.getMessage());
        }
        return null;
    }
    public void delete(String id){
        writeDb1=this.getWritableDatabase();
        try{
            String sql="DELETE FROM "+TABLE_NAME+" WHERE "+columnUserId+"=?";
            writeDb1.execSQL(sql,new String[]{id});
        }catch (SQLException ex){
            Log.e("SqliteDeleteFailor",ex.getMessage());
        }
        finally {
            writeDb1.close();
        }
    }

}

