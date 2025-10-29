package dev.parsick.maven.rewrite.abstractmojotestcase;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.NlsRewrite;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;

import java.util.Comparator;
import java.util.List;

public class MigrateMojoParameter extends Recipe {
    @Override
    public @NlsRewrite.DisplayName String getDisplayName() {
        return "Migrate to MojoParameter annotations";
    }

    @Override
    public @NlsRewrite.Description String getDescription() {
        return "Searching for setVariableValueToObject methods and migrate them to MojoParameter annotations.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MigrateMojoParameterVisitor();



    }

    private class MigrateMojoParameterVisitor extends JavaIsoVisitor<ExecutionContext> {

        private static final String FULLY_QUALIFIED_NAME_MOJO_PARAMETER = "org.apache.maven.api.plugin.testing.MojoParameter";
        private static final String METHOD_FOR_MOJO_PARAMETER = "setVariableValueToObject";

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
            if(!extendsAbstractMojoTestCase()){
                return method;
            }
            J.MethodDeclaration md = method;
            List<J.MethodInvocation> setVariableValueToObjectMethods = findSetVariableValueToObjectMethod(md);
            if(!setVariableValueToObjectMethods.isEmpty()) {
                for (J.MethodInvocation mi : setVariableValueToObjectMethods) {
                    var nameParam = mi.getArguments().get(1);
                    var valueParam = mi.getArguments().get(2);

                    String newAnnotationCode = String.format("""
                        @MojoParameter(name="%s", value="%s")""", nameParam, valueParam);
                    md = JavaTemplate.builder(newAnnotationCode)
                            .javaParser(JavaParser.fromJavaVersion().classpath("maven-plugin-testing-harness"))
                            .imports(FULLY_QUALIFIED_NAME_MOJO_PARAMETER)
                            .build().apply(updateCursor(md), md.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName)));
                }
                maybeAddImport(FULLY_QUALIFIED_NAME_MOJO_PARAMETER); // both imports methods are needed to add a new import
            }
            return super.visitMethodDeclaration(md, executionContext);
        }

        private boolean extendsAbstractMojoTestCase() {
            J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
            return classDecl.getExtends() != null && classDecl.getExtends().getType().toString().equals("org.apache.maven.plugin.testing.AbstractMojoTestCase");
        }

        private @NotNull List<J.MethodInvocation> findSetVariableValueToObjectMethod(J.MethodDeclaration md) {
            return md.getBody().getStatements().stream()
                    .filter(statement -> statement instanceof J.MethodInvocation)
                    .map(statement -> (J.MethodInvocation) statement)
                    .filter(mi -> mi.getSimpleName().equals(METHOD_FOR_MOJO_PARAMETER))
                    .toList();
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
            J.MethodInvocation mi = super.visitMethodInvocation(method, executionContext);
            if(mi.getSimpleName().equals(METHOD_FOR_MOJO_PARAMETER)) {
                return null;  // delete line with lookup
            }
            return mi;
        }
    }
}
