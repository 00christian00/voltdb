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

package org.voltdb.regressionsuites;

import java.io.IOException;

import org.voltdb.BackendTarget;
import org.voltdb.VoltTable;
import org.voltdb.client.Client;
import org.voltdb.client.ClientResponse;
import org.voltdb.compiler.VoltProjectBuilder;
import org.voltdb_testprocs.regressionsuites.doubledown.MultiRoundInsertIntoSelect;
import org.voltdb_testprocs.regressionsuites.doubledown.MultiRoundSelectThenInsert;

public class TestInsertIntoSelectSuite extends RegressionSuite {

    private static final Class<?>[] PROCEDURES = {
        MultiRoundInsertIntoSelect.class,
        MultiRoundSelectThenInsert.class,
    };

    public TestInsertIntoSelectSuite(String name) {
        super(name);
    }

    static public junit.framework.Test suite() {
        VoltServerConfig config = null;
        final MultiConfigSuiteBuilder builder = new MultiConfigSuiteBuilder(TestInsertIntoSelectSuite.class);

        final VoltProjectBuilder project = new VoltProjectBuilder();

        try {
//            // a table that should generate procedures
//            // use column names such that lexical order != column order.
//            project.addLiteralSchema(
//                    "CREATE TABLE p1(b1 BIGINT NOT NULL, a2 VARCHAR(100) NOT NULL);" +
//                    "PARTITION TABLE p1 ON COLUMN b1;" +
//
//                    "CREATE TABLE p2(a1 BIGINT NOT NULL, a2 VARCHAR(100) NOT NULL); " +
//                    "PARTITION TABLE p2 ON COLUMN a1;" +
//                    "CREATE UNIQUE INDEX p2_tree_idx ON p2(a1);" +
//
//                    "CREATE TABLE p3(b1 BIGINT NOT NULL, a2 VARCHAR(100) NOT NULL);" +
//                    "PARTITION TABLE p3 ON COLUMN b1;" +
//
//                                      "CREATE TABLE p4(bi BIGINT default 1024 not null, " +
//                                                      "vc VARCHAR(100) default 'cocker spaniel'," +
//                                                      "ii INTEGER default 512," +
//                                                      "ti TINYINT default 32);" +
//                                      "PARTITION TABLE p4 ON COLUMN bi;" +
//
//
//                    "CREATE TABLE r1(b1 BIGINT NOT NULL, a2 VARCHAR(100) NOT NULL, PRIMARY KEY (b1));" +
//
//                    "CREATE TABLE r2(a1 BIGINT NOT NULL, a2 VARCHAR(100) DEFAULT 'xyz' NOT NULL, PRIMARY KEY (a1));" +
//
//                    "CREATE PROCEDURE CountP1 AS select count(*) from p1;" +
//                    "CREATE PROCEDURE CountP2 AS select count(*) from p2;" +
//                    "CREATE PROCEDURE CountR1 AS select count(*) from r1;" +
//                    "CREATE PROCEDURE CountR2 AS select count(*) from r2;" +
//                    "");

                project.addLiteralSchema(
                                "CREATE TABLE target_p (bi bigint not null," +
                                                           "vc varchar(100) default 'daschund'," +
                                                           "ii integer default 127," +
                                                           "ti tinyint default 7);" +
                            "partition table target_p on column bi;" +

                            "create procedure insert_p_source_p as insert into target_p (bi, vc, ii, ti) select * from source_p1 where bi = ?;" +
                            "partition procedure insert_p_source_p on table target_p column bi;" +

                            "create procedure CountTargetP as select count(*) from target_p;" +

                            "CREATE TABLE target_r (bi bigint not null," +
                                                                   "vc varchar(100) default 'daschund'," +
                                                                   "ii integer default 127," +
                                                                   "ti tinyint default 7);" +

                                "CREATE TABLE source_p1 (bi bigint not null," +
                                           "vc varchar(100) default 'daschund'," +
                                           "ii integer default 127," +
                                           "ti tinyint default 7);" +
                                           "partition table source_p1 on column bi;" +

                                "CREATE TABLE source_p2 (bi bigint not null," +
                                           "vc varchar(100) default 'daschund'," +
                                           "ii integer default 127," +
                                           "ti tinyint default 7);" +
                                           "partition table source_p2 on column bi;" +

                                        "create procedure InsertIntoSelectWithJoin as " +
                                                "insert into target_p " +
                                                        "select sp1.bi, sp1.vc, sp2.ii, sp2.ti " +
                                                        "from source_p1 as sp1 inner join source_p2 as sp2 on sp1.bi = sp2.bi " +
                                                        "where sp1.bi between ? and ?;" +
                                        "partition procedure InsertIntoSelectWithJoin on table target_p column bi;" +

                                        "CREATE TABLE source_r (bi bigint not null," +
                                                                                   "vc varchar(100) default 'daschund'," +
                                                                                   "ii integer default 127," +
                                                                                   "ti tinyint default 7);" +
                                "");

            //project.addProcedures(PROCEDURES);
        } catch (IOException error) {
            fail(error.getMessage());
        }

        // JNI
        config = new LocalCluster("iisf-onesite.jar", 1, 1, 0, BackendTarget.NATIVE_EE_JNI);
        boolean t1 = config.compile(project);
        assertTrue(t1);
        builder.addServerConfig(config);

        /*/ CLUSTER (disable to opt for speed over coverage...
        config = new LocalCluster("iisf-cluster.jar", 2, 3, 1, BackendTarget.NATIVE_EE_JNI);
        boolean t2 = config.compile(project);
        assertTrue(t2);
        builder.addServerConfig(config);
        // ... disable for speed) */
        return builder;
    }

