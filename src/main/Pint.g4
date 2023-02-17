grammar Pint;

file : def* EOF ;
def : funcDef | varDef ;
funcDef : 'let' ID '(' paramList ')' '->' type blockExpr ;
varDef : 'let' ID ':' type ':=' expr ';' ;
paramList : (param ',')* param? ;
param : ID ':' type ;
type : ID               #simpleType
     | type '[' ']'     #arrayType
     | type 'when' expr #conditionType
     | 'unit'           #unitType
     ;
expr : assignExpr ;
assignExpr : left=orExpr (op=(':=' | ':+=' | ':-=' | ':*=' | ':/=') right=assignExpr)? ;
orExpr : left=andExpr (op='or' right=orExpr)? ;
andExpr : left=cmpExpr (op='and' right=andExpr)? ;
cmpExpr : left=addExpr (not='not'? op=('=' | '<' | '>' | '<=' | '>=') right=cmpExpr)? ;
addExpr : left=mulExpr (op=('+' | '-') right=addExpr)? ;
mulExpr : left=unaryExpr (op=('*' | '/') right=mulExpr)? ;
unaryExpr : ops+=('+' | '-' | 'not')* factor ;
factor : labeledBlockExpr #labeledBlockFactor
       | '(' expr ')'     #parensFactor
       | '|' expr '|'     #absFactor
       | ID               #varFactor
       | funcCallExpr     #funcCallFactor
       | controlFlowExpr  #controlFlowFactor
       | factor indexOp   #indexFactor
       | arrayLiteral     #arrayLiteralFactor
       | literal          #literalFactor
       | 'it'             #itFactor
       ;
labeledBlockExpr : label? blockExpr ;
label : ID ':' ;
blockExpr : '{' statement* expr? '}' ;
statement : varDef | expr? ';' ;
funcCallExpr : ID '(' (expr ',')* expr? ')' ;
controlFlowExpr : ifExpr | loopExpr | whileExpr | jumpExpr ;
ifExpr : 'if' cond=expr 'then' thenBody=expr ('else' elseBody=expr)? ;
loopExpr : label? 'loop' body=expr ;
whileExpr : label? 'while' cond=expr 'loop' body=expr ;
jumpExpr : jump=('return' | 'break' | 'continue') atLabel? expr? ;
atLabel : '@' ID ;
indexOp : '[' expr ']' ;
arrayLiteral : '[' (expr ',')* expr? ']' ;
literal : int=INT_LITERAL | string=STRING_LITERAL | bool=('true' | 'false') | unit='unit' ;

ID : [A-Za-z_][A-Za-z0-9_]* ;
INT_LITERAL : [0-9]+ ;
STRING_LITERAL : '"' ~["]* '"' ;

WS : [ \t\r\n]+ -> skip ;
