/*
This file is part of BeepMe.

BeepMe is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

BeepMe is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with BeepMe. If not, see <http://www.gnu.org/licenses/>.

Copyright 2012-2014 Michael Glanznig
http://beepme.yourexp.at
*/

package com.glanznig.beepme.data.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.glanznig.beepme.data.Vocabulary;

/**
 * Represents the table VOCABULARY (collections of predefined selection strings or tags that are
 * translatable).
 */
public class VocabularyTable extends StorageHandler {

    private static final String TAG = "VocabularyTable";

    private static final String TBL_NAME = "vocabulary";
    private static final String TBL_CREATE =
            "CREATE TABLE IF NOT EXISTS " + TBL_NAME + " (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT NOT NULL, " +
                    "project_id INTEGER NOT NULL, " +
                    "FOREIGN KEY(project_id) REFERENCES "+ ProjectTable.getTableName() +"(_id)" +
                    ")";

    public VocabularyTable(Context ctx) {
        super(ctx);
    }

    /**
     * Returns the table name.
     * @return table name
     */
    public static String getTableName() {
        return TBL_NAME;
    }

    /**
     * Creates the table.
     * @param db database object.
     */
    public static void createTable(SQLiteDatabase db) {
        db.execSQL(TBL_CREATE);
    }

    /**
     * Drops the table.
     * @param db database object.
     */
    public static void dropTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + TBL_NAME);
    }

    /**
     * Truncates (deletes the content of) the table.
     */
    public void truncate() {
        SQLiteDatabase db = getDb();
        dropTable(db);
        createTable(db);
    }

    /**
     * Populates content values for the set variables of the vocabulary.
     * @param vocabulary the vocabulary
     * @return populated content values
     */
    private ContentValues getContentValues(Vocabulary vocabulary) {
        ContentValues values = new ContentValues();

        if (vocabulary.getName() != null) {
            values.put("name", vocabulary.getName());
        }
        if (vocabulary.getProjectUid() != 0L) {
            values.put("project_id", vocabulary.getProjectUid());
        }

        return values;
    }

    /**
     * Adds a new vocabulary to the database
     * @param vocabulary values to add to the vocabulary table
     * @return new vocabulary object with set values and uid, or null if an error occurred
     */
    public Vocabulary addVocabulary(Vocabulary vocabulary) {
        Vocabulary newVocabulary = null;

        if (vocabulary != null) {
            SQLiteDatabase db = getDb();

            ContentValues values = getContentValues(vocabulary);

            Log.i(TAG, "inserted values=" + values);
            long vocabularyId = db.insert(getTableName(), null, values);
            db.close();

            // if no error occurred
            if (vocabularyId != -1) {
                newVocabulary = new Vocabulary(vocabularyId);
                vocabulary.copyTo(newVocabulary);
            }
        }

        return newVocabulary;
    }

    /**
     * Updates a vocabulary in the database
     * @param vocabulary values to update for this vocabulary
     * @return true on success or false if an error occurred
     */
    public boolean updateVocabulary(Vocabulary vocabulary) {
        int numRows = 0;
        if (vocabulary.getUid() != 0L) {
            SQLiteDatabase db = getDb();
            ContentValues values = getContentValues(vocabulary);

            numRows = db.update(getTableName(), values, "_id=?", new String[] { String.valueOf(vocabulary.getUid()) });
            db.close();
        }

        return numRows == 1;
    }
}
