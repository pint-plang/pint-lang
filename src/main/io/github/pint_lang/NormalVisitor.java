package io.github.pint_lang;


import com.ibm.icu.impl.InvalidFormatException;



import io.github.pint_lang.gen.PintBaseVisitor;


import io.github.pint_lang.gen.PintParser.*;



import org.antlr.v4.runtime.tree.*;


import java.io.PrintStream;
import java.util.HashMap;


// Normal Visitor will use the same code from the printVisitor, but this visitor will be typechecking the context that has been investigated into each of these methods.
public class NormalVisitor extends PintBaseVisitor<Void>
{

    // LinkedList to hold tokens, not sure if this is needed.
    private final HashMap<String, String> SymbolType= new HashMap<>();
    private final HashMap<String, String> VariableValues = new HashMap<>();
 //   int count = 1;
    
    public PrintStream out;
    public ParseTreeWalker P1;
    public NormalVisitor(PrintStream out, ParseTreeWalker P1){
        this.out= out;
        this.P1 = P1;
    }

    @Override
    public Void visitFile(FileContext ctx) {
        for (var definition : ctx.def()) {
            definition.accept(this);
            //out.println();
        }
        return null;
    }

    @Override
    public Void visitDef(DefContext ctx) {
        var funcDef = ctx.funcDef();
        if (funcDef != null) {
            funcDef.accept(this);
            return null;
        }
        var varDef = ctx.varDef();
        varDef.accept(this);
        return null;
    }

    @Override
    public Void visitFuncDef(FuncDefContext ctx) {

        out.print("let ");
        out.print(ctx.ID().getText());
        out.print("(");
        ctx.paramList().accept(this);
        out.print(") -> ");
        ctx.type().accept(this);
        out.print(" ");
        ctx.blockExpr().accept(this);
        return null;
    }


    @Override
    public Void visitVarDef(VarDefContext ctx) {
      //  LinkedList<Integer> numList= new LinkedList<>();
        out.print("let ");
        out.print(ctx.ID().getText());
        out.print(": ");
        ctx.type().accept(this);
        if(!SymbolType.containsKey(ctx.ID().getText())){
            SymbolType.put(ctx.ID().getText(), ctx.type().getText());
        }
        out.print(" := ");
        ctx.expr().accept(this);
        var s ="";
        var num=0;
        if(ctx.expr().getText().contains("+")){
            s = ctx.expr().getText().replace('+', ' ');
        }
        if (ctx.expr().getText().contains("-")){
            s = ctx.expr().getText().replace('-', ' ');
        }
        if(ctx.expr().getText().equals("*")){
            s = ctx.expr().getText().replace('*', ' ');
        }
        if(ctx.expr().getText().equals("/")){
            s = ctx.expr().getText().replace('/', ' ');
        }
        var variables = s.split(" ");

        boolean putFinalVal = false;

        for( var Val: variables){

            if(VariableValues.containsKey(Val)){
                num+=Integer.parseInt(VariableValues.get(Val));
                putFinalVal = true;
            }
            else if(isANumber(Val)){
                putFinalVal = true;
                num += Integer.parseInt(Val);
            }

        }
        if(putFinalVal){

         VariableValues.put(ctx.ID().getText(),String.valueOf(num));
        }
        else{
            VariableValues.put(ctx.ID().getText(), ctx.expr().getText());
        }

        out.print(";");

        return null;
    }

    @Override
    public Void visitParamList(ParamListContext ctx) {
        for (var param : ctx.param()) {
            param.accept(this);
            out.print(", ");
        }
        return null;
    }

    @Override
    public Void visitParam(ParamContext ctx) {
        out.print(ctx.ID().getText());
        out.print(": ");
        ctx.type().accept(this);
        return null;
    }

    @Override
    public Void visitSimpleType(SimpleTypeContext ctx) {
        out.print(ctx.ID().getText());
        return null;
    }

    @Override
    public Void visitArrayType(ArrayTypeContext ctx) {
        ctx.type().accept(this);
        out.print("[]");
        return null;
    }

    @Override
    public Void visitConditionType(ConditionTypeContext ctx) {
        ctx.type().accept(this);
        out.print(" when ");
        ctx.expr().accept(this);
        return null;
    }

