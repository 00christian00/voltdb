-- CREATE INDEX
-- basic features

CREATE TABLE T1
(
    width INTEGER
,   length INTEGER
,   volume INTEGER
);

CREATE UNIQUE INDEX area
ON
T1
(
    width * length
);

CREATE TABLE T2
(
    width INTEGER
,   length INTEGER
,   area INTEGER NOT NULL
,   volume INTEGER
);

PARTITION TABLE T2
ON
COLUMN
    area
;

CREATE ASSUMEUNIQUE INDEX absVal
ON
T2
(
    ABS(area * 2)
,   ABS(volume / 2)
);

-- hash index

CREATE TABLE T3
(
    val INTEGER
,   str VARCHAR(30)
,   id INTEGER
);

CREATE UNIQUE INDEX abs_Hash_idx
ON
T3
(
    ABS(val)
);

CREATE UNIQUE INDEX nomeaninghashweirdidx
ON
T3
(
    ABS(id)
);

-- function in index definition

CREATE INDEX strMatch
ON
T3
(
    FIELD
    (
        str
    ,   'arbitrary'
    )
,   id
);


-- CREATE ROLE
-- basic

CREATE ROLE guest;

CREATE ROLE admin
WITH
    sysproc
,   adhoc
,   defaultproc;


-- CREATE PROCEDURE AS
-- as sql stmt

CREATE TABLE User
(
    age INTEGER
,   name VARCHAR(20)
);

CREATE PROCEDURE p1
ALLOW
    admin
AS
    SELECT COUNT(*)
         , name
    FROM User
    WHERE age = ?
    GROUP BY name;

CREATE PROCEDURE p2
ALLOW
    admin
AS
    INSERT INTO User
    VALUES (?, ?);

-- as source code

--CREATE PROCEDURE p3
--ALLOW
--    admin
--AS
--    ###
--    stmt = new SQLStmt('SELECT age, name FROM User WHERE age = ?')
--    transactOn = { int key ->
--                   voltQueueSQL(stmt,key)
--                   voltExecuteSQL(true)
--                 }
--    ### LANGUAGE GROOVY
--;


-- CREATE TABLE
-- test all supported SQL datatypes

CREATE TABLE T4
(
    C1 TINYINT DEFAULT 127 NOT NULL
,   C2 SMALLINT DEFAULT 32767 NOT NULL
,   C3 INTEGER DEFAULT 2147483647 NOT NULL
,   C4 BIGINT NOT NULL
,   C5 FLOAT NOT NULL
,   C6 DECIMAL NOT NULL
,   C7 VARCHAR(32) NOT NULL
,   C8 VARBINARY(32) NOT NULL
,   C9 TIMESTAMP DEFAULT NOW NOT NULL
,   C10 TIMESTAMP DEFAULT CURRENT_TIMESTAMP
,   POINT1 GEOGRAPHY_POINT NOT NULL
,   POINT2 GEOGRAPHY_POINT NOT NULL
,   REGION1 GEOGRAPHY NOT NULL
,   REGION2 GEOGRAPHY(777) NOT NULL

,   PRIMARY KEY
    (
        C1
    ,   C9
    )
);

-- test maximum varchar size

CREATE TABLE T5
(
    C VARCHAR(1048576 BYTES)
);

CREATE TABLE T6
(
    C VARCHAR(262144)
);

-- test maximum varbinary size

CREATE TABLE T7
(
    C VARBINARY(1048576)
);

-- test maximum limit partition rows

CREATE TABLE T8
(
    C INTEGER
,   LIMIT PARTITION ROWS 2147483647
);

-- column constraint

CREATE TABLE T9
(
    C1 INTEGER PRIMARY KEY NOT NULL
,   C2 SMALLINT UNIQUE NOT NULL
);

CREATE TABLE T10
(
    C INTEGER DEFAULT 123 NOT NULL
,   CONSTRAINT con UNIQUE
    (
        C
    )
);

CREATE TABLE T11
(
    C INTEGER DEFAULT 123 NOT NULL
,   CONSTRAINT pk1 PRIMARY KEY
    (
        C
    )
);

CREATE TABLE T12
(
    C1 INTEGER NOT NULL
,   C2 INTEGER DEFAULT 123 NOT NULL
,   CONSTRAINT au ASSUMEUNIQUE
    (
        C2
    )
);
PARTITION TABLE T12 ON COLUMN C1;

-- table constraints

CREATE TABLE T13
(
    C INTEGER
,   CONSTRAINT pk2 PRIMARY KEY
    (
        C
    )
);

CREATE TABLE T14
(
    C INTEGER
,   CONSTRAINT uni1 UNIQUE
    (
        C
    )
);

CREATE TABLE T15
(
    C INTEGER
,   C2 TINYINT NOT NULL
,   CONSTRAINT assumeuni ASSUMEUNIQUE
    (
        C
    )
);
PARTITION TABLE T15 ON COLUMN C2;

CREATE TABLE T16
(
    C INTEGER
,   CONSTRAINT lpr1 LIMIT PARTITION ROWS 1
);

-- table constraint without keyword

CREATE TABLE T17
(
    C INTEGER
,   PRIMARY KEY
    (
        C
    )
);

CREATE TABLE T18
(
    C INTEGER
,   UNIQUE
    (
        C
    )
);

CREATE TABLE T19
(
    C INTEGER
,   C2 TINYINT NOT NULL
,   ASSUMEUNIQUE
    (
        C
    )
);
PARTITION TABLE T19 ON COLUMN C2;

CREATE TABLE T20
(
    C INTEGER
,   LIMIT PARTITION ROWS 1
);


-- both column and table constraints

