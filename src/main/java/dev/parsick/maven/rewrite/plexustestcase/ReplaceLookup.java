package dev.parsick.maven.rewrite.plexustestcase;

import org.openrewrite.ExecutionContext;
import org.openrewrite.NlsRewrite;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.TreeVisitingPrinter;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toList;

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
        private Set<String> newFields = new HashSet<>();

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {


            J.ClassDeclaration classDeclaration = super.visitClassDeclaration(classDecl, executionContext);

            for (String fieldCode : newFields) {
                classDeclaration = JavaTemplate.builder(fieldCode)
                        .javaParser(JavaParser.fromJavaVersion().classpath("javax.inject"))
                        .imports("javax.inject.Inject")
                        .build().apply(updateCursor(classDeclaration), classDeclaration.getBody().getCoordinates().firstStatement());
            }
            maybeAddImport("javax.inject.Inject"); // both imports methods are needed to add a new import

            return classDeclaration;
        }


        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext executionContext) {
            J.VariableDeclarations variableDeclarations = super.visitVariableDeclarations(multiVariable, executionContext);
            if(variableDeclarations.getVariables().stream().anyMatch(v -> v.getInitializer()!= null && v.getInitializer().toString().contains("lookup"))) {
                J.VariableDeclarations.NamedVariable namedVariable = variableDeclarations.getVariables().getFirst();
                var newFieldName = namedVariable.getVariableType().getName();
                var newFieldType = ((JavaType.Class) namedVariable.getVariableType().getType()).getClassName();

                String newFieldCode = String.format("""
                        @Inject
                        private %s %s; 
                        """, newFieldType, newFieldName);
                newFields.add(newFieldCode);





                return null;
            }

            return variableDeclarations;
        }
    }
}