    @Override
    public Void visitUnitType(UnitTypeContext ctx) {
        out.print("unit");
        return null;
    }

    @Override
    public Void visitFactorExpr(FactorExprContext ctx) {
        ctx.factor().accept(this);
        return null;
    }

    @Override
    public Void visitUnaryExpr(UnaryExprContext ctx) {
        out.print(ctx.op.getText());
        ctx.expr().accept(this);
        return null;
    }

    @Override
    public Void visitMulExpr(MulExprContext ctx) {
        ctx.left.accept(this);
        out.print(" ");
        out.print(ctx.op.getText());
        out.print(" ");
        ctx.right.accept(this);

        typeCheck(ctx);
        return null;
    }

    @Override
    public Void visitAddExpr(AddExprContext ctx) {
        ctx.left.accept(this);
        out.print(" ");
        out.print(ctx.op.getText());
        out.print(" ");
        ctx.right.accept(this);

        int finalValue =0;
        if(VariableValues.containsKey(ctx.left.getText())){

            finalValue += Integer.parseInt(VariableValues.get(ctx.left.getText()));
        }
        else{
            var s ="";

            if(ctx.left.getText().contains("+")){
                s = ctx.left.getText().replace('+', ' ');
            }
            if (ctx.left.getText().contains("-")){
                s = ctx.left.getText().replace('-', ' ');
            }
            if(ctx.left.getText().equals("*")){
                s = ctx.left.getText().replace('*', ' ');
            }
            if(ctx.left.getText().equals("/")){
                s = ctx.left.getText().replace('/', ' ');
            }
            var variables = s.split(" ");
            for(var val: variables){
             if(VariableValues.containsKey(val)){
                 finalValue+=Integer.parseInt(VariableValues.get(val));
             }
            }
        }
        if(VariableValues.containsKey(ctx.right.getText())){

            switch (ctx.op.getText()){
                case "+" -> finalValue += Integer.parseInt(VariableValues.get(ctx.right.getText()));
                case "-" -> finalValue -= Integer.parseInt(VariableValues.get(ctx.right.getText()));
            }
        }
        else {

            switch (ctx.op.getText()){
                case "+" -> finalValue += Integer.parseInt(ctx.right.getText());
                case "-" -> finalValue -= Integer.parseInt(ctx.right.getText());
            }
        }
        for(var key:SymbolType.keySet()){
            if(!VariableValues.containsKey(key)){
                VariableValues.put(key, Integer.toString(finalValue));
            }

        }
        typeCheck(ctx);
        return null;
    }

    @Override
    public Void visitCmpExpr(CmpExprContext ctx) {
        ctx.left.accept(this);
        out.print(" ");
        if (ctx.not != null) out.print("not ");
        out.print(ctx.op.getText());
        out.print(" ");
        ctx.right.accept(this);
        return null;
    }

    @Override
    public Void visitAndExpr(AndExprContext ctx) {
        ctx.left.accept(this);
        out.print(" ");
        out.print(ctx.op.getText());
        out.print(" ");
        ctx.right.accept(this);
        return null;
    }

    @Override
    public Void visitOrExpr(OrExprContext ctx) {
        ctx.left.accept(this);
        out.print(" ");
        out.print(ctx.op.getText());
        out.print(" ");
        ctx.right.accept(this);
        return null;
    }

    @Override
    public Void visitAssignExpr(AssignExprContext ctx) {
        ctx.left.accept(this);
        out.print(" ");
        out.print(ctx.op.getText());
        out.print(" ");
        ctx.right.accept(this);
        return null;
    }

    @Override
    public Void visitLabeledBlockFactor(LabeledBlockFactorContext ctx) {
        ctx.labeledBlockExpr().accept(this);
        return null;
    }

    @Override
    public Void visitParensFactor(ParensFactorContext ctx) {
        out.print("(");
        ctx.expr().accept(this);
        out.print(")");
        return null;
    }

    @Override
    public Void visitAbsFactor(AbsFactorContext ctx) {
        out.print("|");
        ctx.expr().accept(this);
        out.print("|");
        return null;
    }

    @Override
    public Void visitVarFactor(VarFactorContext ctx) {
        out.print(ctx.ID().getText());
        return null;
    }