CREATE TABLE T21
(
    C1 TINYINT DEFAULT 127 NOT NULL
,   C2 SMALLINT DEFAULT 32767 NOT NULL
,   C3 INTEGER DEFAULT 2147483647 NOT NULL
,   C4 BIGINT NOT NULL
,   C5 FLOAT NOT NULL
,   C6 DECIMAL ASSUMEUNIQUE NOT NULL
,   C7 VARCHAR(32) NOT NULL
,   C8 VARBINARY(32) NOT NULL
,   C9 TIMESTAMP DEFAULT NOW NOT NULL
,   C10 TIMESTAMP DEFAULT CURRENT_TIMESTAMP
,   ASSUMEUNIQUE
    (
        C1
    ,   C9
    )
);
PARTITION TABLE T21 ON COLUMN C3;

CREATE TABLE T22
(
    C1 TINYINT DEFAULT 127 NOT NULL UNIQUE
,   C2 SMALLINT DEFAULT 32767 NOT NULL
,   C3 INTEGER DEFAULT 2147483647 NOT NULL
,   C4 BIGINT NOT NULL
,   C5 FLOAT NOT NULL
,   C6 DECIMAL UNIQUE NOT NULL
,   C7 VARCHAR(32) NOT NULL
,   C8 VARBINARY(32) NOT NULL
,   C9 TIMESTAMP DEFAULT NOW NOT NULL
,   C10 TIMESTAMP DEFAULT CURRENT_TIMESTAMP
,   UNIQUE
    (
        C1
    ,   C9
    )
);

CREATE TABLE T23
(
    C1 INTEGER NOT NULL
,   C2 SMALLINT UNIQUE
,   C3 VARCHAR(32) NOT NULL
,   C4 TINYINT NOT NULL
,   C5 TIMESTAMP NOT NULL
,   C6 BIGINT NOT NULL
,   C7 FLOAT NOT NULL
,   C8 DECIMAL NOT NULL
,   C9 INTEGER
,   CONSTRAINT hash_pk PRIMARY KEY
    (
        C1
    ,   C5
    )
,   CONSTRAINT uni2 UNIQUE
    (
        C1
    ,   C7
    ),
    CONSTRAINT lpr2 LIMIT PARTITION ROWS 123
);


-- CREATE VIEW
-- basic

CREATE TABLE T24
(
    C1 INTEGER
,   C2 INTEGER
);

CREATE VIEW VT1
(
    C1
,   C2
,   TOTAL
)
AS
    SELECT C1
        ,  C2
        ,  COUNT(*)
    FROM T24
    GROUP BY C1
          ,  C2
;

CREATE VIEW VT2
(
    C1
,   C2
,   TOTAL
,   SUMUP
)
AS
    SELECT C1
        ,  C2
        ,  COUNT(*)
        ,  SUM(C2)
    AS
        newTble
    FROM T24
    WHERE T24.C1 < 1000
    GROUP BY C1
          ,  C2
;


-- EXPORT TABLE
-- basic

CREATE TABLE T25
(
    id INTEGER NOT NULL
);
EXPORT TABLE T25;

CREATE TABLE T25S
(
    id INTEGER NOT NULL
);
EXPORT TABLE T25S TO STREAM imagine;


-- IMPORT CLASS
-- basic

-- IMPORT CLASS org.voltdb_testprocs.fullddlfeatures.NoMeaningClass;
-- CREATE PROCEDURE FROM CLASS org.voltdb_testprocs.fullddlfeatures.testImportProc;


-- CREATE PROCEDURE ... PARTITION ON ...
-- basic

CREATE TABLE T26
(
    age BIGINT NOT NULL
,   gender TINYINT
);

PARTITION TABLE T26 ON COLUMN age;

CREATE PROCEDURE p4
ALLOW
    admin
PARTITION ON
    TABLE
        T26
    COLUMN
        age
    PARAMETER
        1
AS
    SELECT COUNT(*)
    FROM T26
    WHERE gender = ? AND age = ?;

-- This would not have worked before the PARTITION clause existed,
-- e.g. a separate PARTITION PROCEDURE statement would be too late.
CREATE PROCEDURE p4a
ALLOW
    admin
PARTITION ON
    TABLE
        T26
    COLUMN
        age
    PARAMETER
        0
AS
    SELECT *
    FROM T26
    WHERE age = ? UNION ALL (
        SELECT *
        FROM T26
        WHERE age = ?);

CREATE PROCEDURE
ALLOW
    admin
PARTITION ON
    TABLE
        T26
    COLUMN
        age
FROM CLASS
    org.voltdb_testprocs.fullddlfeatures.testCreateProcFromClassProc
;


-- PARTITION TABLE
-- basic

CREATE TABLE T27
(
    C INTEGER NOT NULL
);

PARTITION TABLE T27 ON COLUMN C;


-- CREATE PROCEDURE
-- Verify that the sqlcmd parsing survives two consecutive create procedures

CREATE TABLE T28
(
    C1 BIGINT
,   C2 BIGINT
);

CREATE TABLE T29
(
    C1 INTEGER
,   LIMIT PARTITION ROWS 5 EXECUTE (DELETE FROM T29 WHERE C1 > 0)
);

CREATE TABLE T30
(
    C1 INTEGER
,   CONSTRAINT lpr5exec
    LIMIT PARTITION ROWS 5 EXECUTE (DELETE FROM T30 WHERE C1 > 0)
);

CREATE PROCEDURE FOO1 AS SELECT * FROM T28;
CREATE PROCEDURE FOO2 AS SELECT COUNT(*) FROM T28;

-- Verify that consecutive procedure/view statements survive sqlcmd parsing
CREATE PROCEDURE FOO3 AS SELECT * FROM T28;

CREATE VIEW VT3
(
    C1
,   C2
,   TOTAL
)
AS
    SELECT C1
        ,  C2
        ,  COUNT(*)
    FROM T28
    GROUP BY C1
          ,  C2
