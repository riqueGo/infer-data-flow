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
        if (nameMethodInvocation.isBlank() || node.getProperty(nameMethodInvocation) != null) {
            return false;
        }

        IVariableBinding nodeBinding = node.resolveBinding();
        if (nodeBinding != null && Modifier.isFinal(nodeBinding.getModifiers())) {
            return false;
        }

        Expression initializer = node.getInitializer();
        helper.wrapRightHandSide(node.getAST(), nameMethodInvocation, initializer);

        node.setProperty(nameMethodInvocation, true);
        return super.visit(node);
    }

    @Override
    public boolean visit(Assignment node) {
        String nameMethodInvocation = helper.getNameMethodInferWrapperInvocation(node);
        if (nameMethodInvocation.isBlank() || node.getProperty(nameMethodInvocation) != null) {
            return false;
        }

        helper.wrapNonAssignOperator(node, nameMethodInvocation);

        AST ast = node.getAST();
        helper.wrapRightHandSide(ast, nameMethodInvocation, node.getRightHandSide());
        helper.wrapLeftHandSide(ast, nameMethodInvocation, node.getLeftHandSide());

        node.setProperty(nameMethodInvocation, true);
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

        helper.wrapIfSimpleOrQualifiedName(node.getLeftOperand(), ast, nameMethodInvocation);
        helper.wrapIfSimpleOrQualifiedName(node.getRightOperand(), ast, nameMethodInvocation);

        for (Object extendedOperandObj : node.extendedOperands()) {
            helper.wrapIfSimpleOrQualifiedName((Expression) extendedOperandObj, ast, nameMethodInvocation);
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(ForStatement node) {
        String nameMethodInvocation = helper.getNameMethodInferWrapperInvocation(node);
        if (nameMethodInvocation.isBlank() || node.getProperty(nameMethodInvocation) != null) {
            return super.visit(node);
        }

        helper.wrapIfSimpleOrQualifiedName(node.getExpression(), node.getAST(), nameMethodInvocation);
        node.setProperty(nameMethodInvocation, true);
        return super.visit(node);
    }

    @Override
    public boolean visit(IfStatement node) {
        String nameMethodInvocation = helper.getNameMethodInferWrapperInvocation(node);
        if (nameMethodInvocation.isBlank() || node.getProperty(nameMethodInvocation) != null) {
            return super.visit(node);
        }

        helper.wrapIfSimpleOrQualifiedName(node.getExpression(), node.getAST(), nameMethodInvocation);
        node.setProperty(nameMethodInvocation, true);
        return super.visit(node);
    }

    @Override
    public boolean visit(WhileStatement node) {
        String nameMethodInvocation = helper.getNameMethodInferWrapperInvocation(node);
        if (nameMethodInvocation.isBlank() || node.getProperty(nameMethodInvocation) != null) {
            return super.visit(node);
        }

        helper.wrapIfSimpleOrQualifiedName(node.getExpression(), node.getAST(), nameMethodInvocation);
        node.setProperty(nameMethodInvocation, true);
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

        IMethodBinding binding = node.resolveMethodBinding();
        if (binding != null && !Modifier.isStatic(binding.getModifiers())) {
            helper.wrapOptionalExpression(node.getAST(), nameMethodInvocation, node.getExpression());
        }
        helper.wrapChainedMethodInvocation(node, nameMethodInvocation);
        return false;
    }

    @Override
    public boolean visit(MethodDeclaration node) {
        if (helper.getMethodDeclarationName().isBlank()) {
            return super.visit(node);
        }

        if(node.getName().toString().equals(helper.getMethodDeclarationName())) {
            IMethodBinding methodBinding = node.resolveBinding();

            ITypeBinding declaringClass = methodBinding.getDeclaringClass();
            if (declaringClass == null) { return false; }

            String classVisiting = declaringClass.getName();
            return helper.getCurrentClassName().equals(classVisiting);
        }

        return false;
    }

    @Override
    public boolean visit(ReturnStatement node) {
        String nameMethodInvocation = helper.getNameMethodInferWrapperInvocation(node);
        if (nameMethodInvocation.isBlank() || node.getProperty(nameMethodInvocation) != null) {
            return false;
        }

        Expression returnExpression = node.getExpression();
        if (returnExpression != null && !(returnExpression instanceof NullLiteral)) {
            MethodInvocation wrappedExpression = helper.wrapInferMethodInvocation(node.getAST(), nameMethodInvocation, returnExpression);
            inferGenerateCode.rewriterSet(node, ReturnStatement.EXPRESSION_PROPERTY, wrappedExpression, null);
        }

        node.setProperty(nameMethodInvocation, true);
        return super.visit(node);
    }

    @Override
    public boolean visit(ExpressionStatement node) {
        String nameMethodInvocation = helper.getNameMethodInferWrapperInvocation(node);
        if (nameMethodInvocation.isBlank() || node.getProperty(nameMethodInvocation) != null) {
            return false;
        }
        node.setProperty(nameMethodInvocation, true);
        return super.visit(node);
    }
}
