package dev.parsick.maven.rewrite.plexustestcase;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.Comparator;

import static org.openrewrite.NlsRewrite.Description;
import static org.openrewrite.NlsRewrite.DisplayName;

public class ReplacePlexusTestCase extends Recipe {
    @Override
    public @DisplayName String getDisplayName() {
        return "Replace PlexusTestCase by PlexusTest annotation";
    }

    @Override
    public @Description String getDescription() {
        return "Replace extending PlexusTestCase by adding a PlexusTest annotation.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new PlexusTestCaseVisitor();

    }

    private class PlexusTestCaseVisitor extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
            J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, executionContext);
            if (cd.getExtends()==null || !((JavaType.Class)cd.getExtends().getType()).getFullyQualifiedName().equals("org.codehaus.plexus.PlexusTestCase"))  {
                return cd;
            }

            cd = JavaTemplate.builder("@PlexusTest")
                    .javaParser(JavaParser.fromJavaVersion()
                            .classpath("plexus-testing"))
                    .imports("org.codehaus.plexus.testing.PlexusTest")
                    .build()
                    .apply(getCursor(), cd.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName)));
            maybeAddImport("org.codehaus.plexus.testing.PlexusTest"); // both imports methods are needed to add a new import

            cd = cd.withExtends(null);
            maybeRemoveImport("org.codehaus.plexus.PlexusTestCase");
            return cd;
        }
    }
}