;

CREATE PROCEDURE FOO4 AS SELECT * FROM VT3;

-- Verify that create procedure with INSERT INTO SELECT
-- survives sqlcmd
CREATE PROCEDURE INS_T1_SELECT_T1 AS
    INSERT INTO T1 SELECT * FROM T1;

CREATE PROCEDURE INS_T1_COLS_SELECT_T1 AS
    INSERT INTO T1 (WIDTH, LENGTH, VOLUME)
        SELECT WIDTH, LENGTH, VOLUME FROM T1;

CREATE PROCEDURE UPS_T4_SELECT_T4 AS
    INSERT INTO T4 SELECT * FROM T4 ORDER BY C1, C9;

CREATE PROCEDURE UPS_T4_COLS_SELECT_T4 AS
    INSERT INTO T4 (C9, C1, C4, C5, C8, C6, C7, POINT1, POINT2, REGION1, REGION2)
        SELECT C9, C1, C4, C5, C8, C6, C7, POINT1, POINT2, REGION1, REGION2 FROM T4;


-- DROP VIEWS
CREATE TABLE T30A (
   C1 VARCHAR(15),
   C2 VARCHAR(15),
   C3 VARCHAR(15) NOT NULL,
   PRIMARY KEY (C3)
);

CREATE VIEW VT30A
(
    C1
,   C2
,   TOTAL
)
AS
    SELECT C1
        ,  C2
        ,  COUNT(*)
    FROM T30A
    GROUP BY C1
          ,  C2
;

CREATE VIEW VT30B
(
    C2
,   C1
,   TOTAL
)
AS
    SELECT C2
        ,  C1
        ,  COUNT(*)
    FROM T30A
    GROUP BY C2
          ,  C1
;

DROP VIEW VT000 IF EXISTS;
DROP VIEW VT30A IF EXISTS;
DROP VIEW VT30B;

-- DROP INDEX

CREATE TABLE T31 (
    C1 INTEGER
,   C2 INTEGER
,   C3 INTEGER
,   PRIMARY KEY (C3)
);

CREATE UNIQUE INDEX abs_T31A_idx
ON
T31
(
    ABS(C1*C3)
);

DROP INDEX abs_T31A_idx;
DROP INDEX abs_T000_idx IF EXISTS;

-- DROP PROCEDURE
CREATE TABLE T32 (
   C1 VARCHAR(15),
   C2 VARCHAR(15),
   C3 VARCHAR(15) NOT NULL,
   PRIMARY KEY (C3)
);

PARTITION TABLE T32 ON COLUMN C3;

CREATE PROCEDURE T32A PARTITION ON TABLE T32 COLUMN C3 AS SELECT * FROM T32 WHERE C3 = ?;
CREATE PROCEDURE T32B PARTITION ON TABLE T32 COLUMN C3 AS SELECT COUNT(*) FROM T32 WHERE C3 = ?;

DROP PROCEDURE T32A;
DROP PROCEDURE T32B;

-- DROP TABLE
-- basic
CREATE TABLE T33 (
   C1 VARCHAR(15),
);
DROP TABLE T33;
-- cascade and if exists
CREATE TABLE T34 (
   C1 INTEGER,
   C2 INTEGER,
   C3 INTEGER NOT NULL,
   PRIMARY KEY (C3)
);
CREATE VIEW VT34A
(
    C1
,   C2
,   TOTAL
)
AS
    SELECT C1
        ,  C2
        ,  COUNT(*)
    FROM T34
    GROUP BY C1
          ,  C2
;
CREATE UNIQUE INDEX abs_T34A_idx
ON
T34
(
    ABS(C1*C3)
);
DROP TABLE T34 IF EXISTS CASCADE;

-- ALTER TABLE DROP CONSTRAINT
CREATE TABLE T35
(
    C1 INTEGER PRIMARY KEY NOT NULL
,   C2 SMALLINT UNIQUE NOT NULL
);
ALTER TABLE T35 DROP PRIMARY KEY;

CREATE TABLE T35A (
   C1 INTEGER
,  LIMIT PARTITION ROWS 1
);
ALTER TABLE T35A DROP LIMIT PARTITION ROWS;

CREATE TABLE T36
(
    C INTEGER
,   CONSTRAINT pk36A PRIMARY KEY
    (
        C
    )
);
ALTER TABLE T36 DROP CONSTRAINT pk36A;

CREATE TABLE T37
(
    C INTEGER
,   CONSTRAINT con37A UNIQUE
    (
        C
    )
);
ALTER TABLE T37 DROP CONSTRAINT con37A;

CREATE TABLE T38
(
    C INTEGER
,   CONSTRAINT con38A ASSUMEUNIQUE
    (
        C
    )
);
ALTER TABLE T38 DROP CONSTRAINT con38A;

CREATE TABLE T39
(
    C INTEGER
,   CONSTRAINT lpr39A LIMIT PARTITION ROWS 1
);
-- Once ENG-7869 is fixed, switch this back to drop the constraint by name
-- (or, better, should test both, with a table 'T39A'):
ALTER TABLE T39 DROP LIMIT PARTITION ROWS;
--ALTER TABLE T39 DROP CONSTRAINT lpr39A;

-- ALTER TABLE ADD CONSTRAINT
CREATE TABLE T40
(
    C1 INTEGER DEFAULT 123 NOT NULL
,   C2 INTEGER
);
ALTER TABLE T40 ADD CONSTRAINT con40A UNIQUE ( C1, C2 );

CREATE TABLE T41
(
    C1 INTEGER
);
-- ENG-7321 - bug with PRIMARY KEY and verification of generated DDL
-- ALTER TABLE T41 ADD PRIMARY KEY ( C1 );
-- ALTER TABLE T41 ADD CONSTRAINT pk41 PRIMARY KEY ( C1 );

