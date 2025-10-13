package dev.parsick.maven.rewrite.plexustestcase;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import static org.openrewrite.NlsRewrite.Description;
import static org.openrewrite.NlsRewrite.DisplayName;

public class RemovePlexusTestCase extends Recipe {
    @Override
    public @DisplayName String getDisplayName() {
        return "Remove extending PlexusTestCase";
    }

    @Override
    public @Description String getDescription() {
        return "Remove extending PlexusTestCase.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new PlexusTestCaseVisitor();

    }

    private class PlexusTestCaseVisitor extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
            super.visitClassDeclaration(classDecl, executionContext);
            if (classDecl.getExtends()==null || !((JavaType.Class)classDecl.getExtends().getType()).getFullyQualifiedName().equals("org.codehaus.plexus.PlexusTestCase"))  {
                return classDecl;
            }

            classDecl = classDecl.withExtends(null);
            maybeRemoveImport("org.codehaus.plexus.PlexusTestCase");
            return classDecl;
        }
    }
}
