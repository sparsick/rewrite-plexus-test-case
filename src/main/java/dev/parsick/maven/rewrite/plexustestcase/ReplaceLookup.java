package dev.parsick.maven.rewrite.plexustestcase;

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

public class ReplaceLookup extends Recipe {



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
        return new ReplaceLookupVisitor();
    }

    private class ReplaceLookupVisitor extends JavaIsoVisitor<ExecutionContext> {

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
            if(classDecl.getExtends() == null || !classDecl.getExtends().getType().toString().equals("org.codehaus.plexus.PlexusTestCase")) {
                return classDecl;
            }

            J.ClassDeclaration classDeclaration = classDecl;

            List<J.VariableDeclarations.NamedVariable> lookupMethod = findLookupMethod(classDeclaration);

            if  (!lookupMethod.isEmpty()) {
                for(J.VariableDeclarations.NamedVariable var : lookupMethod) {
                    var newFieldName = var.getVariableType().getName();
                    var newFieldType = ((JavaType.Class) var.getVariableType().getType()).getClassName();
                    var newImport =((JavaType.Class) var.getVariableType().getType()).getFullyQualifiedName();

                    String newFieldCode = String.format("""
                        @Inject
                        private %s %s; 
                        """, newFieldType, newFieldName);
                    classDeclaration = JavaTemplate.builder(newFieldCode)
                        .javaParser(JavaParser.fromJavaVersion().classpath("javax.inject", "plexus-build-api"))
                        .imports("javax.inject.Inject", newImport)
                        .build().apply(updateCursor(classDeclaration), classDeclaration.getBody().getCoordinates().firstStatement());
                    maybeAddImport(newImport); // both imports methods are needed to add a new import
                }
             maybeAddImport("javax.inject.Inject"); // both imports methods are needed to add a new import
            }


            return super.visitClassDeclaration(classDeclaration, executionContext);
        }

        private @NotNull List<J.VariableDeclarations.NamedVariable> findLookupMethod(J.ClassDeclaration classDeclaration) {
            List<J.VariableDeclarations.NamedVariable> namedVariables = classDeclaration.getBody().getStatements().stream()
                    .filter(statement -> statement instanceof J.MethodDeclaration)
                    .map(statement -> (J.MethodDeclaration) statement)
                    .flatMap(md -> md.getBody().getStatements().stream())// find all method declaration in the class
                    .filter(st -> st instanceof J.VariableDeclarations)
                    .flatMap(vd -> ((J.VariableDeclarations) vd).getVariables().stream()).toList(); // find all lines of code that init new local variable
            return namedVariables.stream().filter(nv -> nv.getInitializer().toString().contains("lookup")).toList(); // find all lines of code that init a new local variable with a lookup method
        }


        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext executionContext) {
            J.VariableDeclarations variableDeclarations = super.visitVariableDeclarations(multiVariable, executionContext);
            if(variableDeclarations.getVariables().stream().anyMatch(v -> v.getInitializer()!= null && v.getInitializer().toString().contains("lookup"))) {
                return null; // delete line with lookup
            }

            return variableDeclarations;
        }
    }
}