CREATE TABLE T42
(
    C1 INTEGER
,   C2 INTEGER
);
ALTER TABLE T42 ADD CONSTRAINT con42A ASSUMEUNIQUE ( C1, C2 );

CREATE TABLE T42A
(
    C1 INTEGER DEFAULT 123 NOT NULL
,   C2 INTEGER
,   C3 INTEGER
);
ALTER TABLE T42A ADD CONSTRAINT con42AA ASSUMEUNIQUE (
    ABS(C1*C2)
,   C2+C3
);

CREATE TABLE T43
(
    C1 INTEGER DEFAULT 123 NOT NULL
);
ALTER TABLE T43 ADD CONSTRAINT con43A LIMIT PARTITION ROWS 1;

-- ALTER TABLE ADD COLUMN
CREATE TABLE T44
(
    C1 INTEGER DEFAULT 123 NOT NULL
);
ALTER TABLE T44 ADD COLUMN C2 VARCHAR(1);

CREATE TABLE T45
(
    C1 INTEGER DEFAULT 123 NOT NULL
);
ALTER TABLE T45 ADD COLUMN C2 INTEGER DEFAULT 1 NOT NULL;

CREATE TABLE T46
(
    C1 INTEGER DEFAULT 123 NOT NULL
,   C3 INTEGER
);
ALTER TABLE T46 ADD COLUMN C2 INTEGER DEFAULT 1 NOT NULL ASSUMEUNIQUE BEFORE C3;

CREATE TABLE T47
(
    C1 INTEGER DEFAULT 123 NOT NULL
,   C3 INTEGER
);
ALTER TABLE T47 ADD COLUMN C2 INTEGER DEFAULT 1 NOT NULL UNIQUE BEFORE C3;

CREATE TABLE T48
(
    C1 INTEGER DEFAULT 123 NOT NULL
,   C3 INTEGER
);
-- ENG-7321 - bug with PRIMARY KEY and verification of generated DDL
-- ALTER TABLE T48 ADD COLUMN C2 INTEGER DEFAULT 1 NOT NULL PRIMARY KEY BEFORE C3;

-- ALTER TABLE DROP COLUMN
CREATE TABLE T49
(
    C1 INTEGER DEFAULT 123 NOT NULL
,   C2 INTEGER
);
ALTER TABLE T49 DROP COLUMN C1;

CREATE TABLE T50
(
    C1 INTEGER DEFAULT 123 NOT NULL
,   C2 INTEGER
,   C3 INTEGER
,   CONSTRAINT pk391 PRIMARY KEY
    (
        C1
    )
,   CONSTRAINT con391 UNIQUE
    (
        C2
    )
);
CREATE VIEW VT50A
(
    C1
,   C2
,   TOTAL
)
AS
    SELECT C1
        ,  C2
        ,  COUNT(*)
    FROM T50
    GROUP BY C1
          ,  C2
;
CREATE UNIQUE INDEX abs_T50A_idx
ON
T50
(
    ABS(C1*C2)
);
ALTER TABLE T50 DROP COLUMN C2 CASCADE;

-- ALTER TABLE ALTER COLUMN
CREATE TABLE T51
(
    C1 INTEGER NOT NULL
,   C2 INTEGER DEFAULT 123 NOT NULL
);
ALTER TABLE T51 ALTER COLUMN C1 SET DEFAULT NULL;
ALTER TABLE T51 ALTER COLUMN C1 SET NULL;


CREATE TABLE T52
(
    C1 INTEGER DEFAULT 123 NOT NULL
,   C2 INTEGER NOT NULL
);
ALTER TABLE T52 ALTER COLUMN C2 SET NULL;


CREATE TABLE T53
(
    C1 INTEGER DEFAULT 123 NOT NULL
,   C2 INTEGER NOT NULL
);
ALTER TABLE T53 ALTER COLUMN C1 VARCHAR(2);
ALTER TABLE T53 ALTER COLUMN C2 VARCHAR(2);

CREATE TABLE T54
(
    C1 VARCHAR(2) NOT NULL
,   C2 INTEGER DEFAULT NULL
);
CREATE UNIQUE INDEX abs_T54A_idx
ON
T54
(
    C2
);
ALTER TABLE T54 ALTER COLUMN C2 VARCHAR(2) CASCADE;

CREATE TABLE T55
(
    STR VARCHAR(30)
,   TS TIMESTAMP
,   BIG BIGINT
);

-- DR TABLE
CREATE TABLE T56
(
    C1 VARCHAR(2) NOT NULL
,   C2 INTEGER DEFAULT NULL
);
DR TABLE T56;

CREATE TABLE T57
(
    C1 VARCHAR(2) NOT NULL
,   C2 INTEGER NOT NULL
);
PARTITION TABLE T57 ON COLUMN C2;
DR TABLE T57;

-- DROP DR TABLE

CREATE TABLE T58
(
    C1 INTEGER NOT NULL
,   C2 INTEGER NOT NULL
);
DR TABLE T58;

CREATE TABLE T59
(
    C1 INTEGER NOT NULL
,   C2 INTEGER NOT NULL
);
PARTITION TABLE T59 ON COLUMN C2;
DR TABLE T59;

-- DISABLE DR TABLE
CREATE TABLE T60
(
   C1 INTEGER,
   C2 INTEGER,
   C3 INTEGER NOT NULL,
   PRIMARY KEY (C3)
);
DR TABLE T60;

CREATE TABLE T61
(
   C1 INTEGER,
   C2 INTEGER,
   C3 INTEGER NOT NULL,
   PRIMARY KEY (C3)
);
PARTITION TABLE T61 ON COLUMN C3;
DR TABLE T61;

DROP TABLE T58;
DROP TABLE T59;

