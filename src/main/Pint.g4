grammar Pint;

file : def* EOF ;
def : funcDef | varDef ;
funcDef : 'let' ID '(' paramList ')' '->' type blockExpr ;
varDef : 'let' ID ':' type ':=' expr ';' ;
paramList : (param ',')* param? ;
param : ID ':' type ;
type : ID               #simpleType
     | 'unit'           #unitType
     | type '[' ']'     #arrayType
     | type 'when' expr #conditionType
     ;
expr : factor                                                             #factorExpr
     | op=('+' | '-' | 'not') expr                                        #unaryExpr
     | left=expr op=('*' | '/') right=expr                                #mulExpr
     | left=expr op=('+' | '-') right=expr                                #addExpr
     | left=expr not='not'? op=('=' | '<' | '>' | '<=' | '>=') right=expr #cmpExpr
     | left=expr op='and' right=expr                                      #andExpr
     | left=expr op='or' right=expr                                       #orExpr
     | left=expr op=(':=' | ':+=' | ':-=' | ':*=' | ':/=') right=expr     #assignExpr
     ;
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
literal : string=STRING_LITERAL | int=INT_LITERAL | bool=('true' | 'false') | unit='unit' ;

ID : [A-Za-z_][A-Za-z0-9_]* ;
INT_LITERAL : [0-9]+ ;
STRING_LITERAL : '"' ~["]* '"' ;

COMMENT : '/*' (~'*' | '*' ~'/' | COMMENT)* '*/' -> skip ;
LINE_COMMENT : '//' ~[\r\n]* -> skip ;
WS : [ \t\r\n]+ -> skip ;
