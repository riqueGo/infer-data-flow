package infer;

import org.eclipse.jdt.core.dom.*;
import java.util.List;
import java.util.function.Function;

import static infer.InferConstants.*;
import static infer.InferGenerateManagement.PROJECT_PATH;
import static org.example.utils.PathToString.getPath;

public class InferVisitorHelper {
    private final InferGenerateCode inferGenerateCode;
    private Function<Integer, String> getWhoChangedTheLine;
    private final int depth;
    private final String currentClassName;
    private final String methodDeclarationName;

    public InferVisitorHelper(InferGenerateCode inferGenerateCode, Function<Integer, String> getWhoChangedTheLine, int depth) {
        this.inferGenerateCode = inferGenerateCode;
        this.getWhoChangedTheLine = getWhoChangedTheLine;
        this.depth = depth;
        this.currentClassName = "";
        this.methodDeclarationName = "";
    }

    public InferVisitorHelper(InferGenerateCode inferGenerateCode, Function<Integer, String> getWhoChangedTheLine, int depth, String currentClassName, String methodDeclarationName) {
        this.inferGenerateCode = inferGenerateCode;
        this.getWhoChangedTheLine = getWhoChangedTheLine;
        this.depth = depth;
        this.currentClassName = currentClassName;
        this.methodDeclarationName = methodDeclarationName;
    }

    public int getDepth() { return depth; }

    public String getMethodDeclarationName() {
        return methodDeclarationName;
    }

    public String getCurrentClassName() { return currentClassName; }

    public String getNameMethodInferWrapperInvocation(ASTNode node) {
        return getWhoChangedTheLine.apply(inferGenerateCode.getLineNumber(node.getStartPosition()));
    }

    public MethodInvocation wrapInferMethodInvocation(AST ast, String nameMethodInvocation, Expression expression) {
        MethodInvocation methodInvocation = ast.newMethodInvocation();
        methodInvocation.setExpression(ast.newSimpleName(WRAPPER_CLASS_NAME));
        methodInvocation.setName(ast.newSimpleName(nameMethodInvocation));
        methodInvocation.arguments().add(ASTNode.copySubtree(ast, expression));
        return methodInvocation;
    }

    public void wrapArguments(Expression node, String nameMethodInvocation, List<Expression> arguments) {
        for (Expression argument : arguments) {
            if (argument instanceof ClassInstanceCreation nestedInstanceCreation) { // Recursive wrapping for nested ClassInstanceCreation
                wrapClassIntanceCreation(nestedInstanceCreation, nameMethodInvocation);
            } else if (argument instanceof MethodInvocation nestedInvocation) { // Recursive wrapping for nested MethodInvocation
                wrapMethodInvocation(nestedInvocation, nameMethodInvocation);
            } else if (argument instanceof SimpleName){
                MethodInvocation wrappedArgument = wrapInferMethodInvocation(node.getAST(), nameMethodInvocation, argument);
                inferGenerateCode.rewriterReplace(argument, wrappedArgument, null);
                argument.setProperty(REWRITTEN_PROPERTY, wrappedArgument);
            }
        }
    }

    public void updateArguments(List<Expression> arguments) {
        for (int i = 0; i < arguments.size(); i++) {
            Expression argument = arguments.get(i);
            Expression argumentUpdated = (Expression) argument.getProperty(REWRITTEN_PROPERTY);
            if (argumentUpdated != null) {
                arguments.set(i, argumentUpdated);
            }
        }
    }

    public void wrapMethodInvocation(MethodInvocation node, String nameMethodInvocation) {
        if(hasMethodAlreadyWrapped(node, nameMethodInvocation)) {
            return;
        }

        wrapArguments(node, nameMethodInvocation, node.arguments());
        updateArguments(node.arguments());

        if (node.getParent() instanceof VariableDeclarationFragment || node.getParent() instanceof Assignment) {
            MethodInvocation inferWrapper = wrapInferMethodInvocation(node.getAST(), nameMethodInvocation, node);
            inferGenerateCode.rewriterReplace(node, inferWrapper, null);
        }
        interproceduralVisiting(node.resolveMethodBinding(), node.getName().toString(), nameMethodInvocation);
    }

    public void wrapClassIntanceCreation(ClassInstanceCreation node, String nameMethodInvocation) {
        wrapArguments(node, nameMethodInvocation, node.arguments());
        updateArguments(node.arguments());

        if (node.getParent() instanceof VariableDeclarationFragment || node.getParent() instanceof Assignment) {
            MethodInvocation inferWrapper = wrapInferMethodInvocation(node.getAST(), nameMethodInvocation, node);
            inferGenerateCode.rewriterReplace(node, inferWrapper, null);
        }
        interproceduralVisiting(node.resolveConstructorBinding(), node.getType().toString(), nameMethodInvocation);
    }