    public void testPartitionedTableSimple() throws Exception
    {
        final Client client = getClient();
        for (int i=0; i < 10; i++) {
            ClientResponse resp = client.callProcedure("SOURCE_P1.insert", i, Integer.toHexString(i), i, i);
            assertEquals(ClientResponse.SUCCESS, resp.getStatus());
            assertEquals(1, resp.getResults()[0].asScalarLong());
        }

        ClientResponse resp;

        resp = client.callProcedure("insert_p_source_p", 5);
        assertEquals(ClientResponse.SUCCESS, resp.getStatus());
        assertEquals(1, resp.getResults()[0].asScalarLong());

        resp = client.callProcedure("CountTargetP");
        assertEquals(ClientResponse.SUCCESS, resp.getStatus());
        assertEquals(1, resp.getResults()[0].asScalarLong());

        // verify that the corresponding rows in both tables are the same
        String selectAllSource = "select * from source_p1 where bi = 5 order by bi";
        String selectAllTarget = "select * from target_p order by bi";

        resp = client.callProcedure("@AdHoc", selectAllSource);
        assertEquals(ClientResponse.SUCCESS, resp.getStatus());
        VoltTable sourceRows = resp.getResults()[0];

        resp = client.callProcedure("@AdHoc", selectAllTarget);
        assertEquals(ClientResponse.SUCCESS, resp.getStatus());
        VoltTable targetRows = resp.getResults()[0];

        while(sourceRows.advanceRow()) {
                assertEquals(true, targetRows.advanceRow());
                assertEquals(sourceRows.getLong(0), targetRows.getLong(0));
                assertEquals(sourceRows.getString(1), targetRows.getString(1));
                assertEquals(sourceRows.getLong(2), targetRows.getLong(2));
                assertEquals(sourceRows.getLong(3), targetRows.getLong(3));
        }
    }

