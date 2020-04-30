// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.impala.analysis;

import org.apache.impala.catalog.Type;
import org.apache.impala.common.AnalysisException;
import org.apache.impala.common.Pair;
import org.apache.impala.common.Reference;
import org.apache.impala.thrift.TExprNode;
import org.apache.impala.thrift.TExprNodeType;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;



public class CompoundVerticalBarExpr extends Expr {
  private CompoundPredicate logicalOR;
  private FunctionCallExpr concatFunctionCall;

  public CompoundVerticalBarExpr(Expr e1, Expr e2) {
    super();
    Preconditions.checkNotNull(e1);
    Preconditions.checkNotNull(e2);
      children_.add(e1);
      children_.add(e2);
  }

  public CompoundVerticalBarExpr(CompoundVerticalBarExpr other) {
    super(other);
    logicalOR = other.logicalOR;
    concatFunctionCall = other.concatFunctionCall;
  }

  @Override
  protected void toThrift(TExprNode msg) {
    if (logicalOR != null) {
      logicalOR.toThrift(msg);
    } else if (concatFunctionCall != null) {
      concatFunctionCall.toThrift(msg);
    }
  }

  @Override
  protected float computeEvalCost() {
    if (logicalOR != null) {
      return logicalOR.computeEvalCost();
    } else if (concatFunctionCall != null) {
      return concatFunctionCall.computeEvalCost();
    } else {
      return UNKNOWN_COST;
    }
  }

  @Override
  public String toSqlImpl(ToSqlOptions options) {
    if (logicalOR != null) {
      return logicalOR.toSqlImpl(options);
    } else if (concatFunctionCall != null) {
      return concatFunctionCall.toSqlImpl(options);
    } else {
      return "";
    }
  }

  @Override
  protected void analyzeImpl(Analyzer analyzer) throws AnalysisException {
    Expr e1 = getChild(0);
    Expr e2 = getChild(1);
    if (e1.getType ().isBoolean () || e1.getType().isNull()) {
      type_ = Type.BOOLEAN;
      logicalOR = new CompoundPredicate(CompoundPredicate.Operator.OR, e1, e2);
      logicalOR.analyzeImpl(analyzer);
    } else if (e1.getType ().isStringType ()) {
      type_ = Type.STRING;
      concatFunctionCall = new FunctionCallExpr("concat", Lists.newArrayList(e1, e2));
      concatFunctionCall.analyzeImpl(analyzer);
    } else {
      throw new AnalysisException(String.format("Operand '%s' part of CompoundVerticalBarExpr " +
            "'%s' should return type 'BOOLEAN' or 'STRING' but returns type '%s'.",
            e1.toSql(), toSql(), e1.getType().toSql()));
    }
  }

  @Override
  public Expr clone() { return new CompoundVerticalBarExpr (this); }
  
  public Expr getEncapsulatedExpr () {
    if (logicalOR != null) {
      return logicalOR;
    } else {
      return concatFunctionCall;
    }
  }
}