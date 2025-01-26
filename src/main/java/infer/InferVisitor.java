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
        if (initializer != null && !(initializer instanceof MethodInvocation || initializer instanceof ClassInstanceCreation || initializer instanceof NullLiteral)) {
            MethodInvocation inferWrapper = helper.wrapInferMethodInvocation(node.getAST(), nameMethodInvocation, initializer);
            inferGenerateCode.rewriterSet(node, VariableDeclarationFragment.INITIALIZER_PROPERTY, inferWrapper, null);
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(Assignment node) {
        String nameMethodInvocation = helper.getNameMethodInferWrapperInvocation(node);
        if (nameMethodInvocation.isBlank()) { return super.visit(node); }

        helper.wrapNonAssignOperator(node, nameMethodInvocation);

        AST ast = node.getAST();
        Expression rhs = node.getRightHandSide();

        if (!(rhs instanceof MethodInvocation || rhs instanceof ClassInstanceCreation || rhs instanceof NullLiteral)) {
            MethodInvocation inferWrapper = helper.wrapInferMethodInvocation(ast, nameMethodInvocation, rhs);
            inferGenerateCode.rewriterSet(node, Assignment.RIGHT_HAND_SIDE_PROPERTY, inferWrapper, null);
        }

        helper.wrapLeftHandSide(ast, nameMethodInvocation, node.getLeftHandSide());
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
        if (!(node.getParent() instanceof IfStatement || node.getParent() instanceof WhileStatement || node.getParent() instanceof ForStatement || node.getParent() instanceof InfixExpression)) {
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