DR TABLE T60 DISABLE;
DR TABLE T61 DISABLE;

-- Partial index
CREATE TABLE T62
(
   C1 INTEGER,
   C2 INTEGER,
   C3 INTEGER,
   pk INTEGER NOT NULL,
   PRIMARY KEY (pk)
);
CREATE INDEX partial_idx_1 ON T62 (C2) WHERE C1 > 3;
CREATE INDEX partial_idx_2 ON T62 (C1) WHERE C1 IS NOT NULL;
CREATE INDEX partial_idx_3 ON T62 (C2) WHERE C1 IS NOT NULL;
CREATE INDEX partial_idx_4 ON T62 (C1) WHERE ABS(C1) > 5;
CREATE INDEX partial_idx_5 ON T62 (C1,C2) WHERE ABS(C3) = 5;
CREATE INDEX partial_idx_6 ON T62 (C1) WHERE C2 > 5 and C2 < 100;

-- These statements were added when use of some Volt-specific functions or ||
-- or NULL in indexed expressions was discovered to be mishandled (ENG-7792).
-- They showed that views and procs did NOT share these problems,
-- but let's make sure things stay that way.
CREATE PROCEDURE PROC_USES_CONCAT AS SELECT CONCAT(SUBSTRING(STR,4), SUBSTRING(STR,1,3)) FROM T55;
CREATE PROCEDURE PROC_USES_BARBARCONCAT AS SELECT SUBSTRING(STR,5) || SUBSTRING(STR,1,3) FROM T55;
CREATE PROCEDURE PROC_USES_NULL AS SELECT DECODE(STR, NULL, 'NULLISH', STR) FROM T55;
CREATE PROCEDURE PROC_USES_TO_TIMESTAMP AS SELECT TO_TIMESTAMP(SECOND, BIG) FROM T55;
CREATE VIEW V55_USES_NULL AS
    SELECT   DECODE(STR, NULL, 'NULLISH', STR), COUNT(*), SUM(BIG)
    FROM T55
    GROUP BY DECODE(STR, NULL, 'NULLISH', STR);
CREATE VIEW V55_USES_TO_TIMESTAMP AS
    SELECT   TO_TIMESTAMP(SECOND, BIG), COUNT(*), COUNT(STR)
    FROM T55
    GROUP BY TO_TIMESTAMP(SECOND, BIG);
CREATE VIEW V55_USES_CONCAT AS
    SELECT   CONCAT(SUBSTRING(STR,4), SUBSTRING(STR,1,3)), COUNT(*), COUNT(STR)
    FROM T55
    GROUP BY CONCAT(SUBSTRING(STR,4), SUBSTRING(STR,1,3));
CREATE VIEW V55_USES_BARBARCONCAT AS
    SELECT   SUBSTRING(STR,5) || SUBSTRING(STR,1,3), COUNT(*), COUNT(STR)
    FROM T55
    GROUP BY SUBSTRING(STR,5) || SUBSTRING(STR,1,3);

-- ENG-7792 Make sure that concat, ||, and volt-specific SQL functions survive DDL roundtripping.
-- This especially exercises FunctionForVoltDB.getSQL().
CREATE INDEX ENG7792_INDEX_USES_CONCAT ON T55 (CONCAT(SUBSTRING(STR,4), SUBSTRING(STR,1,3)));
CREATE INDEX ENG7792_INDEX_USES_BARBARCONCAT ON T55 (SUBSTRING(STR,5) || SUBSTRING(STR,1,3));
-- ENG-7840 Make sure that a NULL constant survives DDL roundtripping.
CREATE INDEX ENG7840_INDEX_USES_NULL ON T55 (DECODE(STR, NULL, 'NULLISH', STR));
CREATE INDEX INDEX_USES_TRUNCATE_AND_TO_TIMESTAMP ON T55 (TRUNCATE(DAY, TO_TIMESTAMP(SECOND, BIG)));
CREATE INDEX INDEX_USES_JSON_SET_FIELD_ON_FIELD ON T55 (SET_FIELD(FIELD(STR, 'A'), 'B', ''));
CREATE INDEX INDEX_USES_JSON_ARRAY_OPS ON T55 (ARRAY_ELEMENT(STR, ARRAY_LENGTH(STR)-1));

