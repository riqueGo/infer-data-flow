package infer;

import org.eclipse.jdt.core.dom.*;

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
        if (initializer != null && !(initializer instanceof MethodInvocation || initializer instanceof ClassInstanceCreation)) {
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
        Expression lhs = node.getLeftHandSide(), rhs = node.getRightHandSide();

        if(operator == Assignment.Operator.ASSIGN && !(rhs instanceof MethodInvocation || rhs instanceof ClassInstanceCreation)) {
            MethodInvocation inferWrapper = helper.wrapInferMethodInvocation(ast, nameMethodInvocation, rhs);
            inferGenerateCode.rewriterSet(node, Assignment.RIGHT_HAND_SIDE_PROPERTY, inferWrapper, null);
        } else {
            // Create the new right-hand side
            InfixExpression infixExpression = ast.newInfixExpression();
            infixExpression.setLeftOperand((Expression) ASTNode.copySubtree(ast, lhs));
            infixExpression.setRightOperand((Expression) ASTNode.copySubtree(ast, rhs));

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
            newAssignment.setLeftHandSide((Expression) ASTNode.copySubtree(ast, lhs));
            newAssignment.setRightHandSide(inferWrapper);
            newAssignment.setOperator(Assignment.Operator.ASSIGN);

            inferGenerateCode.rewriterReplace(node, newAssignment, null);
        }

        if (lhs instanceof QualifiedName qualifiedName) {
            Expression base = qualifiedName.getQualifier();
            MethodInvocation inferWrapper = helper.wrapInferMethodInvocation(ast, nameMethodInvocation, base);
            inferGenerateCode.rewriterReplace(base, inferWrapper, null);
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(PrefixExpression node) {
        String nameMethodInvocation = helper.getNameMethodInferWrapperInvocation(node);
        if (nameMethodInvocation.isBlank()) { return super.visit(node); }

        AST ast = node.getAST();
        MethodInvocation inferWrapper = helper.wrapInferMethodInvocation(node.getAST(), nameMethodInvocation, node);

        if(node.getParent() instanceof ExpressionStatement expressionStatement) {
            inferGenerateCode.rewriterReplace(expressionStatement, ast.newExpressionStatement(inferWrapper), null);
        } else {
            inferGenerateCode.rewriterReplace(node, inferWrapper, null);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(PostfixExpression node) {
        String nameMethodInvocation = helper.getNameMethodInferWrapperInvocation(node);
        if (nameMethodInvocation.isBlank()) { return super.visit(node); }

        AST ast = node.getAST();
        MethodInvocation inferWrapper = helper.wrapInferMethodInvocation(node.getAST(), nameMethodInvocation, node);

        if(node.getParent() instanceof ExpressionStatement expressionStatement) {
            inferGenerateCode.rewriterReplace(expressionStatement, ast.newExpressionStatement(inferWrapper), null);
        } else {
            inferGenerateCode.rewriterReplace(node, inferWrapper, null);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(InfixExpression node) {
        if (!(node.getParent() instanceof IfStatement || node.getParent() instanceof WhileStatement || node.getParent() instanceof ForStatement)) {
            return super.visit(node);
        }

        String nameMethodInvocation = helper.getNameMethodInferWrapperInvocation(node);
        if (nameMethodInvocation.isBlank()) { return super.visit(node); }

        AST ast = node.getAST();

        helper.wrapIfSimpleName(node.getLeftOperand(), ast, nameMethodInvocation);
        helper.wrapIfSimpleName(node.getRightOperand(), ast, nameMethodInvocation);

        for (Object extendedOperandObj : node.extendedOperands()) {
            helper.wrapIfSimpleName((Expression) extendedOperandObj, ast, nameMethodInvocation);
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(ForStatement node) {
        String nameMethodInvocation = helper.getNameMethodInferWrapperInvocation(node);
        if (!nameMethodInvocation.isBlank()) {
            helper.wrapIfSimpleName(node.getExpression(), node.getAST(), nameMethodInvocation);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(IfStatement node) {
        String nameMethodInvocation = helper.getNameMethodInferWrapperInvocation(node);
        if (!nameMethodInvocation.isBlank()) {
            helper.wrapIfSimpleName(node.getExpression(), node.getAST(), nameMethodInvocation);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(WhileStatement node) {
        String nameMethodInvocation = helper.getNameMethodInferWrapperInvocation(node);
        if (!nameMethodInvocation.isBlank()) {
            helper.wrapIfSimpleName(node.getExpression(), node.getAST(), nameMethodInvocation);
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

    @Override
    public boolean visit(MethodDeclaration node) {
        if (helper.getMethodDeclarationName().isBlank()) {
            return super.visit(node);
        }
        return node.getName().toString().equals(helper.getMethodDeclarationName());
    }

    @Override
    public boolean visit(ReturnStatement node) {
        String nameMethodInvocation = helper.getNameMethodInferWrapperInvocation(node);
        if (nameMethodInvocation.isBlank()) {
            return super.visit(node);
        }

        Expression returnExpression = node.getExpression();
        if (returnExpression != null) {
            MethodInvocation wrappedExpression = helper.wrapInferMethodInvocation(node.getAST(), nameMethodInvocation, returnExpression);
            inferGenerateCode.rewriterSet(node, ReturnStatement.EXPRESSION_PROPERTY, wrappedExpression, null);
        }

        return super.visit(node);
    }
}
