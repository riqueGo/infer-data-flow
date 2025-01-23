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
    private final String methodVisiting;

    public InferVisitorHelper(InferGenerateCode inferGenerateCode, Function<Integer, String> getWhoChangedTheLine, int depth) {
        this.inferGenerateCode = inferGenerateCode;
        this.getWhoChangedTheLine = getWhoChangedTheLine;
        this.depth = depth;
        this.methodVisiting = "";
    }

    public InferVisitorHelper(InferGenerateCode inferGenerateCode, Function<Integer, String> getWhoChangedTheLine, int depth, String methodVisiting) {
        this.inferGenerateCode = inferGenerateCode;
        this.getWhoChangedTheLine = getWhoChangedTheLine;
        this.depth = depth;
        this.methodVisiting = methodVisiting;
    }

    public int getDepth() { return depth; }

    public String getMethodVisiting() {
        return methodVisiting;
    }

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
        wrapArguments(node, nameMethodInvocation, node.arguments());
        updateArguments(node.arguments());

        IMethodBinding methodBinding = node.resolveMethodBinding();
        if (methodBinding == null) {return;}

        // Wrap the MethodInvocation itself if it's not void
        boolean isVoid = methodBinding.getReturnType().isPrimitive() && "void".equals(methodBinding.getReturnType().getName());
        if (!isVoid) {
            AST ast = node.getAST();
            MethodInvocation inferWrapper = wrapInferMethodInvocation(ast, nameMethodInvocation, node);

            if (node.getParent() instanceof ExpressionStatement expressionStatement) {
                inferGenerateCode.rewriterReplace(expressionStatement, ast.newExpressionStatement(inferWrapper), null);
            } else {
                inferGenerateCode.rewriterReplace(node, inferWrapper, null);
                node.setProperty(REWRITTEN_PROPERTY, inferWrapper);
            }
        }

        ITypeBinding declaringClass = methodBinding.getDeclaringClass();
        if (declaringClass == null) {
            return;
        }

        String qualifiedName = declaringClass.getQualifiedName();
        String sourceFilePath = qualifiedName.replace('.', '/') + ".java";
        String filePathAnalysing = getPath(PROJECT_PATH, SOURCE_PROJECT_PATH, sourceFilePath);
        String methodName = node.getName().toString();

        InferGenerate inferGenerate = new InferGenerate(PROJECT_PATH);
        inferGenerate.generateInferInterproceduralMethodCode(filePathAnalysing, methodName, nameMethodInvocation, depth-1);
    }

    public void wrapClassIntanceCreation(ClassInstanceCreation node, String nameMethodInvocation) {
        wrapArguments(node, nameMethodInvocation, node.arguments());
        updateArguments(node.arguments());

        MethodInvocation inferWrapper = wrapInferMethodInvocation(node.getAST(), nameMethodInvocation, node);
        inferGenerateCode.rewriterReplace(node, inferWrapper, null);
        node.setProperty(REWRITTEN_PROPERTY, inferWrapper);
    }

    public void wrapIfSimpleName(Expression expression, AST ast, String nameMethodInvocation) {
        if(expression instanceof SimpleName simpleName) {
            MethodInvocation inferWrapper = wrapInferMethodInvocation(ast, nameMethodInvocation, simpleName);
            inferGenerateCode.rewriterReplace(simpleName, inferWrapper, null);
        }
    }
}
