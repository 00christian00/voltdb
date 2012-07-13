/* This file is part of VoltDB.
 * Copyright (C) 2008-2012 VoltDB Inc.
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

package org.voltdb.planner.microoptimizations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.voltdb.catalog.ColumnRef;
import org.voltdb.catalog.Index;
import org.voltdb.expressions.AbstractExpression;
import org.voltdb.expressions.TupleValueExpression;
import org.voltdb.planner.CompiledPlan;
import org.voltdb.plannodes.AbstractPlanNode;
import org.voltdb.plannodes.AggregatePlanNode;
import org.voltdb.plannodes.IndexCountPlanNode;
import org.voltdb.plannodes.IndexScanPlanNode;
import org.voltdb.types.ExpressionType;
import org.voltdb.utils.CatalogUtil;

public class ReplaceWithIndexCounter implements MicroOptimization {

    @Override
    public List<CompiledPlan> apply(CompiledPlan plan) {
        ArrayList<CompiledPlan> retval = new ArrayList<CompiledPlan>();

        AbstractPlanNode planGraph = plan.fragments.get(0).planGraph;
        planGraph = recursivelyApply(planGraph);
        plan.fragments.get(0).planGraph = planGraph;

        retval.add(plan);
        return retval;
    }

    AbstractPlanNode recursivelyApply(AbstractPlanNode plan) {
        assert(plan != null);

        // depth first:
        //     find AggregatePlanNode with exactly one child
        //     where that child is an AbstractScanPlanNode.
        //     Replace the AggregatePlanNode and AbstractScanPlanNode
        //     with IndexCountPlanNode

        ArrayList<AbstractPlanNode> children = new ArrayList<AbstractPlanNode>();

        for (int i = 0; i < plan.getChildCount(); i++)
            children.add(plan.getChild(i));
        plan.clearChildren();

        for (AbstractPlanNode child : children) {
            // TODO this will break when children feed multiple parents
            child = recursivelyApply(child);
            child.clearParents();
            plan.addAndLinkChild(child);
        }

        if ((plan instanceof AggregatePlanNode) == false)
            return plan;
        if (plan.getChildCount() != 1)
            return plan;
        // check aggregation type
        List <ExpressionType> et = ((AggregatePlanNode) plan).getAggregateTypes();
        if ((et.size() == 1 &&
             et.get(0).equals(ExpressionType.AGGREGATE_COUNT_STAR)) == false)
            return plan;

        AbstractPlanNode child = plan.getChild(0);
        if ((child instanceof IndexScanPlanNode) == false)
            return plan;

        // check index type
        Index idx = ((IndexScanPlanNode)child).getCatalogIndex();
        if (idx.getCountable() == false)
            return plan;

        IndexCountPlanNode icpn = null;
        if (isReplaceable((IndexScanPlanNode)child)) {
            icpn = new IndexCountPlanNode((IndexScanPlanNode)child);
            icpn.setOutputSchema(plan.getOutputSchema());

            // TODO: I am not sure if there is a null case or not
            if (plan.getParent(0) != null) {
                plan.addIntermediary(plan.getParent(0));
            }

            // TODO: set schema using plan's schema
            plan.removeFromGraph();
            child.removeFromGraph();

            return icpn;
        }
        return plan;
    }


    // TODO(xin): add more checkings to replace only certain cases.
    boolean isReplaceable(IndexScanPlanNode child) {

        AbstractExpression endExpr = child.getEndExpression();
        AbstractExpression predicateExpr = child.getPredicate();
        ArrayList<AbstractExpression> subEndExpr = null;
        if (endExpr != null) {
            System.err.println("End Expression:\n" + endExpr);
            subEndExpr = endExpr.findAllSubexpressionsOfClass(TupleValueExpression.class);
        }

        assert(predicateExpr != null);
        System.err.println("Post Expression:\n" + predicateExpr);
        ArrayList<AbstractExpression> hasLeft = predicateExpr.findAllSubexpressionsOfClass(TupleValueExpression.class);

        Set<String> columnsLeft = new HashSet<String>();
        for (AbstractExpression ae: hasLeft) {
            TupleValueExpression tve = (TupleValueExpression) ae;
            columnsLeft.add(tve.getColumnName());
            System.err.println("predicate columns:" + tve.getColumnName());
        }

        if (endExpr != null && subEndExpr != null) {
            for (AbstractExpression ae: subEndExpr) {
                TupleValueExpression tve = (TupleValueExpression) ae;
                String columnName = tve.getColumnName();
                if (columnsLeft.contains(columnName)) {
                    columnsLeft.remove(columnName);
                }
            }
        }

        Index idx = child.getCatalogIndex();
        List<ColumnRef> sortedColumns = CatalogUtil.getSortedCatalogItems(idx.getColumns(), "index");
        int searchKeySize = child.getSearchKeyExpressions().size();

        if (columnsLeft.size() > searchKeySize)
            return false;

        // assume it has searchKey
        for (int i = 0; i < searchKeySize; i++) {
            ColumnRef cr = sortedColumns.get(i);
            String colName = cr.getColumn().getName();
            if (columnsLeft.contains(colName)) {
                columnsLeft.remove(colName);
                System.err.println("SearchKey Column removed:" + colName);
            }
        }

        if (columnsLeft.size() != 0)
            return false;
        return true;
    }

}
