/* This file is part of VoltDB.
 * Copyright (C) 2008-2012 VoltDB Inc.
 *
 * This file contains original code and/or modifications of original code.
 * Any modifications made by VoltDB Inc. are licensed under the following
 * terms and conditions:
 *
 * VoltDB is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * VoltDB is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with VoltDB.  If not, see <http://www.gnu.org/licenses/>.
 */
/* Copyright (C) 2008 by H-Store Project
 * Brown University
 * Massachusetts Institute of Technology
 * Yale University
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
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

#include <sstream>
#include "tablefactory.h"
#include "common/executorcontext.hpp"
#include "common/debuglog.h"
#include "common/tabletuple.h"
#include "storage/table.h"
#include "storage/persistenttable.h"
#include "storage/streamedtable.h"
#include "storage/temptable.h"
#include "storage/TempTableLimits.h"
#include "indexes/tableindexfactory.h"
#include "common/Pool.hpp"

namespace voltdb {
Table* TableFactory::getPersistentTable(
            voltdb::CatalogId databaseId,
            ExecutorContext *ctx,
            const std::string &name,
            TupleSchema* schema,
            const std::vector<std::string> &columnNames,
            const TableIndexScheme *pkeyIndex,
            const std::vector<TableIndexScheme> &indexes,
            int partitionColumn,
            bool exportEnabled,
            bool exportOnly)
{
    Table *table = NULL;

    if (exportOnly) {
        table = new StreamedTable(ctx, exportEnabled);
    }
    else {
        PersistentTable *pTable = new PersistentTable(ctx, exportEnabled);
        table = pTable;
        pTable->m_partitionColumn = partitionColumn;

        if (pkeyIndex) {
            pTable->m_pkeyIndex = TableIndexFactory::getInstance(*pkeyIndex);
            pTable->m_indexes.push_back(pTable->m_pkeyIndex);
            pTable->m_uniqueIndexes.push_back(pTable->m_pkeyIndex);
        }

        for (int i = 0; i < indexes.size(); ++i) {
            TableIndex *index = TableIndexFactory::getInstance(indexes[i]);
            pTable->m_indexes.push_back(index);
            if (index->isUniqueIndex()) {
                pTable->m_uniqueIndexes.push_back(index);
            }
        }
    }

    initCommon(databaseId, table, name, schema, columnNames, true);

    // initialize stats for the table
    configureStats(databaseId, ctx, name, table);

    return dynamic_cast<Table*>(table);
}

TempTable* TableFactory::getTempTable(
            const voltdb::CatalogId databaseId,
            const std::string &name,
            TupleSchema* schema,
            const std::vector<std::string> &columnNames,
            TempTableLimits* limits)
{
    TempTable* table = new TempTable();
    initCommon(databaseId, table, name, schema, columnNames, true);
    table->m_limits = limits;
    return table;
}

/**
 * Creates a temp table with the same schema as the provided template table
 */
TempTable* TableFactory::getCopiedTempTable(
            const voltdb::CatalogId databaseId,
            const std::string &name,
            const Table* template_table,
            TempTableLimits* limits)
{
    TempTable* table = new TempTable();
    initCommon(databaseId, table, name, template_table->m_schema, template_table->m_columnNames, false);
    table->m_limits = limits;
    return table;
}

void TableFactory::initCommon(
            voltdb::CatalogId databaseId,
            Table *table,
            const std::string &name,
            TupleSchema *schema,
            const std::vector<std::string> &columnNames,
            const bool ownsTupleSchema) {

    assert(table != NULL);
    assert(schema != NULL);
    assert(columnNames.size() != 0);

    table->m_databaseId = databaseId;
    table->m_name = name;
    table->initializeWithColumns(schema, columnNames, ownsTupleSchema);
    assert (table->columnCount() == schema->columnCount());
}

void TableFactory::configureStats(voltdb::CatalogId databaseId,
                                  ExecutorContext *ctx,
                                  std::string name,
                                  Table *table) {

    // initialize stats for the table
    table->getTableStats()->configure(name + " stats",
                                      ctx->m_hostId,
                                      ctx->m_hostname,
                                      ctx->m_siteId,
                                      ctx->m_partitionId,
                                      databaseId);

    // initialize stats for all the indexes for the table
    std::vector<TableIndex*> tindexes = table->allIndexes();
    for (size_t i = 0; i < tindexes.size(); i++) {
        TableIndex *index = tindexes[i];
        index->getIndexStats()->configure(index->getName() + " stats",
                                          table->name(),
                                          ctx->m_hostId,
                                          ctx->m_hostname,
                                          ctx->m_siteId,
                                          ctx->m_partitionId,
                                          databaseId);
    }
}

}
