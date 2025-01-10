package infer;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;

public class InferInterproceduralMethodVisitor extends ASTVisitor {

    private final InferInterproceduralMethodVisitorHelper helper;

    public InferInterproceduralMethodVisitor (InferInterproceduralMethodVisitorHelper helper) {
        this.helper = helper;
    }

    @Override
    public boolean visit(MethodInvocation node) {
        helper.wrapMethodInvocation(node, helper.getDeveloper());
        return false;
    }

    @Override
    public boolean visit(MethodDeclaration node) {
        return node.getName().toString().equals(helper.getMethodVisiting());
    }
}