    @Override
    public Void visitFuncCallFactor(FuncCallFactorContext ctx) {
        ctx.funcCallExpr().accept(this);
        return null;
    }

    @Override
    public Void visitControlFlowFactor(ControlFlowFactorContext ctx) {
        ctx.controlFlowExpr().accept(this);
        return null;
    }

    @Override
    public Void visitIndexFactor(IndexFactorContext ctx) {
        ctx.factor().accept(this);
        ctx.indexOp().accept(this);
        return null;
    }

    @Override
    public Void visitArrayLiteralFactor(ArrayLiteralFactorContext ctx) {
        ctx.arrayLiteral().accept(this);
        return null;
    }

    @Override
    public Void visitLiteralFactor(LiteralFactorContext ctx) {
        ctx.literal().accept(this);
        return null;
    }

    @Override
    public Void visitItFactor(ItFactorContext ctx) {
        out.print("it");
        return null;
    }

    @Override
    public Void visitLabeledBlockExpr(LabeledBlockExprContext ctx) {
        var label = ctx.label();
        if (label != null) label.accept(this);
        ctx.blockExpr().accept(this);
        return null;
    }

    @Override
    public Void visitLabel(LabelContext ctx) {
        out.print(ctx.ID().getText());
        out.print(": ");
        return null;
    }

    @Override
    public Void visitBlockExpr(BlockExprContext ctx) {
        out.print("{ ");
        for (var statement : ctx.statement()) {
            statement.accept(this);
            out.print(" ");
        }
        var expr = ctx.expr();
        if (expr != null) {
            expr.accept(this);
            out.print(" ");
        }
        out.print("}");
        return null;
    }

    @Override
    public Void visitStatement(StatementContext ctx) {
        var varDef = ctx.varDef();
        if (varDef != null) {
            varDef.accept(this);
        } else {
            var expr = ctx.expr();
            if (expr != null) expr.accept(this);
            out.print(";");
        }
        return null;
    }

    @Override
    public Void visitFuncCallExpr(FuncCallExprContext ctx) {
        out.print(ctx.ID().getText());
        out.print("(");
        for (var expr : ctx.expr()) {
            expr.accept(this);
            out.print(", ");
        }
        out.print(")");
        return null;
    }

    @Override
    public Void visitControlFlowExpr(ControlFlowExprContext ctx) {
        var ifExpr = ctx.ifExpr();
        if (ifExpr != null) {
            ifExpr.accept(this);
            return null;
        }
        var loopExpr = ctx.loopExpr();
        if (loopExpr != null) {
            loopExpr.accept(this);
            return null;
        }
        var whileExpr = ctx.whileExpr();
        if (whileExpr != null) {
            whileExpr.accept(this);
            return null;
        }
        ctx.jumpExpr().accept(this);
        return null;
    }

    @Override
    public Void visitIfExpr(IfExprContext ctx) {
        out.print("if ");
        ctx.cond.accept(this);
        out.print(" then ");
        ctx.thenBody.accept(this);
        if (ctx.elseBody != null) {
            out.print(" else ");
            ctx.elseBody.accept(this);
        }
        return null;
    }

    @Override
    public Void visitLoopExpr(LoopExprContext ctx) {
        var label = ctx.label();
        if (label != null) label.accept(this);
        out.print("loop ");
        ctx.body.accept(this);
        return null;
    }

    @Override
    public Void visitWhileExpr(WhileExprContext ctx) {
        var label = ctx.label();
        if (label != null) label.accept(this);
        out.print("while ");
        ctx.cond.accept(this);
        out.print(" loop ");
        ctx.body.accept(this);
        return null;
    }

    @Override
    public Void visitJumpExpr(JumpExprContext ctx) {
        out.print(ctx.jump.getText());
        var atLabel = ctx.atLabel();
        if (atLabel != null) atLabel.accept(this);
        var expr = ctx.expr();
        if (expr != null) {
            out.print(" ");
            expr.accept(this);
        }
        return null;
    }

    @Override
    public Void visitAtLabel(AtLabelContext ctx) {
        out.print("@");
        out.print(ctx.ID().getText());
        return null;
    }

