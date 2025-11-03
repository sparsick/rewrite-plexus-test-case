package dev.parsick.maven.rewrite;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.NlsRewrite;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.List;
import java.util.function.Predicate;

public class ReplaceLookup extends Recipe {



    @Override
    public @NlsRewrite.DisplayName String getDisplayName() {
        return "Replace lookup method by Inject annotation";
    }

    @Override
    public @NlsRewrite.Description String getDescription() {
        return "Replace lookup method by Inject annotation.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ReplaceLookupVisitor();
    }

    private class ReplaceLookupVisitor extends JavaIsoVisitor<ExecutionContext> {

        private static final String FULLY_QUALIFIED_NAME_INJECT = "javax.inject.Inject";

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
            if(!extendsPlexusTestCaseOrAbstractMojoTestCase(classDecl)) {
                return classDecl;
            }

            J.ClassDeclaration classDeclaration = classDecl;

            List<J.VariableDeclarations.NamedVariable> lookupMethod = findLookupMethod(classDeclaration);

            if  (!lookupMethod.isEmpty()) {
                for(J.VariableDeclarations.NamedVariable var : lookupMethod) {
                    classDeclaration = addInjectField(var, classDeclaration);
                }
             maybeAddImport(FULLY_QUALIFIED_NAME_INJECT); // both imports methods are needed to add a new import
            }


            return super.visitClassDeclaration(classDeclaration, executionContext);
        }

        private J.@NotNull ClassDeclaration addInjectField(J.VariableDeclarations.NamedVariable var, J.ClassDeclaration classDeclaration) {
            var newFieldName = var.getVariableType().getName();
            var newFieldType = ((JavaType.Class) var.getVariableType().getType()).getClassName();
            var newImport =((JavaType.Class) var.getVariableType().getType()).getFullyQualifiedName();

            String newFieldCode = String.format("""
                @Inject
                private %s %s; 
                """, newFieldType, newFieldName);
            classDeclaration = JavaTemplate.builder(newFieldCode)
                .javaParser(JavaParser.fromJavaVersion().classpath("javax.inject", "plexus-build-api"))
                .imports(FULLY_QUALIFIED_NAME_INJECT, newImport)
                .build().apply(updateCursor(classDeclaration), classDeclaration.getBody().getCoordinates().firstStatement());
            maybeAddImport(newImport); // both imports methods are needed to add a new import
            return classDeclaration;
        }

        private boolean extendsPlexusTestCaseOrAbstractMojoTestCase(J.ClassDeclaration classDecl) {
            return classDecl.getExtends() != null && (classDecl.getExtends().getType().toString().equals("org.codehaus.plexus.PlexusTestCase") || classDecl.getExtends().getType().toString().equals("org.apache.maven.plugin.testing.AbstractMojoTestCase") );
        }

        private @NotNull List<J.VariableDeclarations.NamedVariable> findLookupMethod(J.ClassDeclaration classDeclaration) {
            List<J.VariableDeclarations.NamedVariable> namedVariables = classDeclaration.getBody().getStatements().stream()
                    .filter(statement -> statement instanceof J.MethodDeclaration)
                    .map(statement -> (J.MethodDeclaration) statement)
                    .filter(md -> md.getBody() != null )
                    .flatMap(md -> md.getBody().getStatements().stream())// find all method declaration in the class
                    .filter(st -> st instanceof J.VariableDeclarations)
                    .flatMap(vd -> ((J.VariableDeclarations) vd).getVariables().stream()).toList(); // find all lines of code that init new local variable
            return namedVariables.stream()
                    .filter(hasLookupMethod())
                    .toList(); // find all lines of code that init a new local variable with a lookup method
        }

        private @NotNull Predicate<J.VariableDeclarations.NamedVariable> hasLookupMethod() {
            return nv -> nv.getInitializer() instanceof J.TypeCast
                    && ((J.TypeCast) nv.getInitializer()).getExpression() instanceof J.MethodInvocation
                    && ((J.MethodInvocation) ((J.TypeCast) nv.getInitializer()).getExpression()).getName().toString().equals("lookup");
        }


        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext executionContext) {
            J.VariableDeclarations variableDeclarations = super.visitVariableDeclarations(multiVariable, executionContext);
            if(hasLookupMethod(variableDeclarations)) {
                return null; // delete line with lookup
            }

            return variableDeclarations;
        }

        private boolean hasLookupMethod(J.VariableDeclarations variableDeclarations) {
            return variableDeclarations.getVariables().stream().anyMatch(hasLookupMethod());
        }
    }
}