CREATE INDEX ENG_8168_INDEX_USES_ABS ON T55 (ABS(BIG));
CREATE INDEX ENG_8168_INDEX_USES_ARRAY_ELEMENT ON T55 (ARRAY_ELEMENT(STR, BIG));
CREATE INDEX ENG_8168_INDEX_USES_ARRAY_LENGTH ON T55 (ARRAY_LENGTH(STR));
CREATE INDEX ENG_8168_INDEX_USES_BIN ON T55 (BIN(BIG));
CREATE INDEX ENG_8168_INDEX_USES_BITAND ON T55 (BITAND(BIG, BIG));
CREATE INDEX ENG_8168_INDEX_USES_BITNOT ON T55 (BITNOT(BIG));
CREATE INDEX ENG_8168_INDEX_USES_BITOR ON T55 (BITOR(BIG, BIG));
CREATE INDEX ENG_8168_INDEX_USES_BIT_SHIFT_LEFT ON T55 (BIT_SHIFT_LEFT(BIG, BIG));
CREATE INDEX ENG_8168_INDEX_USES_BIT_SHIFT_RIGHT ON T55 (BIT_SHIFT_RIGHT(BIG, BIG));
CREATE INDEX ENG_8168_INDEX_USES_BITXOR ON T55 (BITXOR(BIG, BIG));
CREATE INDEX ENG_8168_INDEX_USES_CAST_VARCHAR ON T55 (CAST(BIG AS VARCHAR));
CREATE INDEX ENG_8168_INDEX_USES_CAST_BIGINT ON T55 (CAST(STR AS BIGINT));
CREATE INDEX ENG_8168_INDEX_USES_CEILING ON T55 (CEILING(BIG / 3.3));
CREATE INDEX ENG_8168_INDEX_USES_CHAR ON T55 (CHAR(BIG));
CREATE INDEX ENG_8168_INDEX_USES_CHAR_LENGTH ON T55 (CHAR_LENGTH(STR));
CREATE INDEX ENG_8168_INDEX_USES_COALESCE ON T55 (COALESCE(STR, 'UNKNOWN'));
CREATE INDEX ENG_8168_INDEX_USES_CONCAT ON T55 (CONCAT(STR, STR));
CREATE INDEX ENG_8168_INDEX_USES_DATEADD_DAY ON T55 (DATEADD(DAY, big, ts));
CREATE INDEX ENG_8168_INDEX_USES_DATEADD_HOUR ON T55 (DATEADD(HOUR, big, ts));
CREATE INDEX ENG_8168_INDEX_USES_DATEADD_MICROS ON T55 (DATEADD(MICROS, big, ts));
CREATE INDEX ENG_8168_INDEX_USES_DATEADD_MICROSECOND ON T55 (DATEADD(MICROSECOND, big, ts));
CREATE INDEX ENG_8168_INDEX_USES_DATEADD_MILLIS ON T55 (DATEADD(MILLIS, big, ts));
CREATE INDEX ENG_8168_INDEX_USES_DATEADD_MILLISECOND ON T55 (DATEADD(MILLISECOND, big, ts));
CREATE INDEX ENG_8168_INDEX_USES_DATEADD_MINUTE ON T55 (DATEADD(MINUTE, big, ts));
CREATE INDEX ENG_8168_INDEX_USES_DATEADD_MONTH ON T55 (DATEADD(MONTH, big, ts));
CREATE INDEX ENG_8168_INDEX_USES_DATEADD_QUARTER ON T55 (DATEADD(QUARTER, big, ts));
CREATE INDEX ENG_8168_INDEX_USES_DATEADD_SECOND ON T55 (DATEADD(SECOND, big, ts));
CREATE INDEX ENG_8168_INDEX_USES_DAY ON T55 (DAY(TS));
CREATE INDEX ENG_8168_INDEX_USES_DAYOFMONTH ON T55 (DAYOFMONTH(TS));
CREATE INDEX ENG_8168_INDEX_USES_DAYOFWEEK ON T55 (DAYOFWEEK(TS));
CREATE INDEX ENG_8168_INDEX_USES_DAYOFYEAR ON T55 (DAYOFYEAR(TS));
CREATE INDEX ENG_8168_INDEX_USES_DECODE ON T55 (DECODE(STR, 'X', 'Y', 'Z'));
CREATE INDEX ENG_8168_INDEX_USES_EXP ON T55 (EXP(BIG));
CREATE INDEX ENG_8168_INDEX_USES_EXTRACT_YEAR         ON T55 (EXTRACT(YEAR, TS));
CREATE INDEX ENG_8168_INDEX_USES_EXTRACT_QUARTER      ON T55 (EXTRACT(QUARTER FROM TS));
CREATE INDEX ENG_8168_INDEX_USES_EXTRACT_MONTH        ON T55 (EXTRACT(MONTH, TS));
CREATE INDEX ENG_8168_INDEX_USES_EXTRACT_DAY          ON T55 (EXTRACT(DAY, TS));
CREATE INDEX ENG_8168_INDEX_USES_EXTRACT_DAY_OF_MONTH ON T55 (EXTRACT(DAY_OF_MONTH FROM TS));
CREATE INDEX ENG_8168_INDEX_USES_EXTRACT_DAY_OF_WEEK  ON T55 (EXTRACT(DAY_OF_WEEK, TS));
CREATE INDEX ENG_8168_INDEX_USES_EXTRACT_DAY_OF_YEAR  ON T55 (EXTRACT(DAY_OF_YEAR FROM TS));
CREATE INDEX ENG_8168_INDEX_USES_EXTRACT_WEEK         ON T55 (EXTRACT(WEEK, TS));
CREATE INDEX ENG_8168_INDEX_USES_EXTRACT_WEEK_OF_YEAR ON T55 (EXTRACT(WEEK_OF_YEAR FROM TS));
CREATE INDEX ENG_8168_INDEX_USES_EXTRACT_WEEKDAY      ON T55 (EXTRACT(WEEKDAY, TS));
CREATE INDEX ENG_8168_INDEX_USES_EXTRACT_HOUR         ON T55 (EXTRACT(HOUR FROM TS));
CREATE INDEX ENG_8168_INDEX_USES_EXTRACT_MINUTE       ON T55 (EXTRACT(MINUTE, TS));
CREATE INDEX ENG_8168_INDEX_USES_EXTRACT_SECOND ON T55 (EXTRACT(SECOND FROM TS));
CREATE INDEX ENG_8168_INDEX_USES_FIELD ON T55 (FIELD(STR, STR));
CREATE INDEX ENG_8168_INDEX_USES_FLOOR ON T55 (FLOOR(BIG / 3.3));
CREATE INDEX ENG_8168_INDEX_USES_FORMAT_CURRENCY ON T55 (FORMAT_CURRENCY(CAST(BIG AS DECIMAL), BIG));
CREATE INDEX ENG_8168_INDEX_USES_FROM_UNIXTIME ON T55 (FROM_UNIXTIME(BIG));
CREATE INDEX ENG_8168_INDEX_USES_HEX ON T55 (HEX(BIG));
CREATE INDEX ENG_8168_INDEX_USES_HOUR ON T55 (HOUR(TS));
CREATE INDEX ENG_8168_INDEX_USES_LEFT ON T55 (LEFT(STR, BIG));
CREATE INDEX ENG_8168_INDEX_USES_LOWER ON T55 (LOWER(STR));
CREATE INDEX ENG_8168_INDEX_USES_MINUTE ON T55 (MINUTE(TS));
CREATE INDEX ENG_8168_INDEX_USES_MONTH ON T55 (MONTH(TS));
CREATE INDEX ENG_8168_INDEX_USES_OCTET_LENGTH ON T55 (OCTET_LENGTH(STR));
CREATE INDEX ENG_8168_INDEX_USES_OVERLAY ON T55 (OVERLAY(STR PLACING 'XXX' FROM 0 FOR 3 ));
CREATE INDEX ENG_8168_INDEX_USES_OVERLAYC ON T55 (OVERLAY(STR, 'X X', 0, 3 ));
CREATE INDEX ENG_8168_INDEX_USES_OVERLAY2 ON T55 (OVERLAY(STR PLACING 'YYY' FROM BIG ));
CREATE INDEX ENG_8168_INDEX_USES_OVERLAY2C ON T55 (OVERLAY(STR, 'Y Y', BIG ));
CREATE INDEX ENG_8168_INDEX_USES_PI ON T55 (PI());
CREATE INDEX ENG_8168_INDEX_USES_POSITION ON T55 (POSITION(STR IN 'ABCDE'));
CREATE INDEX ENG_8168_INDEX_USES_POSITIONC ON T55 (POSITION(STR, 'ABC'));
CREATE INDEX ENG_8168_INDEX_USES_POWER ON T55 (POWER(BIG, BIG));
CREATE INDEX ENG_8168_INDEX_USES_REGEXP_POSITION ON T55 (REGEXP_POSITION(str, str));
CREATE INDEX ENG_8168_INDEX_USES_REGEXP_POSITION_C ON T55 (REGEXP_POSITION(str, str, 'c'));
CREATE INDEX ENG_8168_INDEX_USES_REGEXP_POSITION_I ON T55 (REGEXP_POSITION(str, str, 'i'));
CREATE INDEX ENG_8168_INDEX_USES_QUARTER ON T55 (QUARTER(TS));
CREATE INDEX ENG_8168_INDEX_USES_REPEAT ON T55 (REPEAT(STR, BIG));
CREATE INDEX ENG_8168_INDEX_USES_REPLACE ON T55 (REPLACE(STR, 'XXX', 'ZZZ'));
CREATE INDEX ENG_8168_INDEX_USES_RIGHT ON T55 (RIGHT(STR, BIG));
CREATE INDEX ENG_8168_INDEX_USES_SECOND ON T55 (SECOND(TS));
CREATE INDEX ENG_8168_INDEX_USES_SET_FIELD ON T55 (SET_FIELD(STR, 'ABC', '0'));
CREATE INDEX ENG_8168_INDEX_USES_SINCE_EPOCH_SECOND ON T55 (SINCE_EPOCH(SECOND, TS));
CREATE INDEX ENG_8168_INDEX_USES_SINCE_EPOCH_MILLISECOND ON T55 (1000 + SINCE_EPOCH(MILLISECOND, TS));
CREATE INDEX ENG_8168_INDEX_USES_SINCE_EPOCH_MICROSECOND ON T55 (1000 + SINCE_EPOCH(MICROSECOND, TS));
CREATE INDEX ENG_8168_INDEX_USES_SINCE_EPOCH_MILLIS ON T55 (SINCE_EPOCH(MILLIS, TS));
CREATE INDEX ENG_8168_INDEX_USES_SINCE_EPOCH_MICROS ON T55 (SINCE_EPOCH(MICROS, TS));
CREATE INDEX ENG_8168_INDEX_USES_SPACE ON T55 (SPACE(BIG));
CREATE INDEX ENG_8168_INDEX_USES_SQRT ON T55 (SQRT(BIG));
CREATE INDEX ENG_8168_INDEX_USES_SUBSTRING ON T55 (SUBSTRING(STR, BIG));
CREATE INDEX ENG_8168_INDEX_USES_SUBSTRING3 ON T55 (SUBSTRING(STR, BIG, 3));
CREATE INDEX ENG_8168_INDEX_USES_SUBSTRINGF ON T55 (SUBSTRING(STR FROM BIG) || 'XXX');
CREATE INDEX ENG_8168_INDEX_USES_SUBSTRINGFF ON T55 (SUBSTRING(STR FROM 2 FOR BIG));
CREATE INDEX ENG_8168_INDEX_USES_TO_TIMESTAMP_SECOND ON T55 (TO_TIMESTAMP(SECOND, BIG));
CREATE INDEX ENG_8168_INDEX_USES_TO_TIMESTAMP_MILLISECOND ON T55 (TO_TIMESTAMP(MILLISECOND, 1000 + BIG));
CREATE INDEX ENG_8168_INDEX_USES_TO_TIMESTAMP_MICROSECOND ON T55 (TO_TIMESTAMP(MICROSECOND, 1000 + BIG));
CREATE INDEX ENG_8168_INDEX_USES_TO_TIMESTAMP_MILLIS ON T55 (TO_TIMESTAMP(MILLIS, BIG));
CREATE INDEX ENG_8168_INDEX_USES_TO_TIMESTAMP_MICROS ON T55 (TO_TIMESTAMP(MICROS, BIG));
CREATE INDEX ENG_8168_INDEX_USES_TRIML ON T55 (TRIM(LEADING FROM STR));
CREATE INDEX ENG_8168_INDEX_USES_TRIMLX ON T55 (TRIM(LEADING 'X' FROM STR));
CREATE INDEX ENG_8168_INDEX_USES_TRIMT ON T55 (TRIM(TRAILING FROM STR));
CREATE INDEX ENG_8168_INDEX_USES_TRIMTX ON T55 (TRIM(TRAILING 'X' FROM STR));
CREATE INDEX ENG_8168_INDEX_USES_TRIM ON T55 (TRIM(STR));
CREATE INDEX ENG_8168_INDEX_USES_TRIMB ON T55 (TRIM(BOTH FROM STR || STR));
CREATE INDEX ENG_8168_INDEX_USES_TRIMBX ON T55 (TRIM(BOTH 'X' FROM STR || STR));
CREATE INDEX ENG_8168_INDEX_USES_TRIMLC ON T55 (TRIM(LEADING, STR || 'Z' || STR));
CREATE INDEX ENG_8168_INDEX_USES_TRIMLXC ON T55 (TRIM(LEADING, 'X', STR || 'Z' || STR));
CREATE INDEX ENG_8168_INDEX_USES_TRIMTC ON T55 (TRIM(TRAILING, STR || 'Z' || STR));
CREATE INDEX ENG_8168_INDEX_USES_TRIMTXC ON T55 (TRIM(TRAILING, 'X', STR || 'Z' || STR));
CREATE INDEX ENG_8168_INDEX_USES_TRIMBC ON T55 (TRIM(BOTH, STR || STR || 'Z' || STR));
CREATE INDEX ENG_8168_INDEX_USES_TRIMBXC ON T55 (TRIM(BOTH, 'X', STR || 'Z' || STR || 'Z' || STR));
CREATE INDEX ENG_8168_INDEX_USES_TRUNCATE_YEAR         ON T55 (TRUNCATE(YEAR, TS));
CREATE INDEX ENG_8168_INDEX_USES_TRUNCATE_QUARTER      ON T55 (TRUNCATE(QUARTER, TS));
CREATE INDEX ENG_8168_INDEX_USES_TRUNCATE_MONTH        ON T55 (TRUNCATE(MONTH, TS));
CREATE INDEX ENG_8168_INDEX_USES_TRUNCATE_DAY          ON T55 (TRUNCATE(DAY, TS));
CREATE INDEX ENG_8168_INDEX_USES_TRUNCATE_HOUR         ON T55 (TRUNCATE(HOUR, TS));
CREATE INDEX ENG_8168_INDEX_USES_TRUNCATE_MINUTE       ON T55 (TRUNCATE(MINUTE, TS));
CREATE INDEX ENG_8168_INDEX_USES_TRUNCATE_SECOND       ON T55 (TRUNCATE(SECOND, TS));
CREATE INDEX ENG_8168_INDEX_USES_TRUNCATE_MILLISECOND  ON T55 (TRUNCATE(MILLISECOND, TS));
CREATE INDEX ENG_8168_INDEX_USES_TRUNCATE_MILLIS       ON T55 (BIG, TRUNCATE(MILLIS, TS));
CREATE INDEX ENG_8168_INDEX_USES_UPPER ON T55 (UPPER(STR));
CREATE INDEX ENG_8168_INDEX_USES_WEEKDAY ON T55 (WEEKDAY(TS) + 7);
CREATE INDEX ENG_8168_INDEX_USES_WEEK ON T55 (WEEK(TS) + 52);
CREATE INDEX ENG_8168_INDEX_USES_WEEKOFYEAR ON T55 (WEEKOFYEAR(TS) + 104);
CREATE INDEX ENG_8168_INDEX_USES_YEAR ON T55 (YEAR(TS) - 1900);
-- Wrap up with a minor variant of an index already tried to show that
-- the previous line didn't corrupt catalog replay.
CREATE INDEX ENG_8168_INDEX_USES_ABS_AGAIN ON T55 (ABS(BIG - 1));


