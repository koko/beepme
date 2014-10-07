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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import com.glanznig.beepme.data.Moment;
import com.glanznig.beepme.data.Value;
import com.glanznig.beepme.data.VocabularyItem;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * Represents the table MOMENT (basic information about moment with/out values).
 */
public class MomentTable extends StorageHandler {
	
	private static final String TAG = "MomentTable";
	
	private static final String TBL_NAME = "moment";
	private static final String TBL_CREATE =
			"CREATE TABLE IF NOT EXISTS " + TBL_NAME + " (" +
			"_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
			"timestamp INTEGER NOT NULL UNIQUE, " +
			"accepted INTEGER NOT NULL, " +
			"uptime_id INTEGER, " +
            "project_id INTEGER NOT NULL, " +
			"FOREIGN KEY (uptime_id) REFERENCES "  + UptimeTable.getTableName() + " (_id), " +
            "FOREIGN KEY (project_id) REFERENCES "  + ProjectTable.getTableName() + " (_id)" +
			")";

    private Context ctx;
	
	public MomentTable(Context ctx) {
		super(ctx);
        this.ctx = ctx.getApplicationContext();
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
     * Populates content values for the set variables of the moment.
     * @param moment the moment
     * @return populated content values
     */
    private ContentValues getContentValues(Moment moment) {
        ContentValues values = new ContentValues();

        if (moment.getProjectUid() != 0L) {
            values.put("project_id", moment.getProjectUid());
        }
        if (moment.getUptimeUid() != 0L) {
            values.put("uptime_id", moment.getUptimeUid());
        }
        if (moment.getTimestamp() != null) {
            values.put("timestamp", moment.getTimestamp().getTime());
        }
        if (moment.getAccepted()) {
            values.put("accepted", 1);
        }
        else {
            values.put("accepted", 0);
        }

        return values;
    }

    /**
     * Populates a moment object by reading values from a cursor
     * @param cursor cursor object
     * @return populated moment object
     */
    private Moment populateObject(Cursor cursor) {
        Moment moment = new Moment(cursor.getLong(0));
        moment.setTimestamp(new Date(cursor.getLong(1)));
        if (cursor.getInt(2) == 1) {
            moment.setAccepted(true);
        }
        else {
            moment.setAccepted(false);
        }
        if (!cursor.isNull(3)) {
            moment.setUptimeUid(cursor.getLong(3));
        }
        moment.setProjectUid(cursor.getLong(4));

        return moment;
    }

    /**
     * Gets all moment uids of the specified project
     * @param projectUid uid of project where moments belong to
     * @return list of moment uids or empty list if none
     */
    public List<Long> getMomentUids(long projectUid) {
        SQLiteDatabase db = getDb();
        List<Long> idList = new ArrayList<Long>();

        Cursor cursor = db.query(getTableName(), new String[] {"_id"}, "accepted=? AND project_id=?"
                , new String[] { "1", Long.valueOf(projectUid).toString() }, null, null, "timestamp DESC");

        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();

            do {
                idList.add(cursor.getLong(0));
            }
            while (cursor.moveToNext());
            cursor.close();
        }
        closeDb();

        return idList;
    }

    /**
     * Gets a specific moment by its uid (without values, only basedata)
     * @param uid uid of moment
     * @return moment, or null if not found
     */
    public Moment getMoment(long uid) {
        return getMoment(uid, false);
    }

    /**
     * Gets a specfic moment by its uid (with values)
     * @param uid uid of moment
     * @return moment, or null if not found
     */
    public Moment getMomentWithValues(long uid) {
        return getMoment(uid, true);
    }

    /**
     * Gets a specific moment by its uid (with or without values).
     * @param uid uid of moment
     * @param values if set to true a list of values is included with each moment, else only base data is returned
     * @return moment, or null if not found
     */
	private Moment getMoment(long uid, boolean values) {
		SQLiteDatabase db = getDb();
        ValueTable valueTable = new ValueTable(ctx);
		Moment moment = null;
		
		Cursor cursor = db.query(TBL_NAME, new String[] {"_id", "timestamp", "accepted", "uptime_id", "project_id"},
				"_id=?", new String[] { String.valueOf(uid) }, null, null, null);
		
		if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            moment = populateObject(cursor);

            if (values && moment.getAccepted()) {
                Iterator<Value> valueIterator = valueTable.getValues(moment.getUid()).iterator();

                while (valueIterator.hasNext()) {
                    Value value = valueIterator.next();
                    moment.setValue(value.getInputElementName(), value);
                }
            }

            cursor.close();
        }
		closeDb();
		
