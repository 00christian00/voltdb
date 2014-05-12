/* This file is part of VoltDB.
 * Copyright (C) 2008-2014 VoltDB Inc.
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

package org.voltdb.planner.microoptimizations;

import java.util.ArrayList;

import org.voltdb.compiler.DeterminismMode;
import org.voltdb.planner.AbstractParsedStmt;
import org.voltdb.planner.CompiledPlan;

public class MicroOptimizationRunner {

    // list all of the micro optimizations here
    static ArrayList<MicroOptimization> optimizations = new ArrayList<MicroOptimization>();
    static {
        optimizations.add(new PushdownLimits());
        optimizations.add(new ReplaceWithIndexCounter());
        optimizations.add(new SeqScansToUniqueTreeScans());
        optimizations.add(new ReplaceWithIndexLimit());
    }

    public static void applyAll(CompiledPlan plan, DeterminismMode detMode,
            AbstractParsedStmt parsedStmt)
    {
        boolean statementIsDeterministic =  plan.hasDeterministicStatement();
        for (MicroOptimization opt : optimizations) {
            // skip optimizations that don't apply at this determinism level
            if (!opt.shouldRun(detMode, statementIsDeterministic)) {
                continue;
            }

            opt.apply(plan, parsedStmt);
        }
    }

}