-- Test for polygon validity.
CREATE TABLE GEO (
    ID BIGINT PRIMARY KEY NOT NULL,
    REGION1    GEOGRAPHY,
    POINT1     GEOGRAPHY_POINT NOT NULL,
);

-- indexing on boolean expression is not supported at present.
--CREATE INDEX GEOINDEX_ISVALID ON GEO ( ID, ISVALID(REGION1) );
CREATE INDEX GEOINDEX_REASONS ON GEO ( ID, ISINVALIDREASON(REGION1) );
-- geo functions
CREATE INDEX INDEX_USES_GEO_ASTEXT_POINT ON T4 (ASTEXT(POINT1));
CREATE INDEX INDEX_USES_GEO_ASTEXT_POLYGON ON T4 (ASTEXT(REGION1));
CREATE INDEX INDEX_USES_GEO_LONGITUDE ON T4 (LONGITUDE(POINT1));
CREATE INDEX INDEX_USES_GEO_LATITUDE ON T4 (LATITUDE(POINT2));
CREATE INDEX INDEX_USES_GEO_DISTANCE_POINT_POINT ON T4 (DISTANCE(POINT1, POINT2));
CREATE INDEX INDEX_USES_GEO_DISTANCE_POLYGON_POINT ON T4 (DISTANCE(REGION1, POINT1));
CREATE INDEX PARTIAL_INDEX_USES_GEO_DISTANCE_POLYGON_POINT ON T4 (DISTANCE(REGION1, POINT1)) WHERE CONTAINS (REGION1, POINT1);
CREATE INDEX PARTIAL_INDEX_USES_GEO_AREA ON T4 (AREA(REGION1)) WHERE ISVALID(REGION1);
-- indexing on boolean expression is not supported at present.
--CREATE INDEX INDEX_USES_GEO_CONTAINS ON T4 (CONTAINS (REGION1, POINT1));
--CREATE INDEX INDEX_USES_GEO_WITHIN100000 ON T4 (DWITHIN(REGION1, POINT1, 100000));
-- Indexing on Geo types - polygon and point not supported at present
-- CREATE INDEX INDEX_USES_GEO_DISTANCE ON T4 (CENTROID(C12));