    public void testPartitionedTableWithSelectJoin() throws Exception
    {
        final Client client = getClient();
        for (int i=0; i < 10; i++) {
                int j = i + 5;
            ClientResponse resp = client.callProcedure("SOURCE_P1.insert", i, Integer.toHexString(i), i, i);
            assertEquals(ClientResponse.SUCCESS, resp.getStatus());
            assertEquals(1, resp.getResults()[0].asScalarLong());

            resp = client.callProcedure("SOURCE_P2.insert", j, Integer.toHexString(j), j * 5, j * 5);
            assertEquals(ClientResponse.SUCCESS, resp.getStatus());
            assertEquals(1, resp.getResults()[0].asScalarLong());
        }

        // source_p1 contains 0..9
        // source_p2 contains 5..14

        ClientResponse resp = client.callProcedure("InsertIntoSelectWithJoin", 3, 6);
        assertEquals(ClientResponse.SUCCESS, resp.getStatus());
        assertEquals(2, resp.getResults()[0].asScalarLong());

        String selectSp1 = "select * from source_p1 where bi between 5 and 6";
        String selectSp2 = "select * from source_p2 where bi between 5 and 6";
        String selectTarget = "select * from target_p where bi between 5 and 6";

        resp = client.callProcedure("@AdHoc", selectTarget);
        assertEquals(ClientResponse.SUCCESS, resp.getStatus());
        VoltTable targetRows = resp.getResults()[0];

        resp = client.callProcedure("@AdHoc", selectSp1);
        assertEquals(ClientResponse.SUCCESS, resp.getStatus());
        VoltTable sp1Rows = resp.getResults()[0];

        resp = client.callProcedure("@AdHoc", selectSp2);
        assertEquals(ClientResponse.SUCCESS, resp.getStatus());
        VoltTable sp2Rows = resp.getResults()[0];

        while(targetRows.advanceRow()) {
                assertTrue(sp1Rows.advanceRow());
                assertTrue(sp2Rows.advanceRow());

                assertEquals(sp1Rows.getLong(0), targetRows.getLong(0));
                assertEquals(sp1Rows.getString(1), targetRows.getString(1));
                assertEquals(sp2Rows.getLong(2), targetRows.getLong(2));
                assertEquals(sp2Rows.getLong(3), targetRows.getLong(3));
        }
    }

//    public void testInsertDefaults() throws Exception
//    {
//      final Client client = getClient();
//
//      // populate p1
//      for (int i=0; i < 10; i++) {
//            ClientResponse resp = client.callProcedure("SOURCE_P1.insert", i, Integer.toHexString(i), i, i);
//            assertEquals(ClientResponse.SUCCESS, resp.getStatus());
//            assertEquals(1, resp.getResults()[0].asScalarLong());
//        }
//
//      String message = "no exception thrown";
//      try {
//              client.callProcedure("insert_p_source_p", 5);
//      } catch (ProcCallException ex) {
//              message = ex.getMessage();
//      }
//
//      assertTrue("Default values used by INSERT INTO SELECT should be unsupported", message.contains("asdf"));
//    }

//    public void testPartitionedTableSelfCopy() throws Exception
//    {
//        final Client client = getClient();
//        for (int i=0; i < 10; i++) {
//            ClientResponse resp = client.callProcedure("P1.insert", i, Integer.toHexString(i));
//            assertEquals(ClientResponse.SUCCESS, resp.getStatus());
//            assertEquals(1, resp.getResults()[0].asScalarLong());
//        }
//
//        ClientResponse resp;
//        String insertIntoSelect = "insert into P1 (b1, a2) select b1, 'z' || a2 from P1 where b1 >= 6";
//
//        /*/ enable for debug ...
//        resp = client.callProcedure("@Explain", insertIntoSelect);
//        VoltTable vt = resp.getResults()[0];
//        System.out.println("DEBUGGING:\n" + vt);
//        // ... enable for debug */
//
//        resp = client.callProcedure("@AdHoc", insertIntoSelect);
//        assertEquals(ClientResponse.SUCCESS, resp.getStatus());
//        assertEquals(4, resp.getResults()[0].asScalarLong());
//
//        resp = client.callProcedure("CountP1");
//        assertEquals(ClientResponse.SUCCESS, resp.getStatus());
//        assertEquals(14, resp.getResults()[0].asScalarLong());
//
//        resp = client.callProcedure("@AdHoc", "select count(*) from P1 where a2 >= 'z'");
//        assertEquals(ClientResponse.SUCCESS, resp.getStatus());
//        assertEquals(4, resp.getResults()[0].asScalarLong());
//    }
//
//    public void testPartitionedTable() throws Exception
//    {
//        final Client client = getClient();
//        for (int i=0; i < 10; i++) {
//            ClientResponse resp = client.callProcedure("P1.insert", i, Integer.toHexString(i));
//            assertEquals(ClientResponse.SUCCESS, resp.getStatus());
//            assertEquals(1, resp.getResults()[0].asScalarLong());
//        }
//
//        ClientResponse resp;
//        String insertIntoSelect = "insert into P2 (a1, a2) select b1, a2 from P1 where b1 >= 6";
//
//        /*/ enable for debug ...
//        resp = client.callProcedure("@Explain", insertIntoSelect);
//        VoltTable vt = resp.getResults()[0];
//        System.out.println("DEBUGGING:\n" + vt);
//        // ... enable for debug */
//
//        resp = client.callProcedure("@AdHoc", insertIntoSelect);
//        assertEquals(ClientResponse.SUCCESS, resp.getStatus());
//        assertEquals(4, resp.getResults()[0].asScalarLong());
//
//        resp = client.callProcedure("CountP1");
//        assertEquals(ClientResponse.SUCCESS, resp.getStatus());
//        assertEquals(10, resp.getResults()[0].asScalarLong());
//
//        resp = client.callProcedure("CountP2");
//        assertEquals(ClientResponse.SUCCESS, resp.getStatus());
//        assertEquals(4, resp.getResults()[0].asScalarLong());
//    }
//
//    public void testReplicatedTableSelfCopy() throws Exception
//    {
//        final Client client = getClient();
//        for (int i=0; i < 10; i++) {
//            ClientResponse resp = client.callProcedure("R1.insert", i, Integer.toHexString(i));
//            assertEquals(ClientResponse.SUCCESS, resp.getStatus());
//            assertEquals(1, resp.getResults()[0].asScalarLong());
//        }
//
//        ClientResponse resp;
//        String insertIntoSelect = "insert into R1 (b1, a2) select 100+b1, a2 from R1 where b1 >= 6";
//
//        /*/ enable for debug ...
//        resp = client.callProcedure("@Explain", insertIntoSelect);
//        VoltTable vt = resp.getResults()[0];
//        System.out.println("DEBUGGING:\n" + vt);
//        // ... enable for debug */
//
//        resp = client.callProcedure("@AdHoc", insertIntoSelect);
//        assertEquals(ClientResponse.SUCCESS, resp.getStatus());
//        assertEquals(4, resp.getResults()[0].asScalarLong());
//
//        resp = client.callProcedure("CountR1");
//        assertEquals(ClientResponse.SUCCESS, resp.getStatus());
//        assertEquals(14, resp.getResults()[0].asScalarLong());
//
//        resp = client.callProcedure("@AdHoc", "select count(*) from R1 where b1 >= 100");
//        assertEquals(ClientResponse.SUCCESS, resp.getStatus());
//        assertEquals(4, resp.getResults()[0].asScalarLong());
//    }
//
//    public void testReplicatedTable() throws Exception
//    {
//        final Client client = getClient();
//        for (int i=0; i < 10; i++) {
//            ClientResponse resp = client.callProcedure("R1.insert", i, Integer.toHexString(i));
//            assertEquals(ClientResponse.SUCCESS, resp.getStatus());
//            assertEquals(1, resp.getResults()[0].asScalarLong());
//        }
//
//        ClientResponse resp;
//        String insertIntoSelect = "insert into R2 (a1, a2) select 100+b1, a2 from R1 where b1 >= 6";
//
//        /*/ enable for debug ...
//        resp = client.callProcedure("@Explain", insertIntoSelect);
//        VoltTable vt = resp.getResults()[0];
//        System.out.println("DEBUGGING:\n" + vt);
//        // ... enable for debug */
//
//        resp = client.callProcedure("@AdHoc", insertIntoSelect);
//        assertEquals(ClientResponse.SUCCESS, resp.getStatus());
//        assertEquals(4, resp.getResults()[0].asScalarLong());
//
//        resp = client.callProcedure("CountR1");
//        assertEquals(ClientResponse.SUCCESS, resp.getStatus());
//        assertEquals(10, resp.getResults()[0].asScalarLong());
//
//        resp = client.callProcedure("CountR2");
//        assertEquals(ClientResponse.SUCCESS, resp.getStatus());
//        assertEquals(4, resp.getResults()[0].asScalarLong());
//
//        resp = client.callProcedure("@AdHoc", "select count(*) from R2 where a1 >= 100");
//        assertEquals(ClientResponse.SUCCESS, resp.getStatus());
//        assertEquals(4, resp.getResults()[0].asScalarLong());
//    }
//
//    public void testReplicatedFilteringIntoPartitionedTable() throws Exception
//    {
//        final Client client = getClient();
//        for (int i=0; i < 10; i++) {
//            ClientResponse resp = client.callProcedure("R1.insert", i, Integer.toHexString(i));
//            assertEquals(ClientResponse.SUCCESS, resp.getStatus());
//            assertEquals(1, resp.getResults()[0].asScalarLong());
//        }
//
//        ClientResponse resp;
//        String insertIntoSelect = "insert into P2 (a1, a2) select 100+b1, a2 from R1 where b1 >= 6";
//
//        /*/ enable for debug ...
//        resp = client.callProcedure("@Explain", insertIntoSelect);
//        VoltTable vt = resp.getResults()[0];
//        System.out.println("DEBUGGING:\n" + vt);
//        // ... enable for debug */
//
//        resp = client.callProcedure("@AdHoc", insertIntoSelect);
//        assertEquals(ClientResponse.SUCCESS, resp.getStatus());
//        assertEquals(4, resp.getResults()[0].asScalarLong());
//
//        resp = client.callProcedure("CountR1");
//        assertEquals(ClientResponse.SUCCESS, resp.getStatus());
//        assertEquals(10, resp.getResults()[0].asScalarLong());
//
//        resp = client.callProcedure("CountP2");
//        assertEquals(ClientResponse.SUCCESS, resp.getStatus());
//        assertEquals(4, resp.getResults()[0].asScalarLong());
//
//        resp = client.callProcedure("@AdHoc", "select count(*) from P2 where a1 >= 100");
//        assertEquals(ClientResponse.SUCCESS, resp.getStatus());
//        assertEquals(4, resp.getResults()[0].asScalarLong());
//    }
//
//    public void testValidatePartitionedTableSmallSelfCopyStoredProc() throws Exception
//    {
//        partitionedTableSelfCopyStoredProc(5);
//        partitionedTableSlowSelfCopyStoredProc(5);
//        final Client client = getClient();
//        ClientResponse resp;
//        resp = client.callProcedure("@AdHoc", "select A2, B1 from P1 order by 1, 2");
//        assertEquals(ClientResponse.SUCCESS, resp.getStatus());
//        VoltTable fastResult = resp.getResults()[0];
//        resp = client.callProcedure("@AdHoc", "select A2, B1 from P3 order by 1, 2");
//        assertEquals(ClientResponse.SUCCESS, resp.getStatus());
//        VoltTable slowResult = resp.getResults()[0];
//        while (fastResult.advanceRow()) {
//            assertTrue(slowResult.advanceRow());
//            assertEquals(slowResult.getString(0), fastResult.getString(0));
//            assertEquals(slowResult.getLong(1), fastResult.getLong(1));
//        }
//        assertFalse(slowResult.advanceRow());
//    }
//
//    public void testPartitionedTableSmallSelfCopyStoredProc() throws Exception
//    {
//        partitionedTableSelfCopyStoredProc(5);
//    }
//
//    public void testPartitionedTableSmallSlowSelfCopyStoredProc() throws Exception
//    {
//        partitionedTableSlowSelfCopyStoredProc(5);
//    }
//
//    public void testPartitionedTableMediumSelfCopyStoredProc() throws Exception
//    {
//        partitionedTableSelfCopyStoredProc(10);
//    }
//
//    public void testPartitionedTableMediumSlowSelfCopyStoredProc() throws Exception
//    {
//        partitionedTableSlowSelfCopyStoredProc(10);
//    }
//
//    public void testPartitionedTableLargeSelfCopyStoredProc() throws Exception
//    {
//        partitionedTableSelfCopyStoredProc(15);
//    }
//
//    public void UNtestPartitionedTableLargeSlowSelfCopyStoredProc() throws Exception
//    {
//        partitionedTableSlowSelfCopyStoredProc(15);
//    }
//
//    // Too slow to run normally
//    // public void UNtestPartitionedTableHugeSelfCopyStoredProc() throws Exception
//    //{
//    //    partitionedTableSelfCopyStoredProc(20);
//    //}
//    //
//    //public void UNtestPartitionedTableHugeSlowSelfCopyStoredProc() throws Exception
//    //{
//    //    partitionedTableSlowSelfCopyStoredProc(20);
//    //}
//
//    public void partitionedTableSelfCopyStoredProc(long iterations) throws Exception
//    {
//        ClientResponse resp;
//        final Client client = getClient();
//        for (int i=0; i < 10; i++) {
//            resp = client.callProcedure("P3.insert", i, Integer.toHexString(i));
//            assertEquals(ClientResponse.SUCCESS, resp.getStatus());
//            assertEquals(1, resp.getResults()[0].asScalarLong());
//        }
//
//        resp = client.callProcedure(MultiRoundInsertIntoSelect.class.getSimpleName(), iterations);
//    }
//
//    public void partitionedTableSlowSelfCopyStoredProc(long iterations) throws Exception
//    {
//        ClientResponse resp;
//        final Client client = getClient();
//        for (int i=0; i < 10; i++) {
//            resp = client.callProcedure("P1.insert", i, Integer.toHexString(i));
//            assertEquals(ClientResponse.SUCCESS, resp.getStatus());
//            assertEquals(1, resp.getResults()[0].asScalarLong());
//        }
//
//        resp = client.callProcedure(MultiRoundSelectThenInsert.class.getSimpleName(), iterations);
//    }
//
//    public void FIXMEtestFailingProjection() throws Exception {
//        final Client client = getClient();
//        for (int i=0; i < 10; i++) {
//            ClientResponse resp = client.callProcedure("R1.insert", i, Integer.toHexString(i));
//            assertEquals(ClientResponse.SUCCESS, resp.getStatus());
//            assertEquals(1, resp.getResults()[0].asScalarLong());
//        }
//
//        ClientResponse resp;
//        String insertIntoSelect = "insert into R2 (a1) select 100+b1 from R1 where b1 >= 6";
//
//        //* enable for debug ...
//        resp = client.callProcedure("@Explain", insertIntoSelect);
//        VoltTable vt = resp.getResults()[0];
//        System.out.println("DEBUGGING:\n" + vt);
//        // ... enable for debug */
//
//        insertIntoSelect = "insert into R2 (a1) values (9)";
//        //*/ enable for debug ...
//        resp = client.callProcedure("@Explain", insertIntoSelect);
//        vt = resp.getResults()[0];
//        System.out.println("DEBUGGING:\n" + vt);
//        // ... enable for debug */
//
//        insertIntoSelect = "insert into R2 (a2, a1) values ('x', 9)";
//
//        //*/ enable for debug ...
//        resp = client.callProcedure("@Explain", insertIntoSelect);
//        vt = resp.getResults()[0];
//        System.out.println("DEBUGGING:\n" + vt);
//        // ... enable for debug */
//
//        insertIntoSelect = "insert into R2 (a2, a1) select a2||'.', 100+b1 from R1 where b1 >= 6";
//
//        resp = client.callProcedure("@AdHoc", insertIntoSelect);
//        assertEquals(ClientResponse.SUCCESS, resp.getStatus());
//        assertEquals(4, resp.getResults()[0].asScalarLong());
//
//        resp = client.callProcedure("CountR1");
//        assertEquals(ClientResponse.SUCCESS, resp.getStatus());
//        assertEquals(10, resp.getResults()[0].asScalarLong());
//
//        resp = client.callProcedure("CountR2");
//        assertEquals(ClientResponse.SUCCESS, resp.getStatus());
//        assertEquals(4, resp.getResults()[0].asScalarLong());
//
//        resp = client.callProcedure("@AdHoc", "select count(*) from R2 where a1 >= 100");
//        assertEquals(ClientResponse.SUCCESS, resp.getStatus());
//        assertEquals(4, resp.getResults()[0].asScalarLong());
//
//    }


}
