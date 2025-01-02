package infer;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.example.gitManager.CollectedMergeDataByFile;

import java.util.List;

import static infer.InferUtils.*;

public class InferVisitor extends ASTVisitor {
    private CollectedMergeDataByFile collectedMergeDataByFile;
    private CompilationUnit compilationUnit;
    private ASTRewrite rewriter;


    public InferVisitor(
            CollectedMergeDataByFile collectedMergeDataByFile,
            CompilationUnit compilationUnit,
            ASTRewrite rewriter
    ) {
        this.collectedMergeDataByFile = collectedMergeDataByFile;
        this.compilationUnit = compilationUnit;
        this.rewriter = rewriter;
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        collectedMergeDataByFile.addClassName(INFER_PACKAGE_NAME + "." + node.getName().getIdentifier());
        return super.visit(node);
    }

    @Override
    public boolean visit(VariableDeclarationFragment node) {
        String nameMethodInvocation = getNameMethodInferWrapperInvocation(node);
        if (nameMethodInvocation.isBlank()) {
            return super.visit(node);
        }

        Expression initializer = node.getInitializer();
        if (initializer != null && !(initializer instanceof ClassInstanceCreation || initializer instanceof MethodInvocation)) {
            MethodInvocation inferWrapper = wrapInferMethodInvocation(node.getAST(), nameMethodInvocation, initializer);
            rewriter.set(node, VariableDeclarationFragment.INITIALIZER_PROPERTY, inferWrapper, null);
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(Assignment node) {
        String nameMethodInvocation = getNameMethodInferWrapperInvocation(node);
        if (nameMethodInvocation.isBlank()) { return super.visit(node); }

        AST ast = node.getAST();

        Assignment.Operator operator = node.getOperator();
        if(operator == Assignment.Operator.ASSIGN) {
            MethodInvocation inferWrapper = wrapInferMethodInvocation(ast, nameMethodInvocation, node.getRightHandSide());
            rewriter.set(node, Assignment.RIGHT_HAND_SIDE_PROPERTY, inferWrapper, null);
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
            MethodInvocation inferWrapper = wrapInferMethodInvocation(ast, nameMethodInvocation, infixExpression);

            // Replace the original assignment with a standard assignment using the wrapped RHS
            Assignment newAssignment = ast.newAssignment();
            newAssignment.setLeftHandSide((Expression) ASTNode.copySubtree(ast, node.getLeftHandSide()));
            newAssignment.setRightHandSide(inferWrapper);
            newAssignment.setOperator(Assignment.Operator.ASSIGN);

            rewriter.replace(node, newAssignment, null);
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(PrefixExpression node) {
        String nameMethodInvocation = getNameMethodInferWrapperInvocation(node);
        if (nameMethodInvocation.isBlank()) { return super.visit(node); }

        if(node.getParent() instanceof ExpressionStatement expressionStatement) {
            AST ast = node.getAST();

            MethodInvocation inferWrapper = wrapInferMethodInvocation(node.getAST(), nameMethodInvocation, node);
            rewriter.replace(expressionStatement, ast.newExpressionStatement(inferWrapper), null);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(PostfixExpression node) {
        String nameMethodInvocation = getNameMethodInferWrapperInvocation(node);
        if (nameMethodInvocation.isBlank()) { return super.visit(node); }

        if(node.getParent() instanceof ExpressionStatement expressionStatement) {
            AST ast = node.getAST();

            MethodInvocation inferWrapper = wrapInferMethodInvocation(node.getAST(), nameMethodInvocation, node);
            rewriter.replace(expressionStatement, ast.newExpressionStatement(inferWrapper), null);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(InfixExpression node) {
        String nameMethodInvocation = getNameMethodInferWrapperInvocation(node);
        if (nameMethodInvocation.isBlank()) { return super.visit(node); }

        AST ast = node.getAST();

        if (node.getLeftOperand() instanceof SimpleName leftOperand) {
            MethodInvocation wrappedLeftOperand = wrapInferMethodInvocation(ast, nameMethodInvocation, leftOperand);
            rewriter.replace(leftOperand, wrappedLeftOperand, null);
        }

        if (node.getRightOperand() instanceof SimpleName rightOperand) {
            MethodInvocation wrappedRightOperand = wrapInferMethodInvocation(ast, nameMethodInvocation, rightOperand);
            rewriter.replace(rightOperand, wrappedRightOperand, null);
        }

        for (Object extendedOperandObj : node.extendedOperands()) {
            if (extendedOperandObj instanceof SimpleName extendedOperand) {
                MethodInvocation wrappedExtendedOperand = wrapInferMethodInvocation(ast, nameMethodInvocation, extendedOperand);
                rewriter.replace((ASTNode) extendedOperandObj, wrappedExtendedOperand, null);
            }
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(ForStatement node) {
        String nameMethodInvocation = getNameMethodInferWrapperInvocation(node);
        if (nameMethodInvocation.isBlank()) { return super.visit(node); }

        AST ast = node.getAST();

        for (Object initializerObj : node.initializers()) {
            if (!(initializerObj instanceof VariableDeclarationExpression variableDeclaration)) { continue; }
            for(Object fragmentObj : variableDeclaration.fragments()) {
                if (!(fragmentObj instanceof VariableDeclarationFragment fragment)) { continue; }

                Expression initializer = fragment.getInitializer();
                if (initializer != null) {
                    MethodInvocation inferWrapper = wrapInferMethodInvocation(node.getAST(), nameMethodInvocation, initializer);
                    rewriter.set(fragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, inferWrapper, null);
                }
            }
        }

        if(node.getExpression() instanceof SimpleName condition) {
            MethodInvocation inferWrapper = wrapInferMethodInvocation(ast, nameMethodInvocation, condition);
            rewriter.replace(condition, inferWrapper, null);
        }

        ListRewrite updateRewrite = rewriter.getListRewrite(node, ForStatement.UPDATERS_PROPERTY);
        for(Object updaterObj : node.updaters()) {
            if(updaterObj instanceof Expression updater) {
                MethodInvocation inferWrapper = wrapInferMethodInvocation(ast, nameMethodInvocation, updater);
                updateRewrite.replace(updater, inferWrapper, null);
            }
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(ClassInstanceCreation node) {
        String nameMethodInvocation = getNameMethodInferWrapperInvocation(node);
        if (nameMethodInvocation.isBlank()) {
            return super.visit(node);
        }

        wrapArguments(node, nameMethodInvocation, node.arguments());

        return false;
    }

    @Override
    public boolean visit(MethodInvocation node) {
        String nameMethodInvocation = getNameMethodInferWrapperInvocation(node);
        if (nameMethodInvocation.isBlank()) {
            return super.visit(node);
        }

        // Wrap nested MethodInvocation arguments first
        wrapArguments(node, nameMethodInvocation, node.arguments());
        // Replace rewritten arguments if necessary
        updateArguments(node.arguments());

        AST ast = node.getAST();

        IMethodBinding methodBinding = node.resolveMethodBinding();
        if (methodBinding == null) {
            return false;
        }

        // Wrap the MethodInvocation itself if it's not void
        boolean isVoid = methodBinding.getReturnType().isPrimitive() && "void".equals(methodBinding.getReturnType().getName());
        if (!isVoid) {
            MethodInvocation wrappedMethod = wrapInferMethodInvocation(ast, nameMethodInvocation, node);

            if (node.getParent() instanceof ExpressionStatement expressionStatement) {
                rewriter.replace(expressionStatement, ast.newExpressionStatement(wrappedMethod), null);
            } else {
                rewriter.replace(node, wrappedMethod, null);
            }
        }

        return false;
    }

    private String getNameMethodInferWrapperInvocation(ASTNode node) {
//        int nodeLine = compilationUnit.getLineNumber(node.getStartPosition());
//        return collectedMergeDataByFile.getWhoChangedTheLine(nodeLine);
        return "left";
    }

    private MethodInvocation wrapInferMethodInvocation(AST ast, String nameMethodInvocation, Expression expression) {
        MethodInvocation methodInvocation = ast.newMethodInvocation();
        methodInvocation.setExpression(ast.newSimpleName(WRAPPER_CLASS_NAME));
        methodInvocation.setName(ast.newSimpleName(nameMethodInvocation));
        methodInvocation.arguments().add(ASTNode.copySubtree(ast, expression));
        return methodInvocation;
    }

    private void wrapArguments(Expression node, String nameMethodInvocation, List<Expression> arguments) {
        for (Expression argument : arguments) {
            if (argument instanceof ClassInstanceCreation nestedInstanceCreation) { // Recursive wrapping for nested ClassInstanceCreation
                wrapArguments(nestedInstanceCreation, nameMethodInvocation, nestedInstanceCreation.arguments());
                updateArguments(nestedInstanceCreation.arguments());
                continue;
            } else if (argument instanceof MethodInvocation nestedInvocation) { // Recursive wrapping for nested MethodInvocation
                wrapArguments(nestedInvocation, nameMethodInvocation, nestedInvocation.arguments());
                updateArguments(nestedInvocation.arguments());
            }

            //Wrap the argument
            MethodInvocation wrappedArgument = wrapInferMethodInvocation(node.getAST(), nameMethodInvocation, argument);
            rewriter.replace(argument, wrappedArgument, null);
            argument.setProperty(REWRITTEN_PROPERTY, wrappedArgument);
        }
    }

    private void updateArguments(List<Expression> arguments) {
        for (int i = 0; i < arguments.size(); i++) {
            Expression argument = arguments.get(i);
            Expression argumentUpdated = (Expression) argument.getProperty(REWRITTEN_PROPERTY);
            if (argumentUpdated != null) {
                arguments.set(i, argumentUpdated);
            }
        }
    }
}