    public void wrapChainedMethodInvocation(MethodInvocation node, String nameMethodInvocation) {
        // Check if the current method invocation is part of a chain
        Expression expression = node.getExpression();
        if (expression instanceof MethodInvocation) {
            wrapChainedMethodInvocation((MethodInvocation) expression, nameMethodInvocation);
        } else if (expression instanceof ClassInstanceCreation) {
            wrapClassIntanceCreation((ClassInstanceCreation) expression, nameMethodInvocation);
        }
        wrapMethodInvocation(node, nameMethodInvocation);
    }

    public void interproceduralVisiting(IMethodBinding methodBinding, String methodDeclarationName, String nameMethodInvocation) {
        if (methodBinding == null) { return; }

        ITypeBinding declaringClass = methodBinding.getDeclaringClass();
        if (declaringClass == null) { return; }

        String classVisiting = declaringClass.getName();

        ITypeBinding outerClass = getOutermostClass(declaringClass);
        String qualifiedName = outerClass.getQualifiedName();

        String sourceFilePath = qualifiedName.replace('.', '/') + ".java";
        String filePathAnalysing = getPath(PROJECT_PATH, SOURCE_PROJECT_PATH, sourceFilePath);

        InferGenerate inferGenerate = new InferGenerate(PROJECT_PATH);
        inferGenerate.generateInferInterproceduralCode(filePathAnalysing, classVisiting, methodDeclarationName, nameMethodInvocation, depth-1);
    }

    public void wrapIfSimpleName(Expression expression, AST ast, String nameMethodInvocation) {
        if(expression instanceof SimpleName simpleName) {
            MethodInvocation inferWrapper = wrapInferMethodInvocation(ast, nameMethodInvocation, simpleName);
            inferGenerateCode.rewriterReplace(simpleName, inferWrapper, null);
        }
    }

    private ITypeBinding getOutermostClass(ITypeBinding typeBinding) {
        ITypeBinding current = typeBinding;
        while (current.getDeclaringClass() != null) {
            current = current.getDeclaringClass();
        }
        return current;
    }

    public void wrapLeftHandSide(AST ast, String nameMethodInvocation, Expression lhs) {
        if (lhs instanceof QualifiedName qualifiedName) {
            Expression base = qualifiedName.getQualifier();
            MethodInvocation inferWrapper = wrapInferMethodInvocation(ast, nameMethodInvocation, base);
            inferGenerateCode.rewriterReplace(base, inferWrapper, null);
        } else if (lhs instanceof FieldAccess fieldAccess) {
            Expression base = fieldAccess.getExpression();
            MethodInvocation inferWrapper = wrapInferMethodInvocation(ast, nameMethodInvocation, base);
            inferGenerateCode.rewriterReplace(base, inferWrapper, null);
        }
    }

    public void wrapNonAssignOperator(Assignment node, String nameMethodInvocation) {
        AST ast = node.getAST();

        Assignment.Operator operator = node.getOperator();
        Expression lhs = node.getLeftHandSide(), rhs = node.getRightHandSide();

        if (operator == Assignment.Operator.ASSIGN) {
            return;
        }

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
            default -> {
                return;
            }
        }

        // Wrap the new right-hand side in InferWrapper
        MethodInvocation inferWrapper = wrapInferMethodInvocation(ast, nameMethodInvocation, infixExpression);

        // Replace the original assignment with a standard assignment using the wrapped RHS
        Assignment newAssignment = ast.newAssignment();
        newAssignment.setLeftHandSide((Expression) ASTNode.copySubtree(ast, lhs));
        newAssignment.setRightHandSide(inferWrapper);
        newAssignment.setOperator(Assignment.Operator.ASSIGN);

        inferGenerateCode.rewriterReplace(node, newAssignment, null);
    }

    private Boolean hasMethodAlreadyWrapped(MethodInvocation node, String inferMethodInvocation) {
        String expressionName = node.getExpression() == null ? "" : node.getExpression().toString();
        String nameMethodCall = expressionName + "." + node.getName().toString();

        String transformedInferMethodCall = WRAPPER_CLASS_NAME + "." + inferMethodInvocation;
        return transformedInferMethodCall.equals(nameMethodCall);
    }
}