    @Override
    public Void visitIndexOp(IndexOpContext ctx) {
        out.print("[");
        ctx.expr().accept(this);
        out.print("]");

        return null;
    }

    @Override
    public Void visitArrayLiteral(ArrayLiteralContext ctx) {
        out.print("[");
        for (var expr : ctx.expr()) {
            expr.accept(this);
            out.print(", ");
        }
        out.print("]");
        return null;
    }

    @Override
    public Void visitLiteral(LiteralContext ctx) {
        if (ctx.int_ != null) out.print(ctx.int_.getText());
        else if (ctx.string != null) out.print(ctx.string.getText());
        else if (ctx.bool !=  null) out.print(ctx.bool.getText());
        else out.print(ctx.unit.getText());
        return null;
    }



    public boolean isANumber(String number){
        try {
            Integer.parseInt(number);
            return true;

        }
        catch (NumberFormatException E){
            return false;
        }

    }

    // separate typeCheck Method ()
    // expression is the context to which a variable potentially exists.
    /*
    public boolean typeCheck2(Object Expression, Value ExpectedType){
        if(Expression instanceof Boolean B1){
            return ExpectedType.equals(Value.of(B1));
        }
        if(Expression instanceof Integer I1){
            return ExpectedType.equals(Value.of(I1));
        }
        if(Expression instanceof AddExprContext context){

            return typeCheck2(context.left, ExpectedType) && typeCheck2(context.right, ExpectedType);
        }
        if(Expression instanceof MulExprContext context){
            return typeCheck2(context.left, ExpectedType) && typeCheck2(context.right, ExpectedType);
        }
        if(Expression instanceof VarDefContext context){
            var type = SymbolType.get(context.ID().getText());
            return type.equals("int") && (ExpectedType.getClass().toString().equalsIgnoreCase("Integer"));
        }
        


        return false;

    }

     */


    public  void typeCheck(Object ctx){
        //out.print("\n");
       if(ctx==null)
           return;

       if(ctx instanceof LiteralContext Ctx){
           try {

            if(Ctx.int_!=null){
                Integer.parseInt(Ctx.int_.getText());
            }

        }
        catch (NumberFormatException E){
            System.err.println("Incorrect type for value of " +Ctx.int_.getText());
        }
        catch (RuntimeException E){
               System.err.println("Incorrect type for value of "+ E.getMessage());
        }

       }
       else if(ctx instanceof AddExprContext Ctx){
           try {
               var Cont = (AddExprContext) ctx;

               if(!Cont.op.getText().equals("+") && !Cont.op.getText().equals("-")){
                   throw new InvalidFormatException("Unexpected operator "+Cont.op.getText() + " applied to operators");
               }


               var leftList = Cont.left.children;
               var rightList = Cont.right.children;

               // checking if the variable has already been defined beforehand in the main function or a given literal cannot be parsed into an Integer, since the initial values regarding adding and multiplying only work with integers.
               if(!VariableValues.containsKey(Cont.left.getText()) &&!VariableValues.containsKey(Cont.right.getText()) && !isANumber(leftList.get(leftList.size()-1).getText())&& !isANumber(rightList.get(rightList.size()-1).getText())){

                   throw new NumberFormatException("Unexpected operand expression or undefined variable for " + Ctx.left.getText());
               }


           }
           catch (InvalidFormatException E){
               System.err.println(E.getMessage());
           }
           catch (NumberFormatException E){
               System.err.println("Unexpected operand expression or literal for " +Ctx.getText());
           }
           }
       else if(ctx instanceof MulExprContext Ctx)
       {
           try{
               var Cont = (MulExprContext) ctx;
               var op = Cont.op.getText();
               if(!op.equals("*") && !op.equals("/")){
                   throw new InvalidFormatException("Unexpected operator "+ Cont.op.getText() + " applied to operators");
               }

               var leftList = Cont.left.children;
               Integer.parseInt(leftList.get(leftList.size()-1).getText());
               var rightList = Cont.right.children;
               Integer.parseInt(rightList.get(rightList.size()-1).getText());
           }
           catch (InvalidFormatException E){
               System.err.println(E.getMessage());
           }
           catch (NumberFormatException E){
               System.err.println("Unexpected operand expression or literal for "+ Ctx.getText());
           }
       }

       }



    }


