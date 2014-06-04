/* This file is part of VoltDB.
 * Copyright (C) 2008-2014 VoltDB Inc.
 *
 * This file contains original code and/or modifications of original code.
 * Any modifications made by VoltDB Inc. are licensed under the following
 * terms and conditions:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
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

#ifndef VOLTDBSCANEXECUTOR_H
#define VOLTDBSCANEXECUTOR_H

#include "executors/abstractexecutor.h"

#include "plannodes/limitnode.h"
#include "plannodes/projectionnode.h"

namespace voltdb {

class TableCatalogDelegate;
class AbstractExpression;

class AbstractScanExecutor : public AbstractExecutor
{
public:
    AbstractScanExecutor() { }

protected:
    bool p_init(TempTableLimits* limits);
    virtual bool p_initMore(TempTableLimits* limits) = 0;
    Table* getTargetTable();

    void getLimitAndOffsetByReference(int& limit, int& offset)
    { m_inlineLimitOffset.getLimitAndOffsetByReference(m_engine, limit, offset); }

    const int* getProjectionColumns() const
    { return m_inlineProjection.getProjectionColumns(); }

    const AbstractExpression* const* getProjectionExpressions() const
    { return m_inlineProjection.getProjectionExpressions(); }

    AbstractExpression* getPredicate() const { return m_predicate; }
private:
    ProjectionPlanNode::InlineState m_inlineProjection;
    LimitPlanNode::InlineState m_inlineLimitOffset;
    TableCatalogDelegate* m_target_tcd;
    AbstractExpression* m_predicate;

};

}

#endif
