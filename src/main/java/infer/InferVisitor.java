package infer;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

public class InferVisitor extends ASTVisitor {
    private final InferGenerateCode inferGenerateCode;
    private final InferVisitorHelper helper;

    public InferVisitor (InferGenerateCode inferGenerateCode, InferVisitorHelper helper) {
        this.inferGenerateCode = inferGenerateCode;
        this.helper = helper;
    }

    @Override
    public boolean visit(VariableDeclarationFragment node) {
        String nameMethodInvocation = helper.getNameMethodInferWrapperInvocation(node);
        if (nameMethodInvocation.isBlank()) {
            return super.visit(node);
        }

        Expression initializer = node.getInitializer();
        if (initializer != null && !(initializer instanceof ClassInstanceCreation || initializer instanceof MethodInvocation)) {
            MethodInvocation inferWrapper = helper.wrapInferMethodInvocation(node.getAST(), nameMethodInvocation, initializer);
            inferGenerateCode.rewriterSet(node, VariableDeclarationFragment.INITIALIZER_PROPERTY, inferWrapper, null);
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(Assignment node) {
        String nameMethodInvocation = helper.getNameMethodInferWrapperInvocation(node);
        if (nameMethodInvocation.isBlank()) { return super.visit(node); }

        AST ast = node.getAST();

        Assignment.Operator operator = node.getOperator();
        if(operator == Assignment.Operator.ASSIGN) {
            MethodInvocation inferWrapper = helper.wrapInferMethodInvocation(ast, nameMethodInvocation, node.getRightHandSide());
            inferGenerateCode.rewriterSet(node, Assignment.RIGHT_HAND_SIDE_PROPERTY, inferWrapper, null);
        } else {
            // Create the new right-hand side
            InfixExpression infixExpression = ast.newInfixExpression();
            infixExpression.setLeftOperand((Expression) ASTNode.copySubtree(ast, node.getLeftHandSide()));
            infixExpression.setRightOperand((Expression) ASTNode.copySubtree(ast, node.getRightHandSide()));

            switch (operator.toString()) {
                case "+=" -> infixExpression.setOperator(InfixExpression.Operator.PLUS);
                case "-=" -> infixExpression.setOperator(InfixExpression.Operator.MINUS);
                case "*=" -> infixExpression.setOperator(InfixExpression.Operator.TIMES);
                case "/=" -> infixExpression.setOperator(InfixExpression.Operator.DIVIDE);
                case "%=" -> infixExpression.setOperator(InfixExpression.Operator.REMAINDER);
                default -> { return super.visit(node); }
            }

            // Wrap the new right-hand side in InferWrapper
            MethodInvocation inferWrapper = helper.wrapInferMethodInvocation(ast, nameMethodInvocation, infixExpression);

            // Replace the original assignment with a standard assignment using the wrapped RHS
            Assignment newAssignment = ast.newAssignment();
            newAssignment.setLeftHandSide((Expression) ASTNode.copySubtree(ast, node.getLeftHandSide()));
            newAssignment.setRightHandSide(inferWrapper);
            newAssignment.setOperator(Assignment.Operator.ASSIGN);

            inferGenerateCode.rewriterReplace(node, newAssignment, null);
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(PrefixExpression node) {
        String nameMethodInvocation = helper.getNameMethodInferWrapperInvocation(node);
        if (nameMethodInvocation.isBlank()) { return super.visit(node); }

        if(node.getParent() instanceof ExpressionStatement expressionStatement) {
            AST ast = node.getAST();

            MethodInvocation inferWrapper = helper.wrapInferMethodInvocation(node.getAST(), nameMethodInvocation, node);
            inferGenerateCode.rewriterReplace(expressionStatement, ast.newExpressionStatement(inferWrapper), null);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(PostfixExpression node) {
        String nameMethodInvocation = helper.getNameMethodInferWrapperInvocation(node);
        if (nameMethodInvocation.isBlank()) { return super.visit(node); }

        if(node.getParent() instanceof ExpressionStatement expressionStatement) {
            AST ast = node.getAST();

            MethodInvocation inferWrapper = helper.wrapInferMethodInvocation(node.getAST(), nameMethodInvocation, node);
            inferGenerateCode.rewriterReplace(expressionStatement, ast.newExpressionStatement(inferWrapper), null);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(InfixExpression node) {
        String nameMethodInvocation = helper.getNameMethodInferWrapperInvocation(node);
        if (nameMethodInvocation.isBlank()) { return super.visit(node); }

        AST ast = node.getAST();

        if (node.getLeftOperand() instanceof SimpleName leftOperand) {
            MethodInvocation wrappedLeftOperand = helper.wrapInferMethodInvocation(ast, nameMethodInvocation, leftOperand);
            inferGenerateCode.rewriterReplace(leftOperand, wrappedLeftOperand, null);
        }

        if (node.getRightOperand() instanceof SimpleName rightOperand) {
            MethodInvocation wrappedRightOperand = helper.wrapInferMethodInvocation(ast, nameMethodInvocation, rightOperand);
            inferGenerateCode.rewriterReplace(rightOperand, wrappedRightOperand, null);
        }

        for (Object extendedOperandObj : node.extendedOperands()) {
            if (extendedOperandObj instanceof SimpleName extendedOperand) {
                MethodInvocation wrappedExtendedOperand = helper.wrapInferMethodInvocation(ast, nameMethodInvocation, extendedOperand);
                inferGenerateCode.rewriterReplace((ASTNode) extendedOperandObj, wrappedExtendedOperand, null);
            }
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(ForStatement node) {
        String nameMethodInvocation = helper.getNameMethodInferWrapperInvocation(node);
        if (nameMethodInvocation.isBlank()) { return super.visit(node); }

        AST ast = node.getAST();

        for (Object initializerObj : node.initializers()) {
            if (!(initializerObj instanceof VariableDeclarationExpression variableDeclaration)) { continue; }
            for(Object fragmentObj : variableDeclaration.fragments()) {
                if (!(fragmentObj instanceof VariableDeclarationFragment fragment)) { continue; }

                Expression initializer = fragment.getInitializer();
                if (initializer != null) {
                    MethodInvocation inferWrapper = helper.wrapInferMethodInvocation(node.getAST(), nameMethodInvocation, initializer);
                    inferGenerateCode.rewriterSet(fragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, inferWrapper, null);
                }
            }
        }

        if(node.getExpression() instanceof SimpleName condition) {
            MethodInvocation inferWrapper = helper.wrapInferMethodInvocation(ast, nameMethodInvocation, condition);
            inferGenerateCode.rewriterReplace(condition, inferWrapper, null);
        }

        ListRewrite updateRewrite = inferGenerateCode.getListRewrite(node, ForStatement.UPDATERS_PROPERTY);
        for(Object updaterObj : node.updaters()) {
            if(updaterObj instanceof Expression updater) {
                MethodInvocation inferWrapper = helper.wrapInferMethodInvocation(ast, nameMethodInvocation, updater);
                updateRewrite.replace(updater, inferWrapper, null);
            }
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(ClassInstanceCreation node) {
        String nameMethodInvocation = helper.getNameMethodInferWrapperInvocation(node);
        if (nameMethodInvocation.isBlank()) {
            return super.visit(node);
        }

        helper.wrapClassIntanceCreation(node, nameMethodInvocation);
        return false;
    }

    @Override
    public boolean visit(MethodInvocation node) {
        String nameMethodInvocation = helper.getNameMethodInferWrapperInvocation(node);
        if (nameMethodInvocation.isBlank()) {
            return super.visit(node);
        }

        helper.wrapMethodInvocation(node, nameMethodInvocation);
        return false;
    }
}
