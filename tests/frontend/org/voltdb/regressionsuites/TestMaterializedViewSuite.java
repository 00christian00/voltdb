/* This file is part of VoltDB.
 * Copyright (C) 2008-2016 VoltDB Inc.
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

package org.voltdb.regressionsuites;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.voltdb.BackendTarget;
import org.voltdb.VoltTable;
import org.voltdb.VoltTableRow;
import org.voltdb.VoltType;
import org.voltdb.client.Client;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.NoConnectionsException;
import org.voltdb.client.ProcCallException;
import org.voltdb.compiler.VoltProjectBuilder;
import org.voltdb_testprocs.regressionsuites.matviewprocs.AddPerson;
import org.voltdb_testprocs.regressionsuites.matviewprocs.AddThing;
import org.voltdb_testprocs.regressionsuites.matviewprocs.AggAges;
import org.voltdb_testprocs.regressionsuites.matviewprocs.AggThings;
import org.voltdb_testprocs.regressionsuites.matviewprocs.DeletePerson;
import org.voltdb_testprocs.regressionsuites.matviewprocs.Eng798Insert;
import org.voltdb_testprocs.regressionsuites.matviewprocs.OverflowTest;
import org.voltdb_testprocs.regressionsuites.matviewprocs.SelectAllPeople;
import org.voltdb_testprocs.regressionsuites.matviewprocs.TruncateMatViewDataMP;
import org.voltdb_testprocs.regressionsuites.matviewprocs.UpdatePerson;

import com.google_voltpatches.common.collect.Lists;

import junit.framework.Test;


public class TestMaterializedViewSuite extends RegressionSuite {

    // Constants to control whether to abort a procedure invocation with explicit sabotage
    // or to allow it to run normally.
    private static final int SABOTAGE = 2;
    private static final int NORMALLY = 0;

    // procedures used by these tests
    static final Class<?>[] PROCEDURES = {
 TruncateMatViewDataMP.class
    };

    public TestMaterializedViewSuite(String name) {
        super(name);
    }

    private void truncateBeforeTest(Client client) {
        // TODO Auto-generated method stub
        VoltTable[] results = null;
        try {
            results = client.callProcedure("TruncateMatViewDataMP").getResults();
        } catch (NoConnectionsException e) {
            e.printStackTrace();
            fail("Unexpected:" + e);
        } catch (IOException e) {
            e.printStackTrace();
            fail("Unexpected:" + e);
        } catch (ProcCallException e) {
            e.printStackTrace();
            fail("Unexpected:" + e);
        }
    }

    private void assertAggNoGroupBy(Client client, String tableName, String... values) throws IOException, ProcCallException
    {
        assertTrue(values != null);
        VoltTable[] results = client.callProcedure("@AdHoc", "SELECT * FROM " + tableName).getResults();

        assertTrue(results != null);
        assertEquals(1, results.length);
        VoltTable t = results[0];
        assertTrue(values.length <= t.getColumnCount());
        assertEquals(1, t.getRowCount());
        t.advanceRow();
        for (int i=0; i<values.length; ++i) {
            // if it's integer
            if (t.getColumnType(i) == VoltType.TINYINT ||
                t.getColumnType(i) == VoltType.SMALLINT ||
                t.getColumnType(i) == VoltType.INTEGER ||
                t.getColumnType(i) == VoltType.BIGINT) {
                long value = t.getLong(i);
                if (values[i].equals("null")) {
                    assertTrue(t.wasNull());
                }
                else {
                    assertEquals(Long.parseLong(values[i]), value);
                }
            }
            else if (t.getColumnType(i) == VoltType.FLOAT) {
                double value = t.getDouble(i);
                if (values[i].equals("null")) {
                    assertTrue(t.wasNull());
                }
                else {
                    assertEquals(Double.parseDouble(values[i]), value);
                }
            }
        }
    }

    private void subtestENG7872SinglePartition() throws IOException, ProcCallException
    {
        Client client = getClient();
        truncateBeforeTest(client);
        VoltTable[] results = null;

        assertAggNoGroupBy(client, "MATPEOPLE_COUNT", "0");
        assertAggNoGroupBy(client, "MATPEOPLE_CONDITIONAL_COUNT", "0");
        assertAggNoGroupBy(client, "MATPEOPLE_CONDITIONAL_COUNT_SUM", "0", "null");
        assertAggNoGroupBy(client, "MATPEOPLE_CONDITIONAL_COUNT_MIN_MAX", "0", "null", "null");

        results = client.callProcedure("AddPerson", 1, 1L, 31L, 1000.0, 3, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("AddPerson", 1, 2L, 31L, 900.0, 5, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("AddPerson", 1, 3L, 31L, 900.0, 1, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("AddPerson", 1, 4L, 31L, 2500.0, 5, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("AddPerson", 1, 5L, 31L, null, null, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());

        assertAggNoGroupBy(client, "MATPEOPLE_COUNT", "5");
        assertAggNoGroupBy(client, "MATPEOPLE_CONDITIONAL_COUNT", "2");
        assertAggNoGroupBy(client, "MATPEOPLE_CONDITIONAL_COUNT_SUM", "2", "4");
        assertAggNoGroupBy(client, "MATPEOPLE_CONDITIONAL_COUNT_MIN_MAX", "3", "900.0", "5");

        results = client.callProcedure("DeletePerson", 1, 2L, NORMALLY).getResults();

        assertAggNoGroupBy(client, "MATPEOPLE_COUNT", "4");
        assertAggNoGroupBy(client, "MATPEOPLE_CONDITIONAL_COUNT", "1");
        assertAggNoGroupBy(client, "MATPEOPLE_CONDITIONAL_COUNT_SUM", "2", "4");
        assertAggNoGroupBy(client, "MATPEOPLE_CONDITIONAL_COUNT_MIN_MAX", "2", "1000.0", "5");

        results = client.callProcedure("UpdatePerson", 1, 3L, 31L, 200, 9).getResults();

        assertAggNoGroupBy(client, "MATPEOPLE_COUNT", "4");
        assertAggNoGroupBy(client, "MATPEOPLE_CONDITIONAL_COUNT", "2");
        assertAggNoGroupBy(client, "MATPEOPLE_CONDITIONAL_COUNT_SUM", "1", "3");
        assertAggNoGroupBy(client, "MATPEOPLE_CONDITIONAL_COUNT_MIN_MAX", "3", "200.0", "9");
    }

    private void verifyENG6511(Client client) throws IOException, ProcCallException
    {
        VoltTable vresult = null;
        VoltTable tresult = null;
        String prefix = "Assertion failed comparing the view content and the AdHoc query result ";

        vresult = client.callProcedure("@AdHoc", "SELECT * FROM VENG6511 ORDER BY d1, d2;").getResults()[0];
        tresult = client.callProcedure("@AdHoc", "SELECT d1, d2, COUNT(*), MIN(v2) AS vmin, MAX(v2) AS vmax FROM ENG6511 GROUP BY d1, d2 ORDER BY 1, 2;").getResults()[0];
        assertTablesAreEqual(prefix + "VENG6511: ", tresult, vresult);

        vresult = client.callProcedure("@AdHoc", "SELECT * FROM VENG6511expL ORDER BY d1, d2;").getResults()[0];
        tresult = client.callProcedure("@AdHoc", "SELECT d1+1, d2*2, COUNT(*), MIN(v2) AS vmin, MAX(v2) AS vmax FROM ENG6511 GROUP BY d1+1, d2*2 ORDER BY 1, 2;").getResults()[0];
        assertTablesAreEqual(prefix + "VENG6511expL: ", tresult, vresult);

        vresult = client.callProcedure("@AdHoc", "SELECT * FROM VENG6511expR ORDER BY d1, d2;").getResults()[0];
        tresult = client.callProcedure("@AdHoc", "SELECT d1, d2, COUNT(*), MIN(abs(v1)) AS vmin, MAX(abs(v1)) AS vmax FROM ENG6511 GROUP BY d1, d2 ORDER BY 1, 2;").getResults()[0];
        assertTablesAreEqual(prefix + "VENG6511expR: ", tresult, vresult);

        vresult = client.callProcedure("@AdHoc", "SELECT * FROM VENG6511expLR ORDER BY d1, d2;").getResults()[0];
        tresult = client.callProcedure("@AdHoc", "SELECT d1+1, d2*2, COUNT(*), MIN(v2-1) AS vmin, MAX(v2-1) AS vmax FROM ENG6511 GROUP BY d1+1, d2*2 ORDER BY 1, 2;").getResults()[0];
        assertTablesAreEqual(prefix + "VENG6511expLR: ", tresult, vresult);

        vresult = client.callProcedure("@AdHoc", "SELECT * FROM VENG6511C ORDER BY d1, d2;").getResults()[0];
        tresult = client.callProcedure("@AdHoc", "SELECT d1, d2, COUNT(*), MIN(v1) AS vmin, MAX(v1) AS vmax FROM ENG6511 WHERE v1 > 4 GROUP BY d1, d2 ORDER BY 1, 2;").getResults()[0];
        assertTablesAreEqual(prefix + "VENG6511C: ", tresult, vresult);

        vresult = client.callProcedure("@AdHoc", "SELECT * FROM VENG6511TwoIndexes ORDER BY d1, d2;").getResults()[0];
        tresult = client.callProcedure("@AdHoc", "SELECT d1, d2, COUNT(*), MIN(abs(v1)) AS vmin, MAX(v2) AS vmax FROM ENG6511 WHERE v1 > 4 GROUP BY d1, d2 ORDER BY 1, 2;").getResults()[0];
        assertTablesAreEqual(prefix + "VENG6511TwoIndexes: ", tresult, vresult);

        vresult = client.callProcedure("@AdHoc", "SELECT * FROM VENG6511NoGroup ORDER BY 1, 2, 3;").getResults()[0];
        tresult = client.callProcedure("@AdHoc", "SELECT COUNT(*), MIN(v1) AS vmin, MAX(v2) AS vmax FROM ENG6511 ORDER BY 1, 2, 3;").getResults()[0];
        assertTablesAreEqual(prefix + "VENG6511NoGroup: ", tresult, vresult);
    }

    private void runAndVerifyENG6511(Client client, String query) throws IOException, ProcCallException
    {
        VoltTable[] results = null;
        results = client.callProcedure("@AdHoc", query).getResults();
        assertEquals(1, results.length);
        verifyENG6511(client);
    }

    // Test the correctness of min/max when choosing an index on both group-by columns and aggregation column/exprs.
    private void subtestENG6511(boolean singlePartition) throws IOException, ProcCallException
    {
        Client client = getClient();
        truncateBeforeTest(client);
        int pid = singlePartition ? 1 : 2;

        insertRow(client, "ENG6511", 1, 1, 3, 70, 46);
        insertRow(client, "ENG6511", 1, 1, 3, 70, 46);
        insertRow(client, "ENG6511", 1, 1, 3, 12, 66);
        insertRow(client, "ENG6511", pid, 1, 3, 9, 70);
        insertRow(client, "ENG6511", pid, 1, 3, 256, 412);
        insertRow(client, "ENG6511", pid, 1, 3, 70, -46);

        insertRow(client, "ENG6511", 1, 1, 4, 17, 218);
        insertRow(client, "ENG6511", 1, 1, 4, 25, 28);
        insertRow(client, "ENG6511", pid, 1, 4, 48, 65);
        insertRow(client, "ENG6511", pid, 1, 4, -48, 70);

        insertRow(client, "ENG6511", 1, 2, 5, -71, 75);
        insertRow(client, "ENG6511", 1, 2, 5, -4, 5);
        insertRow(client, "ENG6511", pid, 2, 5, 64, 16);
        insertRow(client, "ENG6511", pid, 2, 5, null, 91);

        insertRow(client, "ENG6511", 1, 2, 6, -9, 85);
        insertRow(client, "ENG6511", 1, 2, 6, 38, 43);
        insertRow(client, "ENG6511", pid, 2, 6, 21, -51);
        insertRow(client, "ENG6511", pid, 2, 6, null, 17);
        verifyENG6511(client);

        runAndVerifyENG6511(client, "UPDATE ENG6511 SET v2=120 WHERE v2=17;");
        runAndVerifyENG6511(client, "DELETE FROM ENG6511 WHERE v2=-51;");
        runAndVerifyENG6511(client, "DELETE FROM ENG6511 WHERE v1=-71;");
        runAndVerifyENG6511(client, "DELETE FROM ENG6511 WHERE v1=48;");
        runAndVerifyENG6511(client, "UPDATE ENG6511 SET v1=NULL WHERE v1=256;");
        runAndVerifyENG6511(client, "DELETE FROM ENG6511 WHERE pid=1 AND v1=70 ORDER BY pid, d1, d2, v1, v2 LIMIT 2;");
        runAndVerifyENG6511(client, "DELETE FROM ENG6511 WHERE d1=2 AND d2=5 AND v1 IS NOT NULL;");
    }

    public void UNtestSinglePartition() throws IOException, ProcCallException
    {
        subtestInsertSinglePartition();
        subtestDeleteSinglePartition();
        subtestUpdateSinglePartition();
        subtestSinglePartitionWithPredicates();
        subtestMinMaxSinglePartition();
        subtestMinMaxSinglePartitionWithPredicate();
        subtestIndexMinMaxSinglePartition();
        subtestIndexMinMaxSinglePartitionWithPredicate();
        subtestNullMinMaxSinglePartition();
        subtestENG7872SinglePartition();
        subtestENG6511(false);
    }


    private void subtestInsertSinglePartition() throws IOException, ProcCallException
    {
        Client client = getClient();
        truncateBeforeTest(client);
        VoltTable[] results = null;

        results = client.callProcedure("AggAges", 1).getResults();
        assertEquals(1, results.length);
        assertEquals(0, results[0].getRowCount());

        results = client.callProcedure("AddPerson", 1, 1L, 31L, 27500.20, 7, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("AddPerson", 1, 2L, 31L, 28920.99, 3, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("AddPerson", 1, 3L, 32L, 63250.01, -1, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());

        results = client.callProcedure("AggAges", 1).getResults();
        assertEquals(1, results.length);
        assertEquals(2, results[0].getRowCount());
        assert(results != null);

        // HSQL backend does not support multi-statement transactionality.
        if ( ! isHSQL()) {
            // Make a doomed attempt to insert that should have no effect.
            try {
                results = client.callProcedure("AddPerson", 1, 4L, 44L, 44444.44, 4, SABOTAGE).getResults();
                fail("intentional ProcCallException failed");
            } catch (ProcCallException pce) {
                // Expected the throw.
            }
        }
        results = client.callProcedure("AggAges", 1).getResults();
        assertEquals(1, results.length);
        assertEquals(2, results[0].getRowCount());

}

    private void subtestDeleteSinglePartition() throws IOException, ProcCallException
    {
        Client client = getClient();
        truncateBeforeTest(client);
        VoltTable[] results = null;

        results = client.callProcedure("AggAges", 1).getResults();
        assertEquals(1, results.length);
        assertEquals(0, results[0].getRowCount());

        results = client.callProcedure("AddPerson", 1, 1L, 31L, 27500.20, 7, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("AddPerson", 1, 2L, 31L, 28920.99, 3, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());

        results = client.callProcedure("AggAges", 1).getResults();
        assertEquals(1, results.length);
        results[0].advanceRow();
        assertEquals(31L, results[0].getLong(0));
        assertEquals(2L, results[0].getLong(2));
        assertEquals(27500.20 + 28920.99, results[0].getDouble("SALARIES"), 0.001);

        // HSQL backend does not support multi-statement transactionality.
        if ( ! isHSQL()) {
            // Make a doomed attempt to delete that should have no effect.
            try {
                results = client.callProcedure("DeletePerson", 1, 1L, SABOTAGE).getResults();
                fail("intentional ProcCallException failed");
            } catch (ProcCallException pce) {
                // Expected the throw.
            }
        }
        results = client.callProcedure("AggAges", 1).getResults();
        assertEquals(1, results.length);
        results[0].advanceRow();
        assertEquals(31L, results[0].getLong(0));
        assertEquals(2L, results[0].getLong(2));
        assertEquals(27500.20 + 28920.99, results[0].getDouble("SALARIES"), 0.001);

        results = client.callProcedure("DeletePerson", 1, 1L, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());

        results = client.callProcedure("AggAges", 1).getResults();
        assertEquals(1, results.length);
        assertEquals(1, results[0].getRowCount());
        while (results[0].advanceRow()) {
            assertEquals(31L, results[0].getLong(0));
            assertEquals(1L, results[0].getLong(2));
            assertEquals(28920.99, results[0].getDouble(3), 0.01);
            assertEquals(3L, results[0].getLong(4));
        }
        assert(results != null);

        results = client.callProcedure("DeletePerson", 1, 2L, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());

        results = client.callProcedure("AggAges", 1).getResults();
        assertEquals(1, results.length);
        assertEquals(0, results[0].getRowCount());
        assert(results != null);
    }

    private void subtestUpdateSinglePartition() throws IOException, ProcCallException
    {
        Client client = getClient();
        truncateBeforeTest(client);
        VoltTable[] results = null;

        results = client.callProcedure("AggAges", 1).getResults();
        assertEquals(1, results.length);
        assertEquals(0, results[0].getRowCount());
        assert(results != null);

        results = client.callProcedure("AddPerson", 1, 1L, 31L, 27500.20, 7, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("AddPerson", 1, 2L, 31L, 28920.99, 3, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("AddPerson", 1, 3L, 33L, 28920.99, 3, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("UpdatePerson", 1, 2L, 31L, 15000.00, 3).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("UpdatePerson", 1, 1L, 31L, 15000.00, 5).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());

        results = client.callProcedure("AggAges", 1).getResults();
        assert(results != null);
        assertEquals(1, results.length);
        assertEquals(2, results[0].getRowCount());
        System.out.println(results[0].toString());
        VoltTableRow r1 = results[0].fetchRow(0);
        VoltTableRow r2 = results[0].fetchRow(1);
        assertEquals(31L, r1.getLong(0));
        assertEquals(2L, r1.getLong(2));
        assertTrue(Math.abs(r1.getDouble(3) - 30000.0) < .01);
        assertEquals(8L, r1.getLong(4));

        assertEquals(33L, r2.getLong(0));
        assertEquals(1L, r2.getLong(2));
        assertTrue(Math.abs(r2.getDouble(3) - 28920.99) < .01);
        assertEquals(3L, r2.getLong(4));
    }

    private void subtestSinglePartitionWithPredicates() throws IOException, ProcCallException
    {
        Client client = getClient();
        truncateBeforeTest(client);
        VoltTable[] results = null;

        results = client.callProcedure("AggAges", 1).getResults();
        assertEquals(1, results.length);
        assertEquals(0, results[0].getRowCount());
        assert(results != null);

        // expecting the 2yr old won't make it
        results = client.callProcedure("AddPerson", 1, 1L, 31L, 1000.0, 7, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("AddPerson", 1, 2L, 2L, 2000.0, 3, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());

        results = client.callProcedure("AggAges", 1).getResults();
        assertEquals(1, results.length);
        assertEquals(1, results[0].getRowCount());
        assert(results != null);

        results = client.callProcedure("UpdatePerson", 1, 1L, 3L, 1000.0, 6).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());

        results = client.callProcedure("AggAges", 1).getResults();
        assertEquals(1, results.length);
        assertEquals(0, results[0].getRowCount());
        assert(results != null);

        results = client.callProcedure("UpdatePerson", 1, 2L, 50L, 4000.0, 4).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());

        results = client.callProcedure("AggAges", 1).getResults();
        assertEquals(1, results.length);
        assertEquals(1, results[0].getRowCount());
        assert(results != null);

        results = client.callProcedure("DeletePerson", 1, 1L, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());

        results = client.callProcedure("AggAges", 1).getResults();
        assertEquals(1, results.length);
        assertEquals(1, results[0].getRowCount());
        assert(results != null);
    }

    private void subtestMinMaxSinglePartition() throws IOException, ProcCallException {
        Client client = getClient();
        truncateBeforeTest(client);
        VoltTable[] results = null;
        VoltTable t;

        results = client.callProcedure("@AdHoc", "SELECT * FROM MATPEOPLE2").getResults();
        assert(results != null);
        assertEquals(1, results.length);
        assertEquals(0, results[0].getRowCount());

        results = client.callProcedure("AddPerson", 1, 1L, 31L, 1000.0, 3, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("AddPerson", 1, 2L, 31L, 900.0, 5, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("AddPerson", 1, 3L, 31L, 900.0, 1, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("AddPerson", 1, 4L, 31L, 2500.0, 5, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());

        results = client.callProcedure("@AdHoc", "SELECT * FROM MATPEOPLE2").getResults();
        assert(results != null);
        assertEquals(1, results.length);
        t = results[0];
        assertEquals(1, t.getRowCount());
        System.out.println(t.toString());
        t.advanceRow();
        assertEquals(4, t.getLong(2));
        assertEquals(900, (int)(t.getDouble(3)));
        assertEquals(5, t.getLong(4));

        results = client.callProcedure("DeletePerson", 1, 2L, NORMALLY).getResults();

        results = client.callProcedure("@AdHoc", "SELECT * FROM MATPEOPLE2").getResults();
        assert(results != null);
        assertEquals(1, results.length);
        t = results[0];
        assertEquals(1, t.getRowCount());
        System.out.println(t.toString());
        t.advanceRow();
        assertEquals(3, t.getLong(2));
        assertEquals(900, (int)(t.getDouble(3)));
        assertEquals(5, t.getLong(4));

        results = client.callProcedure("UpdatePerson", 1, 3L, 31L, 200, 9).getResults();

        results = client.callProcedure("@AdHoc", "SELECT * FROM MATPEOPLE2").getResults();
        assert(results != null);
        assertEquals(1, results.length);
        t = results[0];
        assertEquals(1, t.getRowCount());
        System.out.println(t.toString());
        t.advanceRow();
        assertEquals(3, t.getLong(2));
        assertEquals(200, (int)(t.getDouble(3)));
        assertEquals(9, t.getLong(4));

        results = client.callProcedure("UpdatePerson", 1, 4L, 31L, 0, 10).getResults();

        results = client.callProcedure("@AdHoc", "SELECT * FROM MATPEOPLE2").getResults();
        assert(results != null);
        assertEquals(1, results.length);
        t = results[0];
        assertEquals(1, t.getRowCount());
        System.out.println(t.toString());
        t.advanceRow();
        assertEquals(3, t.getLong(2));
        assertEquals(0, (int)(t.getDouble(3)));
        assertEquals(10, t.getLong(4));

        results = client.callProcedure("DeletePerson", 1, 1L, NORMALLY).getResults();

        results = client.callProcedure("@AdHoc", "SELECT * FROM MATPEOPLE2").getResults();
        assert(results != null);
        assertEquals(1, results.length);
        t = results[0];
        assertEquals(1, t.getRowCount());
        System.out.println(t.toString());
        t.advanceRow();
        assertEquals(2, t.getLong(2));
        assertEquals(0, (int)(t.getDouble(3)));
        assertEquals(10, t.getLong(4));

    }

    private void subtestMinMaxSinglePartitionWithPredicate() throws IOException, ProcCallException {
        Client client = getClient();
        truncateBeforeTest(client);
        VoltTable[] results = null;
        VoltTable t;

        results = client.callProcedure("@AdHoc", "SELECT * FROM MATPEOPLE3").getResults();
        assert(results != null);
        assertEquals(1, results.length);
        assertEquals(0, results[0].getRowCount());

        results = client.callProcedure("AddPerson", 1, 1L, 31L, 1000.0, 3, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("AddPerson", 1, 2L, 31L, 900.0, 5, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("AddPerson", 1, 3L, 31L, 900.0, 1, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("AddPerson", 1, 4L, 31L, 2500.0, 5, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());

        results = client.callProcedure("@AdHoc", "SELECT * FROM MATPEOPLE3").getResults();
        assert(results != null);
        assertEquals(1, results.length);
        t = results[0];
        assertEquals(1, t.getRowCount());
        System.out.println(t.toString());
        t.advanceRow();
        assertEquals(2, t.getLong(2));
        assertEquals(5, t.getLong(3));

        results = client.callProcedure("DeletePerson", 1, 4L, NORMALLY).getResults();

        results = client.callProcedure("@AdHoc", "SELECT * FROM MATPEOPLE3").getResults();
        assert(results != null);
        assertEquals(1, results.length);
        t = results[0];
        assertEquals(1, t.getRowCount());
        System.out.println(t.toString());
        t.advanceRow();
        assertEquals(1, t.getLong(2));
        assertEquals(3, t.getLong(3));

        results = client.callProcedure("UpdatePerson", 1, 1L, 31L, 2000, 9).getResults();

        results = client.callProcedure("@AdHoc", "SELECT * FROM MATPEOPLE3").getResults();
        assert(results != null);
        assertEquals(1, results.length);
        t = results[0];
        assertEquals(1, t.getRowCount());
        System.out.println(t.toString());
        t.advanceRow();
        assertEquals(1, t.getLong(2));
        assertEquals(9, t.getLong(3));

    }

    private void subtestIndexMinMaxSinglePartition() throws IOException, ProcCallException
    {
        Client client = getClient();
        truncateBeforeTest(client);
        VoltTable[] results = null;

        results = client.callProcedure("@AdHoc", "SELECT * FROM DEPT_AGE_MATVIEW;").getResults();
        assert(results != null);
        assertEquals(1, results.length);
        assertEquals(0, results[0].getRowCount());

        insertRow(client, "DEPT_PEOPLE", 1, 1L, 31L, 1000.00, 3);
        insertRow(client, "DEPT_PEOPLE", 2, 1L, 31L, 900.00, 5);
        insertRow(client, "DEPT_PEOPLE", 3, 1L, 31L, 900.00, 1);
        insertRow(client, "DEPT_PEOPLE", 4, 1L, 31L, 2500.00, 5);

        VoltTable t;
        results = client.callProcedure("@AdHoc", "SELECT * FROM DEPT_AGE_MATVIEW").getResults();
        assert(results != null);
        assertEquals(1, results.length);
        t = results[0];
        assertEquals(1, t.getRowCount());
        System.out.println(t.toString());
        t.advanceRow();
        assertEquals(4, t.getLong(2));
        assertEquals(900, (int)(t.getDouble(3)));
        assertEquals(5, t.getLong(4));

        results = client.callProcedure("DEPT_PEOPLE.delete", 2L).getResults();

        results = client.callProcedure("@AdHoc", "SELECT * FROM DEPT_AGE_MATVIEW").getResults();
        assert(results != null);
        assertEquals(1, results.length);
        t = results[0];
        assertEquals(1, t.getRowCount());
        System.out.println(t.toString());
        t.advanceRow();
        assertEquals(3, t.getLong(2));
        assertEquals(900, (int)(t.getDouble(3)));
        assertEquals(5, t.getLong(4));

        results = client.callProcedure("DEPT_PEOPLE.update", 3L, 1, 31L, 200, 9, 3L).getResults();

        results = client.callProcedure("@AdHoc", "SELECT * FROM DEPT_AGE_MATVIEW").getResults();
        assert(results != null);
        assertEquals(1, results.length);
        t = results[0];
        assertEquals(1, t.getRowCount());
        System.out.println(t.toString());
        t.advanceRow();
        assertEquals(3, t.getLong(2));
        assertEquals(200, (int)(t.getDouble(3)));
        assertEquals(9, t.getLong(4));

        results = client.callProcedure("DEPT_PEOPLE.update", 4L, 1, 31L, 0, 10, 4L).getResults();

        results = client.callProcedure("@AdHoc", "SELECT * FROM DEPT_AGE_MATVIEW").getResults();
        assert(results != null);
        assertEquals(1, results.length);
        t = results[0];
        assertEquals(1, t.getRowCount());
        System.out.println(t.toString());
        t.advanceRow();
        assertEquals(3, t.getLong(2));
        assertEquals(0, (int)(t.getDouble(3)));
        assertEquals(10, t.getLong(4));

        results = client.callProcedure("DEPT_PEOPLE.delete", 1L).getResults();

        results = client.callProcedure("@AdHoc", "SELECT * FROM DEPT_AGE_MATVIEW").getResults();
        assert(results != null);
        assertEquals(1, results.length);
        t = results[0];
        assertEquals(1, t.getRowCount());
        System.out.println(t.toString());
        t.advanceRow();
        assertEquals(2, t.getLong(2));
        assertEquals(0, (int)(t.getDouble(3)));
        assertEquals(10, t.getLong(4));
    }

    private void subtestIndexMinMaxSinglePartitionWithPredicate() throws IOException, ProcCallException
    {
        Client client = getClient();
        truncateBeforeTest(client);
        VoltTable[] results = null;

        results = client.callProcedure("@AdHoc", "SELECT * FROM DEPT_AGE_FILTER_MATVIEW;").getResults();
        assert(results != null);
        assertEquals(1, results.length);
        assertEquals(0, results[0].getRowCount());

        insertRow(client, "DEPT_PEOPLE", 1, 1L, 31L, 1000.00, 3);
        insertRow(client, "DEPT_PEOPLE", 2, 1L, 31L, 900.00, 5);
        insertRow(client, "DEPT_PEOPLE", 3, 1L, 31L, 900.00, 1);
        insertRow(client, "DEPT_PEOPLE", 4, 1L, 31L, 2500.00, 5);

        VoltTable t;
        results = client.callProcedure("@AdHoc", "SELECT * FROM DEPT_AGE_FILTER_MATVIEW").getResults();
        assert(results != null);
        assertEquals(1, results.length);
        t = results[0];
        assertEquals(1, t.getRowCount());
        System.out.println(t.toString());
        t.advanceRow();
        assertEquals(2, t.getLong(2));
        assertEquals(5, t.getLong(3));

        results = client.callProcedure("DEPT_PEOPLE.delete", 2L).getResults();

        results = client.callProcedure("@AdHoc", "SELECT * FROM DEPT_AGE_FILTER_MATVIEW").getResults();
        assert(results != null);
        assertEquals(1, results.length);
        t = results[0];
        assertEquals(1, t.getRowCount());
        System.out.println(t.toString());
        t.advanceRow();
        assertEquals(2, t.getLong(2));
        assertEquals(5, t.getLong(3));

        results = client.callProcedure("DEPT_PEOPLE.update", 4L, 1, 31L, 200, 9, 4L).getResults();

        results = client.callProcedure("@AdHoc", "SELECT * FROM DEPT_PEOPLE;").getResults();
        System.out.println(results[0].toString());

        results = client.callProcedure("@AdHoc", "SELECT * FROM DEPT_AGE_FILTER_MATVIEW").getResults();
        assert(results != null);
        assertEquals(1, results.length);
        t = results[0];
        assertEquals(1, t.getRowCount());
        System.out.println(t.toString());
        t.advanceRow();
        assertEquals(1, t.getLong(2));
        assertEquals(3, t.getLong(3));

        results = client.callProcedure("DEPT_PEOPLE.update", 4L, 1, 31L, 2000, 9, 4L).getResults();

        results = client.callProcedure("@AdHoc", "SELECT * FROM DEPT_AGE_FILTER_MATVIEW").getResults();
        assert(results != null);
        assertEquals(1, results.length);
        t = results[0];
        assertEquals(1, t.getRowCount());
        System.out.println(t.toString());
        t.advanceRow();
        assertEquals(2, t.getLong(2));
        assertEquals(9, t.getLong(3));

    }

    private void subtestNullMinMaxSinglePartition() throws IOException, ProcCallException
    {
        Client client = getClient();
        truncateBeforeTest(client);
        VoltTable[] results = null;
        VoltTable t;

        results = client.callProcedure("@AdHoc", "SELECT * FROM MATPEOPLE2").getResults();
        assert(results != null);
        assertEquals(1, results.length);
        assertEquals(0, results[0].getRowCount());

        results = client.callProcedure("AddPerson", 1, 1L, 31L, 1000.0, 3, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("AddPerson", 1, 2L, 31L, 900.0, 5, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("AddPerson", 1, 3L, 31L, 900.0, 1, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("AddPerson", 1, 4L, 31L, 2500.0, 5, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("AddPerson", 1, 5L, 31L, null, null, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());

        results = client.callProcedure("@AdHoc", "SELECT * FROM MATPEOPLE2").getResults();
        assert(results != null);
        assertEquals(1, results.length);
        t = results[0];
        assertEquals(1, t.getRowCount());
        System.out.println(t.toString());
        t.advanceRow();
        assertEquals(5, t.getLong(2));
        assertEquals(900, (int)(t.getDouble(3)));
        assertEquals(5, t.getLong(4));

        results = client.callProcedure("DeletePerson", 1, 2L, NORMALLY).getResults();

        results = client.callProcedure("@AdHoc", "SELECT * FROM MATPEOPLE2").getResults();
        assert(results != null);
        assertEquals(1, results.length);
        t = results[0];
        assertEquals(1, t.getRowCount());
        System.out.println(t.toString());
        t.advanceRow();
        assertEquals(4, t.getLong(2));
        assertEquals(900, (int)(t.getDouble(3)));
        assertEquals(5, t.getLong(4));

        results = client.callProcedure("UpdatePerson", 1, 3L, 31L, 200, 9).getResults();

        results = client.callProcedure("@AdHoc", "SELECT * FROM MATPEOPLE2").getResults();
        assert(results != null);
        assertEquals(1, results.length);
        t = results[0];
        assertEquals(1, t.getRowCount());
        System.out.println(t.toString());
        t.advanceRow();
        assertEquals(4, t.getLong(2));
        assertEquals(200, (int)(t.getDouble(3)));
        assertEquals(9, t.getLong(4));
    }

    private void subtestENG7872MP() throws IOException, ProcCallException
    {
        Client client = getClient();
        truncateBeforeTest(client);
        VoltTable[] results = null;

        assertAggNoGroupBy(client, "MATPEOPLE_COUNT", "0");
        assertAggNoGroupBy(client, "MATPEOPLE_CONDITIONAL_COUNT", "0");
        assertAggNoGroupBy(client, "MATPEOPLE_CONDITIONAL_COUNT_SUM", "0", "null");
        assertAggNoGroupBy(client, "MATPEOPLE_CONDITIONAL_COUNT_MIN_MAX", "0", "null", "null");

        results = client.callProcedure("AddPerson", 1, 1L, 31L, 1000.0, 3, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("AddPerson", 1, 2L, 31L, 900.0, 5, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("AddPerson", 1, 3L, 31L, 900.0, 1, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("AddPerson", 1, 4L, 31L, 2500.0, 5, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("AddPerson", 2, 5L, 31L, 1000.0, 3, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("AddPerson", 2, 6L, 31L, 900.0, 5, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("AddPerson", 2, 7L, 31L, 900.0, 1, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("AddPerson", 2, 8L, 31L, 2500.0, 5, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());

        assertAggNoGroupBy(client, "MATPEOPLE_COUNT", "8");
        assertAggNoGroupBy(client, "MATPEOPLE_CONDITIONAL_COUNT", "4");
        assertAggNoGroupBy(client, "MATPEOPLE_CONDITIONAL_COUNT_SUM", "4", "8");
        assertAggNoGroupBy(client, "MATPEOPLE_CONDITIONAL_COUNT_MIN_MAX", "6", "900.0", "5");

        results = client.callProcedure("DeletePerson", 1, 2L, NORMALLY).getResults();

        assertAggNoGroupBy(client, "MATPEOPLE_COUNT", "7");
        assertAggNoGroupBy(client, "MATPEOPLE_CONDITIONAL_COUNT", "3");
        assertAggNoGroupBy(client, "MATPEOPLE_CONDITIONAL_COUNT_SUM", "4", "8");
        assertAggNoGroupBy(client, "MATPEOPLE_CONDITIONAL_COUNT_MIN_MAX", "5", "900.0", "5");

        results = client.callProcedure("DeletePerson", 2, 6L, NORMALLY).getResults();

        assertAggNoGroupBy(client, "MATPEOPLE_COUNT", "6");
        assertAggNoGroupBy(client, "MATPEOPLE_CONDITIONAL_COUNT", "2");
        assertAggNoGroupBy(client, "MATPEOPLE_CONDITIONAL_COUNT_SUM", "4", "8");
        assertAggNoGroupBy(client, "MATPEOPLE_CONDITIONAL_COUNT_MIN_MAX", "4", "1000.0", "5");

        results = client.callProcedure("UpdatePerson", 1, 3L, 31L, 200, 9).getResults();
        results = client.callProcedure("UpdatePerson", 2, 7L, 31L, 200, 9).getResults();

        assertAggNoGroupBy(client, "MATPEOPLE_COUNT", "6");
        assertAggNoGroupBy(client, "MATPEOPLE_CONDITIONAL_COUNT", "4");
        assertAggNoGroupBy(client, "MATPEOPLE_CONDITIONAL_COUNT_SUM", "2", "6");
        assertAggNoGroupBy(client, "MATPEOPLE_CONDITIONAL_COUNT_MIN_MAX", "6", "200.0", "9");

        results = client.callProcedure("UpdatePerson", 1, 4L, 31L, 0, 10).getResults();
        results = client.callProcedure("UpdatePerson", 2, 8L, 31L, 0, 10).getResults();

        assertAggNoGroupBy(client, "MATPEOPLE_COUNT", "6");
        assertAggNoGroupBy(client, "MATPEOPLE_CONDITIONAL_COUNT", "4");
        assertAggNoGroupBy(client, "MATPEOPLE_CONDITIONAL_COUNT_SUM", "2", "6");
        assertAggNoGroupBy(client, "MATPEOPLE_CONDITIONAL_COUNT_MIN_MAX", "6", "0.0", "10");

        results = client.callProcedure("DeletePerson", 1, 1L, NORMALLY).getResults();
        results = client.callProcedure("DeletePerson", 2, 5L, NORMALLY).getResults();

        assertAggNoGroupBy(client, "MATPEOPLE_COUNT", "4");
        assertAggNoGroupBy(client, "MATPEOPLE_CONDITIONAL_COUNT", "4");
        assertAggNoGroupBy(client, "MATPEOPLE_CONDITIONAL_COUNT_SUM", "0", "null");
        assertAggNoGroupBy(client, "MATPEOPLE_CONDITIONAL_COUNT_MIN_MAX", "4", "0.0", "10");
    }

    public void UNtestMPAndRegressions() throws IOException, ProcCallException
    {
        subtestMultiPartitionSimple();
        subtestInsertReplicated();
        subtestInsertAndOverflowSum();
        subtestENG798();
        subtestIndexed();
        subtestMinMaxMultiPartition();
        subtestENG7872MP();
        subtestENG6511(true);
    }

    private void subtestMultiPartitionSimple() throws IOException, ProcCallException
    {
        Client client = getClient();
        truncateBeforeTest(client);
        VoltTable[] results = null;

        results = client.callProcedure("AggAges", 1).getResults();
        assertEquals(1, results.length);
        assertEquals(0, results[0].getRowCount());
        assert(results != null);

        results = client.callProcedure("AddPerson", 1, 1L, 31L, 1000.0, 3, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("AddPerson", 1, 2L, 2L, 1000.0, 3, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("AddPerson", 2, 3L, 23L, 1000.0, 3, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("AddPerson", 2, 4L, 23L, 1000.0, 3, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("AddPerson", 2, 5L, 35L, 1000.0, 3, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("AddPerson", 2, 6L, 35L, 1000.0, 3, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("UpdatePerson", 1, 2L, 32L, 1000.0, 3).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("DeletePerson", 2, 6L, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());

        results = client.callProcedure("AggAges", 1).getResults();
        assert(results != null);
        assertEquals(1, results.length);

        VoltTable results2[] = client.callProcedure("AggAges", 2).getResults();
        assert(results != null);
        assertEquals(1, results2.length);

        int totalRows = results[0].getRowCount() + results2[0].getRowCount();
        // unfortunately they're both 4 in the hsql case, the fact that partitioning
        // can change behavior between backends if not used smartly should be corrected
        assertTrue((4 == totalRows) ||
                   (results[0].getRowCount() == 4) || (results2[0].getRowCount() == 4));
    }

    private void subtestInsertReplicated() throws IOException, ProcCallException
    {
        Client client = getClient();
        truncateBeforeTest(client);
        VoltTable[] results = null;

        results = client.callProcedure("AggThings").getResults();
        assertEquals(1, results.length);
        assertEquals(0, results[0].getRowCount());
        assert(results != null);

        results = client.callProcedure("AddThing", 1L, 10L).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("AddThing", 2L, 12L).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("AddThing", 3L, 10L).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());

        results = client.callProcedure("AggThings").getResults();
        assertEquals(1, results.length);
        assertEquals(2, results[0].getRowCount());
        assert(results != null);
    }

    private void subtestInsertAndOverflowSum() throws IOException, ProcCallException
    {
        if (isHSQL()) {
            return;
        }
        Client client = getClient();
        truncateBeforeTest(client);
        int invocationIndex = 0;
        VoltTable[] results = client.callProcedure("OverflowTest", 0, 0, invocationIndex++).getResults();
        results = client.callProcedure("OverflowTest", 2, 0, invocationIndex++).getResults();
        results = client.callProcedure("OverflowTest", 1, 0, 0).getResults();
        results[0].advanceRow();
        long preRollbackValue = results[0].getLong(3);
        boolean threwException = false;
        try {
            results = client.callProcedure("OverflowTest", 0, 0, invocationIndex++).getResults();
        } catch (Exception e) {
           threwException = true;
        }
        assertTrue(threwException);
        results = client.callProcedure("OverflowTest", 1, 0, 0).getResults();
        results[0].advanceRow();
        assertEquals(preRollbackValue, results[0].getLong(3));
        preRollbackValue = 0;
        threwException = false;
        while (!threwException) {
            try {
                results = client.callProcedure("OverflowTest", 2, 0, invocationIndex++).getResults();
                results = client.callProcedure("OverflowTest", 1, 0, 0).getResults();
                results[0].advanceRow();
                preRollbackValue = results[0].getLong(2);
            } catch (Exception e) {
                threwException = true;
                break;
            }
        }
        results = client.callProcedure("OverflowTest", 1, 0, 0).getResults();
        results[0].advanceRow();
        assertEquals(preRollbackValue, results[0].getLong(2));
    }

    /** Test a view that re-orders the source table's columns */
    private void subtestENG798() throws IOException, ProcCallException
    {
        if (isHSQL()) {
            return;
        }

        // this would throw on a bad cast in the broken case.
        Client client = getClient();
        truncateBeforeTest(client);
        ClientResponse callProcedure = client.callProcedure("Eng798Insert", "clientname");
        assertTrue(callProcedure.getStatus() == ClientResponse.SUCCESS);
        assertEquals(1, callProcedure.getResults().length);
        assertEquals(1, callProcedure.getResults()[0].asScalarLong());
    }


    private void subtestIndexed() throws IOException, ProcCallException
    {
        Client client = getClient();
        truncateBeforeTest(client);
        VoltTable[] results = null;

        results = client.callProcedure("AggAges", 1).getResults();
        assertEquals(1, results.length);
        assertEquals(0, results[0].getRowCount());
        assert(results != null);

        results = client.callProcedure("AddPerson", 1, 1L, 31L, 1000.0, 3, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("AddPerson", 1, 2L, 31L, 1000.0, 3, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("AddPerson", 1, 3L, 33L, 28920.99, 3, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("AddPerson", 2, 4L, 23L, 1000.0, 3, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("AddPerson", 2, 5L, 35L, 1000.0, 3, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("AddPerson", 2, 6L, 35L, 1000.0, 3, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("AddPerson", 2, 7L, 23L, 1000.0, 3, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("AddPerson", 2, 8L, 31L, 2222.22, 3, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("UpdatePerson", 1, 2L, 32L, 1000.0, 3).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("DeletePerson", 2, 6L, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());

        int totalRows;
        // INDEXED_FIRST_GROUP   AS SELECT AGE, SALARIES LIMIT 1;
        results = client.callProcedure("INDEXED_FIRST_GROUP").getResults();
        assert(results != null);
        assertEquals(1, results.length);
        totalRows = results[0].getRowCount();
        assertEquals(1, totalRows);
        results[0].advanceRow();
        assertEquals(33L, results[0].getLong(0));
        assertEquals(28920.99, results[0].getDouble(1), 0.001);

        // INDEXED_MAX_GROUP     AS SELECT MAX(SALARIES);
        results = client.callProcedure("INDEXED_MAX_GROUP").getResults();
        assert(results != null);
        assertEquals(1, results.length);
        totalRows = results[0].getRowCount();
        assertEquals(1, totalRows);
        results[0].advanceRow();
        assertEquals(28920.99, results[0].getDouble(0), 0.001);

        // INDEXED_MAX_IN_GROUPS AS SELECT MAX(SALARIES) WHERE AGE = ?;
        results = client.callProcedure("INDEXED_MAX_IN_GROUPS", 31L).getResults();
        assert(results != null);
        assertEquals(1, results.length);
        totalRows = results[0].getRowCount();
        assertEquals(1, totalRows);
        results[0].advanceRow();
        assertEquals(2222.22, results[0].getDouble(0), 0.001);

        // INDEXED_GROUPS: AGE, SALARIES, PARTITION, NUM, KIDS ORDER BY AGE, SALARIES */
        results = client.callProcedure("INDEXED_GROUPS").getResults();
        assert(results != null);
        totalRows = results[0].getRowCount();
        assertEquals(6, totalRows);
        results[0].advanceRow();
        assertEquals(23L, results[0].getLong(0));
        assertEquals(2000.0, results[0].getDouble(1), 0.001);
        results[0].advanceRow();
        assertEquals(31L, results[0].getLong(0));
        assertEquals(1000.0, results[0].getDouble(1), 0.001);
        results[0].advanceRow();
        assertEquals(31L, results[0].getLong(0));
        assertEquals(2222.22, results[0].getDouble(1), 0.001);
        results[0].advanceRow();
        assertEquals(32L, results[0].getLong(0));
        assertEquals(1000.00, results[0].getDouble(1), 0.001);

        long timestampInitializer;
        int ii;

        int delay = 0; // keeps the clock moving forward.
        // +1 V_TEAM_MEMBERSHIP, +1 V_TEAM_TIMES
        timestampInitializer = (System.currentTimeMillis() + (++delay))*1000;
        insertRow(client, "CONTEST", "Senior", timestampInitializer, "Boston", "Jack");

        // +1 V_TEAM_MEMBERSHIP, +4 V_TEAM_TIMES
        for (ii = 0; ii < 4; ++ii) {
            timestampInitializer = (System.currentTimeMillis() + (++delay))*1000;
            insertRow(client, "CONTEST", "Senior", timestampInitializer, "Cambridge", "anonymous " + ii);
        }

        // +0 V_TEAM_MEMBERSHIP, +1 V_TEAM_TIMES
        timestampInitializer = (System.currentTimeMillis() + (++delay))*1000;
        for (ii = 0; ii < 3; ++ii) {
            insertRow(client, "CONTEST", "Senior", timestampInitializer, "Boston",  "not Jack " + ii);
        }

        // +1 V_TEAM_MEMBERSHIP, +1 V_TEAM_TIMES
        timestampInitializer = (System.currentTimeMillis() + (++delay))*1000;
        for (ii = 0; ii < 3; ++ii) {
            insertRow(client, "CONTEST", "Senior", timestampInitializer, "Concord", "Emerson " + ii);
        }

        // +1 V_TEAM_MEMBERSHIP, +2 V_TEAM_TIMES
        for (ii = 0; ii < 2; ++ii) {
            timestampInitializer = (System.currentTimeMillis() + (++delay))*1000;
            insertRow(client, "CONTEST", "Senior", timestampInitializer, "Lexington", "Luis " + ii);
        }

        if ( ! isHSQL()) {
            results = client.callProcedure("@AdHoc",
                "SELECT team, total, finish FROM V_TEAM_TIMES " +
                "ORDER BY total DESC, 0-SINCE_EPOCH(MILLISECOND, finish) DESC").getResults();
            assertEquals(1, results.length);
            System.out.println(results[0]);
            assertEquals(9, results[0].getRowCount());
            results[0].advanceRow();
            assertEquals("Boston", results[0].getString(0));
            assertEquals(3, results[0].getLong(1));
            results[0].advanceRow();
            assertEquals("Concord", results[0].getString(0));
            assertEquals(3, results[0].getLong(1));

            results[0].advanceToRow(8);
            assertEquals("Lexington", results[0].getString(0));
            assertEquals(1, results[0].getLong(1));
        }

        /**
         * Current data in MV table: V_TEAM_MEMBERSHIP.
         *  header size: 39
         *   status code: -128 column count: 3
         *   cols (RUNNER_CLASS:STRING), (TEAM:STRING), (TOTAL:INTEGER),
         *   rows -
         *    Senior,Boston,4
         *    Senior,Cambridge,4
         *    Senior,Concord,3
         *    Senior,Lexington,2
         */
        results = client.callProcedure("@AdHoc",
                "SELECT count(*) FROM V_TEAM_MEMBERSHIP where team > 'Cambridge' order by total").getResults();
        assertEquals(1, results.length);
        System.out.println(results[0]);
        assertEquals(2L, results[0].asScalarLong());

        results = client.callProcedure("@AdHoc",
                "SELECT count(*) FROM V_TEAM_MEMBERSHIP where total > 3 ").getResults();
        assertEquals(1, results.length);
        System.out.println(results[0]);
        assertEquals(2L, results[0].asScalarLong());



        results = client.callProcedure("@AdHoc",
                "SELECT team, finish FROM V_TEAM_TIMES ORDER BY finish DESC limit 3").getResults();
        assertEquals(1, results.length);
        System.out.println(results[0]);
        assertEquals(3, results[0].getRowCount());
        results[0].advanceRow();
        assertEquals("Lexington", results[0].getString(0));
        results[0].advanceRow();
        assertEquals("Lexington", results[0].getString(0));
        results[0].advanceRow();
        assertEquals("Concord", results[0].getString(0));
    }

    private void subtestMinMaxMultiPartition() throws IOException, ProcCallException {
        Client client = getClient();
        truncateBeforeTest(client);
        VoltTable[] results = null;
        VoltTable t;

        results = client.callProcedure("@AdHoc", "SELECT * FROM MATPEOPLE2").getResults();
        assert(results != null);
        assertEquals(1, results.length);
        assertEquals(0, results[0].getRowCount());

        results = client.callProcedure("AddPerson", 1, 1L, 31L, 1000.0, 3, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("AddPerson", 1, 2L, 31L, 900.0, 5, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("AddPerson", 1, 3L, 31L, 900.0, 1, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("AddPerson", 1, 4L, 31L, 2500.0, 5, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("AddPerson", 2, 5L, 31L, 1000.0, 3, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("AddPerson", 2, 6L, 31L, 900.0, 5, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("AddPerson", 2, 7L, 31L, 900.0, 1, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
        results = client.callProcedure("AddPerson", 2, 8L, 31L, 2500.0, 5, NORMALLY).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());

        results = client.callProcedure("@AdHoc", "SELECT * FROM MATPEOPLE2").getResults();
        assert(results != null);
        assertEquals(1, results.length);
        t = results[0];
        assertEquals(2, t.getRowCount());
        System.out.println(t.toString());
        t.advanceRow();
        assertEquals(4, t.getLong(2));
        assertEquals(900, (int)(t.getDouble(3)));
        assertEquals(5, t.getLong(4));
        t.advanceRow();
        assertEquals(4, t.getLong(2));
        assertEquals(900, (int)(t.getDouble(3)));
        assertEquals(5, t.getLong(4));

        results = client.callProcedure("DeletePerson", 1, 2L, NORMALLY).getResults();
        assert(results != null);
        assertEquals(1, results.length);
        assertEquals(1, results[0].getRowCount());
        results = client.callProcedure("DeletePerson", 2, 6L, NORMALLY).getResults();
        assert(results != null);
        assertEquals(1, results.length);
        assertEquals(1, results[0].getRowCount());

        results = client.callProcedure("@AdHoc", "SELECT * FROM MATPEOPLE2").getResults();
        assert(results != null);
        assertEquals(1, results.length);
        t = results[0];
        assertEquals(2, t.getRowCount());
        System.out.println(t.toString());
        t.advanceRow();
        assertEquals(3, t.getLong(2));
        assertEquals(900, (int)(t.getDouble(3)));
        assertEquals(5, t.getLong(4));
        t.advanceRow();
        assertEquals(3, t.getLong(2));
        assertEquals(900, (int)(t.getDouble(3)));
        assertEquals(5, t.getLong(4));

        results = client.callProcedure("UpdatePerson", 1, 3L, 31L, 200, 9).getResults();
        assert(results != null);
        assertEquals(1, results.length);
        assertEquals(1, results[0].getRowCount());
        results = client.callProcedure("UpdatePerson", 2, 7L, 31L, 200, 9).getResults();
        assert(results != null);
        assertEquals(1, results.length);
        assertEquals(1, results[0].getRowCount());

        results = client.callProcedure("@AdHoc", "SELECT * FROM MATPEOPLE2").getResults();
        assert(results != null);
        assertEquals(1, results.length);
        t = results[0];
        assertEquals(2, t.getRowCount());
        System.out.println(t.toString());
        t.advanceRow();
        assertEquals(3, t.getLong(2));
        assertEquals(200, (int)(t.getDouble(3)));
        assertEquals(9, t.getLong(4));
        t.advanceRow();
        assertEquals(3, t.getLong(2));
        assertEquals(200, (int)(t.getDouble(3)));
        assertEquals(9, t.getLong(4));

        results = client.callProcedure("UpdatePerson", 1, 4L, 31L, 0, 10).getResults();
        assert(results != null);
        assertEquals(1, results.length);
        assertEquals(1, results[0].getRowCount());
        results = client.callProcedure("UpdatePerson", 2, 8L, 31L, 0, 10).getResults();
        assert(results != null);
        assertEquals(1, results.length);
        assertEquals(1, results[0].getRowCount());

        results = client.callProcedure("@AdHoc", "SELECT * FROM MATPEOPLE2").getResults();
        assert(results != null);
        assertEquals(1, results.length);
        t = results[0];
        assertEquals(2, t.getRowCount());
        System.out.println(t.toString());
        t.advanceRow();
        assertEquals(3, t.getLong(2));
        assertEquals(0, (int)(t.getDouble(3)));
        assertEquals(10, t.getLong(4));
        t.advanceRow();
        assertEquals(3, t.getLong(2));
        assertEquals(0, (int)(t.getDouble(3)));
        assertEquals(10, t.getLong(4));

        results = client.callProcedure("DeletePerson", 1, 1L, NORMALLY).getResults();
        results = client.callProcedure("DeletePerson", 2, 5L, NORMALLY).getResults();

        results = client.callProcedure("@AdHoc", "SELECT * FROM MATPEOPLE2").getResults();
        assert(results != null);
        assertEquals(1, results.length);
        t = results[0];
        assertEquals(2, t.getRowCount());
        System.out.println(t.toString());
        t.advanceRow();
        assertEquals(2, t.getLong(2));
        assertEquals(0, (int)(t.getDouble(3)));
        assertEquals(10, t.getLong(4));
        t.advanceRow();
        assertEquals(2, t.getLong(2));
        assertEquals(0, (int)(t.getDouble(3)));
        assertEquals(10, t.getLong(4));

    }

    private void insertRow(Client client, Object... parameters) throws IOException, ProcCallException
    {
        VoltTable[] results = null;
        results = client.callProcedure(parameters[0].toString() + ".insert", Arrays.copyOfRange(parameters, 1, parameters.length)).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
    }

    private void deleteRow(Client client, Object... parameters) throws IOException, ProcCallException
    {
        VoltTable[] results = null;
        String tableName = parameters[0].toString();
        if (tableName.equalsIgnoreCase("ORDERITEMS")) {
            results = client.callProcedure("DELETEORDERITEMS", parameters[1], parameters[2]).getResults();
        }
        else {
            results = client.callProcedure(tableName + ".delete", parameters[1]).getResults();
        }
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
    }

    private void updateRow(Client client, Object[] oldRow, Object[] newRow) throws IOException, ProcCallException
    {
        VoltTable[] results = null;
        String tableName1 = oldRow[0].toString();
        String tableName2 = newRow[0].toString();
        assertEquals("Trying to update table " + tableName1 + " with " + tableName2 + " data.", tableName1, tableName2);
        results = client.callProcedure("UPDATE" + tableName1, newRow[2], newRow[3],
                                                              oldRow[1], oldRow[2], oldRow[3]).getResults();
        assertEquals(1, results.length);
        assertEquals(1L, results[0].asScalarLong());
    }

    private void verifyViewOnJoinQueryResult(Client client) throws IOException, ProcCallException
    {
        VoltTable vresult = null;
        VoltTable tresult = null;
        String prefix = "Assertion failed comparing the view content and the AdHoc query result ";

        vresult = client.callProcedure("@AdHoc", "SELECT * FROM ORDER_COUNT_NOPCOL ORDER BY 1;").getResults()[0];
        tresult = client.callProcedure("PROC_ORDER_COUNT_NOPCOL").getResults()[0];
        assertTablesAreEqual(prefix + "ORDER_COUNT_NOPCOL: ", tresult, vresult);

        vresult = client.callProcedure("@AdHoc", "SELECT * FROM ORDER_COUNT_GLOBAL ORDER BY 1;").getResults()[0];
        tresult = client.callProcedure("PROC_ORDER_COUNT_GLOBAL").getResults()[0];
        assertTablesAreEqual(prefix + "ORDER_COUNT_GLOBAL: ", tresult, vresult);

        vresult = client.callProcedure("@AdHoc", "SELECT * FROM ORDER_DETAIL_NOPCOL ORDER BY 1;").getResults()[0];
        tresult = client.callProcedure("PROC_ORDER_DETAIL_NOPCOL").getResults()[0];
        assertTablesAreEqual(prefix + "ORDER_DETAIL_NOPCOL: ", tresult, vresult);

        vresult = client.callProcedure("@AdHoc", "SELECT * FROM ORDER_DETAIL_WITHPCOL ORDER BY 1, 2;").getResults()[0];
        tresult = client.callProcedure("PROC_ORDER_DETAIL_WITHPCOL").getResults()[0];
        assertTablesAreEqual(prefix + "ORDER_DETAIL_WITHPCOL: ", tresult, vresult);

        vresult = client.callProcedure("@AdHoc", "SELECT * FROM ORDER2016 ORDER BY 1;").getResults()[0];
        tresult = client.callProcedure("PROC_ORDER2016").getResults()[0];
        assertTablesAreEqual(prefix + "ORDER2016: ", tresult, vresult);

        vresult = client.callProcedure("@AdHoc", "SELECT * FROM QTYPERPRODUCT ORDER BY 1;").getResults()[0];
        tresult = client.callProcedure("PROC_QTYPERPRODUCT").getResults()[0];
        assertTablesAreEqual(prefix + "QTYPERPRODUCT: ", tresult, vresult);
    }

    public void testViewOnJoinQuery() throws IOException, ProcCallException
    {
        Client client = getClient();
        ArrayList<Object[]> dataList1 = Lists.newArrayList(
            new Object[][] {
                {"CUSTOMERS", 1, "Tom", "VoltDB"},
                {"ORDERS", 1, 2, "2016-04-23 13:24:57.671000"},
                {"PRODUCTS", 1, "H MART", 20.97},
                {"PRODUCTS", 2, "COSTCO WHOLESALE", 62.66},
                {"ORDERITEMS", 1, 2, 1},
            }
        );
        ArrayList<Object[]> dataList2 = Lists.newArrayList(
            new Object[][] {
                {"CUSTOMERS", 6, "Mike", "WPI"},
                {"ORDERS", 5, 3, "2015-04-23 00:24:45.768000"},
                {"PRODUCTS", 16, "SAN SOO KAP SAN SHUSHI", 10.69},
                {"PRODUCTS", 17, "PLASTC INC.", 155.00},
                {"ORDERITEMS", 5, 12, 6},
            }
        );
        assertEquals(dataList1.size(), dataList2.size());

        // -- 1 -- Test updating the data in the source tables.
        // There are two lists of data, we first insert the data in the first list
        // to the corresponding source tables, then update each row with the data
        // from the second data list.
        System.out.println("Now testing updating the join query view source table.");
        for (int i=0; i<dataList1.size(); i++) {
            insertRow(client, dataList1.get(i));
        }

        // -- 2 -- Test inserting the data into the source tables.
        // We do a shuffle here and in the delete test. But I do believe we still
        // have the full coverage of all the cases because we are inserting and deleting
        // all the rows. The cases updating values of all kinds of aggregations will be
        // tested in one row or another.
        truncateBeforeTest(client);

    }

    public void UNtestEng11024() throws Exception {
        // Regression test for ENG-11024, found by sqlcoverage
        Client client = getClient();

        // This is an edge case where a view with no group by keys
        // is having its output tuples counted with count(*).  Of course,
        // the result of this query will always be one.  This query was
        // causing anissue because intermediate temp tables had zero columns.
        VoltTable vt;
        vt = client.callProcedure("@AdHoc", "select count(*) from v3_eng_11024_join").getResults()[0];
        assertEquals(vt.asScalarLong(), 1);

        vt = client.callProcedure("@AdHoc", "select count(*) from v3_eng_11024_1tbl").getResults()[0];
        assertEquals(vt.asScalarLong(), 1);

        vt = client.callProcedure("@AdHoc",
                "select count(*) "
                        + "from v3_eng_11024_1tbl inner join r1_eng_11024 using (ratio)").getResults()[0];
        assertEquals(0, vt.asScalarLong());

        vt = client.callProcedure("@AdHoc",
                "select count(*) "
                        + "from v3_eng_11024_1tbl left outer join r1_eng_11024 using (ratio)").getResults()[0];
        assertEquals(1, vt.asScalarLong());
    }

    public void UNtestUpdateAndMinMax() throws Exception {
        Client client = getClient();

        //        CREATE TABLE P2_ENG_11024 (
        //                ID INTEGER NOT NULL,
        //                VCHAR VARCHAR(300),
        //                NUM INTEGER,
        //                RATIO FLOAT,
        //                PRIMARY KEY (ID)
        //        );
        //        PARTITION TABLE P2_ENG_11024 ON COLUMN ID;
        //
        //        CREATE TABLE P1_ENG_11024 (
        //                ID INTEGER NOT NULL,
        //                VCHAR VARCHAR(300),
        //                NUM INTEGER,
        //                RATIO FLOAT,
        //                PRIMARY KEY (ID)
        //        );
        //
        //        CREATE VIEW V16_ENG_11042 (ID, COUNT_STAR, NUM) AS
        //            SELECT T2.NUM, COUNT(*), MAX(T1.NUM)
        //            FROM R1_ENG_11024 T1 JOIN R2_ENG_11024 T2 ON T1.ID = T2.ID
        //            GROUP BY T2.NUM;

        client.callProcedure("R1_ENG_11024.Insert", 1, "", 20, 0.0);
        client.callProcedure("R2_ENG_11024.Insert", 1, "", 100, 0.0);

        client.callProcedure("R1_ENG_11024.Insert", 2, "", 1000, 0.0);
        client.callProcedure("R2_ENG_11024.Insert", 2, "", 100, 0.0);

        VoltTable vt;
        vt = client.callProcedure("@AdHoc",
                "select * from V16_ENG_11042").getResults()[0];
        assertContentOfTable(new Object[][] {
            {100, 2, 1000}}, vt);

        vt = client.callProcedure("@AdHoc",
                "update R1_ENG_11024 set num = 15 where id = 2;")
                .getResults()[0];
        assertContentOfTable(new Object[][] {{1}}, vt);
        vt = client.callProcedure("@AdHoc",
                "select * from V16_ENG_11042").getResults()[0];
        assertContentOfTable(new Object[][] {
            {100, 2, 20}}, vt);

        // A second way of reproducing, slightly different
        client.callProcedure("@AdHoc", "DELETE FROM R1_ENG_11024");
        client.callProcedure("@AdHoc", "DELETE FROM R2_ENG_11024");

        client.callProcedure("@AdHoc", "INSERT INTO R1_ENG_11024 VALUES(-13, 'mmm', -6, -13.0);");
        client.callProcedure("@AdHoc", "INSERT INTO R2_ENG_11024 VALUES(-13, 'mmm', -4, -13.0);");

        client.callProcedure("@AdHoc", "INSERT INTO R1_ENG_11024 VALUES(-12, 'mmm', -12, -12.0);");
        client.callProcedure("@AdHoc", "INSERT INTO R2_ENG_11024 VALUES(-12, 'mmm', -4, -12.0);");

        vt = client.callProcedure("@AdHoc",
                "select * from V16_ENG_11042").getResults()[0];
        assertContentOfTable(new Object[][] {
            {-4, 2, -6}}, vt);

        client.callProcedure("@AdHoc", "UPDATE R1_ENG_11024 A SET NUM = ID WHERE ID=-13;");

        vt = client.callProcedure("@AdHoc",
                "select * from V16_ENG_11042").getResults()[0];
        assertContentOfTable(new Object[][] {
            {-4, 2, -12}}, vt);
    }

    /**
     * Build a list of the tests that will be run when TestTPCCSuite gets run by JUnit.
     * Use helper classes that are part of the RegressionSuite framework.
     * This particular class runs all tests on the the local JNI backend with both
     * one and two partition configurations, as well as on the hsql backend.
     *
     * @return The TestSuite containing all the tests to be run.
     */
    static public Test suite() {
        URL url = AddPerson.class.getResource("matviewsuite-ddl.sql");
        String schemaPath = url.getPath();

        // the suite made here will all be using the tests from this class
        MultiConfigSuiteBuilder builder = new MultiConfigSuiteBuilder(TestMaterializedViewSuite.class);

        // build up a project builder for the workload
        VoltProjectBuilder project = new VoltProjectBuilder();
        project.setUseDDLSchema(true);
        //project.setBackendTarget(BackendTarget.NATIVE_EE_IPC);
        project.addSchema(schemaPath);

        project.addProcedures(PROCEDURES);

        /////////////////////////////////////////////////////////////
        // CONFIG #1: 1 Local Site/Partition running on JNI backend
        /////////////////////////////////////////////////////////////

        // get a server config for the native backend with one sites/partitions
        VoltServerConfig config = new LocalCluster("matview-onesite.jar", 1, 1, 0, BackendTarget.NATIVE_EE_JNI);

        // build the jarfile
        boolean success = config.compile(project);
        assertTrue(success);

        // add this config to the set of tests to run
        builder.addServerConfig(config);
        return builder;
    }
}