		return moment;
	}

    /**
     * Gets a list of accepted moments without values for the specified project.
     * @param projectUid uid of project where moments belong to
     * @return list of moments, or empty list if none
     */
    public List<Moment> getMoments(long projectUid) {
        return getMoments(projectUid, false, false, null);
    }

    /**
     * Gets a list of accepted moments with values for the specified project.
     * @param projectUid uid of project where moments belong to
     * @return list of moments, or empty list if none
     */
    public List<Moment> getMomentsWithValues(long projectUid) {
        return getMoments(projectUid, false, true, null);
    }

    /**
     * Gets a list of all moments (accepted and declined) without values for the specified project.
     * @param projectUid uid of project where moments belong to
     * @return list of moments, or empty list if none
     */
    public List<Moment> getAllMoments(long projectUid) {
        return getMoments(projectUid, true, false, null);
    }

    /**
     * Gets a list of all moments (accepted and declined) with values (accepted only) for the specified project.
     * @param projectUid uid of project where moments belong to
     * @return list of moments, or empty list if none
     */
    public List<Moment> getAllMomentsWithValues(long projectUid) {
        return getMoments(projectUid, true, true, null);
    }

    /**
     * Gets a list of moments (accepted only) without values for the specified day.
     * @param projectUid uid of project where moments belong to
     * @param day day on which the moments occured
     * @return list of moments, or empty list if none
     */
    public List<Moment> getMomentsOfDay(long projectUid, Calendar day) {
        return getMoments(projectUid, false, false, day);
    }

    /**
     * Gets a list of all moments (accepted and declined) without values for the specified day.
     * @param projectUid uid of project where moments belong to
     * @param day day on which the moments occured
     * @return list of moments, or empty list if none
     */
    public List<Moment> getAllMomentsOfDay(long projectUid, Calendar day) {
        return getMoments(projectUid, true, false, day);
    }

    /**
     * Gets a list of moments for the specified project according to parameters declined, values and day.
     * @param projectUid uid of project where moments belong to
     * @param declined if set to true all moments (accepted and declined are returned), else only accepted ones
     * @param values if set to true a list of values is included with each moment, else only base data is returned
     * @param day if not null this parameter provides a filter for the specified day
     * @return a list of moments according to parameters, or empty list if none
     */
    private List<Moment> getMoments(long projectUid, boolean declined, boolean values, Calendar day) {
        SQLiteDatabase db = getDb();
        ArrayList<Moment> momentList = new ArrayList<Moment>();
        ValueTable valueTable = new ValueTable(ctx);

        String where = "project_id=?";
        ArrayList<String> whereArgs = new ArrayList<String>();
        whereArgs.add(Long.valueOf(projectUid).toString());
        if (declined == false) {
            where += " AND accepted=?";
            whereArgs.add("1");
        }
        if (day != null && day.isSet(Calendar.YEAR) && day.isSet(Calendar.MONTH) && day.isSet(Calendar.DAY_OF_MONTH)) {
            long startOfDay = day.getTimeInMillis();
            day.roll(Calendar.DAY_OF_MONTH, true);
            long endOfDay = day.getTimeInMillis();
            day.roll(Calendar.DAY_OF_MONTH, false);

            where += " AND timestamp between ? and ?";
            whereArgs.add(Long.valueOf(startOfDay).toString());
            whereArgs.add(Long.valueOf(endOfDay).toString());
        }

        Cursor cursor = db.query(getTableName(), new String[] { "_id", "timestamp", "accepted",
                "uptime_id", "project_id" }, where, whereArgs.toArray(new String[]{}), null, null, "timestamp DESC");

        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();

            do {
                Moment m = populateObject(cursor);

                if (values) {
                    Iterator<Value> valueIterator = valueTable.getValues(m.getUid()).iterator();

                    while (valueIterator.hasNext()) {
                        Value value = valueIterator.next();
                        m.setValue(value.getInputElementName(), value);
                    }
                }

                momentList.add(m);
            }
            while (cursor.moveToNext());
            cursor.close();
        }
        closeDb();

        return momentList;
    }

    /**
     * Adds a new moment to the database
     * @param moment values to add to the moment table
     * @return new moment object with set values and uid, or null if an error occurred
     */
    public Moment addMoment(Moment moment) {
        Moment newMoment = null;

        if (moment != null) {
            SQLiteDatabase db = getDb();
            ContentValues values = getContentValues(moment);

            long momentId = db.insert(getTableName(), null, values);
            closeDb();
            // only if no error occurred
            if (momentId != -1) {
                newMoment = new Moment(momentId);
                moment.copyTo(newMoment);
            }
        }

        return newMoment;
    }

    /**
     * Deletes the given moment (and all its values) from the database.
     * @param uid uid of moment
     * @return true if successfully deleted, false otherwise
     */
    public boolean deleteMoment(long uid) {
        SQLiteDatabase db = getDb();

        ValueTable valueTable = new ValueTable(ctx);
        // first delete values of moment
        valueTable.deleteValues(uid);

        int rows = db.delete(TBL_NAME, "_id=?", new String[] { String.valueOf(uid) });
        closeDb();

        return rows == 1;
    }
}