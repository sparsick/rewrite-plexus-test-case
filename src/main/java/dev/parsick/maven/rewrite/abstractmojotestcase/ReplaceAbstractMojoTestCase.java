package dev.parsick.maven.rewrite.abstractmojotestcase;

import org.openrewrite.ExecutionContext;
import org.openrewrite.NlsRewrite;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.Comparator;

public class ReplaceAbstractMojoTestCase extends Recipe {
    @Override
    public @NlsRewrite.DisplayName String getDisplayName() {
        return "";
    }

    @Override
    public @NlsRewrite.Description String getDescription() {
        return ".";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ReplaceAbstractMojoTestCase.AbstractMojoTestCaseVisitor();

    }

    private class AbstractMojoTestCaseVisitor extends JavaIsoVisitor<ExecutionContext> {



        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
            J.ClassDeclaration cd = classDecl;
            if (cd.getExtends()==null || !((JavaType.Class)cd.getExtends().getType()).getFullyQualifiedName().equals("org.apache.maven.plugin.testing.AbstractMojoTestCase"))  {
                return cd;
            }

            cd = JavaTemplate.builder("@MojoTest")
                    .javaParser(JavaParser.fromJavaVersion()
                            .classpath("maven-plugin-testing-harness"))
                    .imports("org.apache.maven.api.plugin.testing.MojoTest")
                    .build()
                    .apply(getCursor(), cd.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName)));
            maybeAddImport("org.apache.maven.api.plugin.testing.MojoTest"); // both imports methods are needed to add a new import

            cd = cd.withExtends(null);
            maybeRemoveImport("org.apache.maven.plugin.testing.AbstractMojoTestCase");

            return cd;
        }





    }
}
