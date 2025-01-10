package infer;

import org.eclipse.jdt.core.dom.ASTNode;

import java.util.function.Function;

public class InferInterproceduralMethodVisitorHelper extends InferVisitorHelper{
    private String methodVisiting;
    private String developer;

    public InferInterproceduralMethodVisitorHelper(
            InferGenerateCode inferGenerateCode,
            int depth,
            String methodVisiting,
            String developer
    ) {
        super(inferGenerateCode, depth);
        this.methodVisiting = methodVisiting;
        this.developer = developer;
    }

    public String getMethodVisiting() {
        return methodVisiting;
    }

    public String getDeveloper() {
        return developer;
    }

    @Override
    public String getNameMethodInferWrapperInvocation(ASTNode node) {
        return developer;
    }
}
