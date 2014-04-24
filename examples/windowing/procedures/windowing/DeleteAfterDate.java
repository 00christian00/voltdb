/* This file is part of VoltDB.
 * Copyright (C) 2008-2014 VoltDB Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package windowing;

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;
import org.voltdb.VoltType;
import org.voltdb.types.TimestampType;

public class DeleteAfterDate extends VoltProcedure {

    final SQLStmt countMatchingRows = new SQLStmt(
            "SELECT COUNT(*) FROM timedata WHERE update_ts <= ?;");

    final SQLStmt getNthOldestTimestamp = new SQLStmt(
            "SELECT update_ts FROM timedata ORDER BY update_ts ASC OFFSET ? LIMIT 1;");

    final SQLStmt deleteOlderThanDate = new SQLStmt(
            "DELETE FROM timedata WHERE update_ts <= ? and update_ts > FROM_UNIXTIME(0);");

    final VoltTable retvalTemplate = new VoltTable(
            new VoltTable.ColumnInfo("deleted", VoltType.BIGINT),
            new VoltTable.ColumnInfo("not_deleted", VoltType.BIGINT));

    public VoltTable run(String partitionValue, TimestampType newestToDiscard, long maxRowsToDeletePerProc) {
        if (newestToDiscard == null) {
            throw new VoltAbortException("newestToDiscard shouldn't be null.");
            // It might be Long.MIN_VALUE as a TimestampType though.
        }
        if (maxRowsToDeletePerProc <= 0) {
            throw new VoltAbortException("maxRowsToDeletePerProc must be > 0.");
        }

        voltQueueSQL(countMatchingRows, EXPECT_SCALAR_LONG, newestToDiscard);
        VoltTable[] countResults = voltExecuteSQL();
        long agedOutCount = countResults[0].asScalarLong();

        if (agedOutCount > maxRowsToDeletePerProc) {
            voltQueueSQL(getNthOldestTimestamp, EXPECT_SCALAR, maxRowsToDeletePerProc);
            newestToDiscard = voltExecuteSQL()[0].fetchRow(0).getTimestampAsTimestamp(0);
        }

        voltQueueSQL(deleteOlderThanDate, EXPECT_SCALAR_LONG, newestToDiscard);
        long deletedCount = voltExecuteSQL(true)[0].asScalarLong();

        VoltTable retval = retvalTemplate.clone(20); // 20b to hold two longs in one row
        retval.addRow(deletedCount, agedOutCount - deletedCount);
        return retval;
    }
}
