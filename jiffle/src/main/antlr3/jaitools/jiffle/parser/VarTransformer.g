/*
 * Copyright 2011 Michael Bedward
 * 
 * This file is part of jai-tools.
 *
 * jai-tools is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 *
 * jai-tools is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public 
 * License along with jai-tools.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
  
 /** 
  * Transforms variables in the AST from the parser.
  * <ul>
  * <li>Names constants (e.g. PI) are replaced by FIXED_VALUE nodes.</li>
  * <li>TRUE, FALSE and NULL are replaced by 1.0, 0.0 and Double.NaN 
  *     FIXED_VALUE nodes.</li>
  * <li>Proxy functions (e.g. width(), x()) are replaced by VAR nodes 
  *     with runtime variable names.</li>
  * </ul>
  *
  * @author Michael Bedward
  */

tree grammar VarTransformer;

options {
    tokenVocab = Jiffle;
    ASTLabelType = CommonTree;
    output = AST;
    superClass = ErrorHandlingTreeParser;
}

tokens {
    FIXED_VALUE;
    IMAGE_VAR;
    IMAGE_WRITE;
    VAR;
}

@header {
package jaitools.jiffle.parser;

import java.util.Map;
import jaitools.jiffle.Jiffle;
}

@members {

private FunctionLookup functionLookup = new FunctionLookup();

private Map<String, Jiffle.ImageRole> imageParams;

public void setImageParams( Map<String, Jiffle.ImageRole> params ) {
    imageParams = params;
}

}

start
@init {
    if (imageParams == null) {
        throw new IllegalStateException("Internal compiler error: image params not set");
    }
}
                : (var_init_block)? statement+ 
                ;

var_init_block  : ^(VAR_INIT_BLOCK var_init_list)
                ;

var_init_list   : ^(VAR_INIT_LIST (var_init)*)
                ;

var_init        : ^(VAR_INIT ID expr)
                ;

statement       : expr
                ;

expr_list returns [int size]
@init { $size = 0; }
                : ^(EXPR_LIST (expr {$size++;})*)
                ;

expr            : ^(ASSIGN assign_op var expr)
                  -> {imageParams.containsKey($var.text)}? ^(IMAGE_WRITE var expr)
                  -> ^(ASSIGN assign_op var expr)
                  
                | ^(FUNC_CALL ID expr_list)
                  { 
                      FunctionInfo info = null;
                      try {
                          info = functionLookup.getInfo($ID.text, $expr_list.size);
                      } catch (UndefinedFunctionException ex) {
                          throw new IllegalStateException("Internal compiler error", ex);
                      }
                  }

                  -> {info.isProxy()}? VAR[info.getRuntimeExpr()]
                  -> ^(FUNC_CALL ID expr_list)
                  
                | ^(NBR_REF ID nbr_ref_expr nbr_ref_expr) 
                  -> ^(NBR_REF IMAGE_VAR[$ID.text] nbr_ref_expr nbr_ref_expr)
                  
                | ^(IF_CALL expr_list)
                | ^(QUESTION expr expr expr)
                | ^(PREFIX unary_op expr)
                | ^(expr_op expr expr)
                | ^(BRACKETED_EXPR expr)
                | var
                | constant
                | INT_LITERAL -> FIXED_VALUE<FixedValueNode>[$INT_LITERAL.text]
                | FLOAT_LITERAL -> FIXED_VALUE<FixedValueNode>[$FLOAT_LITERAL.text] 
                ;

nbr_ref_expr    : ^(ABS_NBR_REF expr)
                | ^(REL_NBR_REF expr)
                ;

var             :ID
                  -> {imageParams.containsKey($ID.text)}? IMAGE_VAR[$ID.text]
                  -> {ConstantLookup.isDefined($ID.text)}? FIXED_VALUE<FixedValueNode>[ConstantLookup.getValue($ID.text)]
                  -> VAR[$ID.text]
                ;
                
constant        : TRUE -> FIXED_VALUE<FixedValueNode>[1.0d]
                | FALSE -> FIXED_VALUE<FixedValueNode>[0.0d]
                | NULL -> FIXED_VALUE<FixedValueNode>[Double.NaN]
                ;

expr_op         : POW
                | TIMES 
                | DIV 
                | MOD
                | PLUS  
                | MINUS
                | OR 
                | AND 
                | XOR 
                | GT 
                | GE 
                | LE 
                | LT 
                | LOGICALEQ 
                | NE 
                ;

assign_op	: EQ
		| TIMESEQ
		| DIVEQ
		| MODEQ
		| PLUSEQ
		| MINUSEQ
		;
		
incdec_op       : INCR
                | DECR
                ;

unary_op	: PLUS
		| MINUS
		| NOT
		;
		
type_name	: 'int'
		| 'float'
		| 'double'
		| 'boolean'
		;
