package dev.parsick.maven.rewrite.abstractmojotestcase;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.NlsRewrite;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ReplaceLookupMojo extends Recipe {

    @Override
    public @NlsRewrite.DisplayName String getDisplayName() {
        return "Replace lookupMojo method by InjectMojo annotation";
    }

    @Override
    public @NlsRewrite.Description String getDescription() {
        return "Replace lookupMojo method by InjectMojo annotation.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ReplaceLookupMojoVisitor();
    }

    private class ReplaceLookupMojoVisitor extends JavaIsoVisitor<ExecutionContext> {


        private static final String FULLY_QUALIFIED_NAME_INJECT_MOJO = "org.apache.maven.api.plugin.testing.InjectMojo";
        private static final String LOOKUP_MOJO_METHOD = "lookupMojo";

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
            if(!extendsAbstractMojoTestCase()){
                return method;
            }
            J.MethodDeclaration md = method;
            List<J.VariableDeclarations.NamedVariable> lookupMojoMethods = findLookupMojoMethod(md);
            if(!lookupMojoMethods.isEmpty()) {
                for(J.VariableDeclarations.NamedVariable var : lookupMojoMethods) {
                    md = addInjectMojoAnnotation(var, method);
                    md = addMethodParameter(var, md);
                }
                maybeAddImport(FULLY_QUALIFIED_NAME_INJECT_MOJO); // both imports methods are needed to add a new import
            }
            return super.visitMethodDeclaration(md, executionContext);
        }

        private J.@NotNull MethodDeclaration addMethodParameter(J.VariableDeclarations.NamedVariable var, J.MethodDeclaration md) {
            var newParamName = var.getVariableType().getName();
            var newParamType = ((JavaType.Class) var.getVariableType().getType()).getClassName();
            var newImport =((JavaType.Class) var.getVariableType().getType()).getFullyQualifiedName();
            // TODO check if some parameter already exists
            String newParameter = String.format("""
                    %s %s""", newParamType, newParamName);
            md = JavaTemplate.builder(newParameter)
                    .javaParser(JavaParser.fromJavaVersion().classpath("maven-plugin-api"))
                    .imports(newImport)
                    .build().apply(updateCursor(md), md.getCoordinates().replaceParameters());
            maybeAddImport(newImport); // both imports methods are needed to add a new import
            return md;
        }

        private J.MethodDeclaration addInjectMojoAnnotation(J.VariableDeclarations.NamedVariable var, J.MethodDeclaration method) {
            List<Expression> arguments = ((J.MethodInvocation) var.getInitializer()).getArguments();
            var goalName = arguments.get(0).toString();
            var pomPath = extractPomPath(arguments.get(1), method.getBody());
            String newAnnotationCode = String.format("""
                @InjectMojo(goal="%s", pom="%s")""", goalName, pomPath);
            return JavaTemplate.builder(newAnnotationCode)
                    .javaParser(JavaParser.fromJavaVersion().classpath("maven-plugin-testing-harness"))
                    .imports(FULLY_QUALIFIED_NAME_INJECT_MOJO)
                    .build().apply(updateCursor(method), method.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName)));
        }

        private static String extractPomPath(Expression pomPath, J.@Nullable Block body) {
            if (pomPath instanceof J.Literal) {
                return pomPath.toString();
            } else {
                var varName = pomPath.toString();
                J.VariableDeclarations.NamedVariable pomFile = body.getStatements().stream()
                        .filter(statement -> statement instanceof J.VariableDeclarations)
                        .map(statement -> (J.VariableDeclarations) statement)
                        .filter(variableDeclarations -> variableDeclarations.getVariables().size() == 1)
                        .flatMap(variableDeclarations -> variableDeclarations.getVariables().stream())
                        .filter(namedVariable -> namedVariable.getSimpleName().equals(varName))
                        .findFirst().get();
                return ((J.NewClass) pomFile.getInitializer()).getArguments().get(1).toString();
            }
        }

        private boolean extendsAbstractMojoTestCase() {
            J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
            return classDecl.getExtends() != null && classDecl.getExtends().getType().toString().equals("org.apache.maven.plugin.testing.AbstractMojoTestCase");
        }

        private @NotNull List<J.VariableDeclarations.NamedVariable> findLookupMojoMethod(J.MethodDeclaration md) {
            if(md.getBody() == null) {
                return Collections.emptyList();
            }
            List<J.VariableDeclarations.NamedVariable> namedVariables = md.getBody().getStatements().stream()
                    .filter(st -> st instanceof J.VariableDeclarations)
                    .flatMap(vd -> ((J.VariableDeclarations) vd).getVariables().stream()).toList(); // find all lines of code that init new local variable
            return namedVariables.stream().filter(nv -> nv.getInitializer().toString().contains(LOOKUP_MOJO_METHOD)).toList(); // find all lines of code that init a new local variable with a lookupMojo method
        }


        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext executionContext) {
            J.VariableDeclarations variableDeclarations = super.visitVariableDeclarations(multiVariable, executionContext);

            if(variableDeclarations.getVariables().stream().anyMatch(v -> v.getInitializer()!= null && v.getInitializer().toString().contains("new File"))) {
                J.VariableDeclarations.NamedVariable potentialLookupArgument = variableDeclarations.getVariables().stream().filter(v -> v.getInitializer() != null && v.getInitializer().toString().contains("new File")).findFirst().get();
                if (isArgumentOfLookupMethod(potentialLookupArgument.getSimpleName())){
                    maybeRemoveImport("java.io.File");
                    return null; // delete pomPath file var
                }

            }

            if(variableDeclarations.getVariables().stream().anyMatch(v -> v.getInitializer()!= null && v.getInitializer().toString().contains(LOOKUP_MOJO_METHOD))) {
                return null; // delete line with lookupMojo
            }

            return variableDeclarations;
        }

        private boolean isArgumentOfLookupMethod(String argumentVarName) {
            J.MethodDeclaration methodDeclaration = getCursor().firstEnclosing(J.MethodDeclaration.class);
            return methodDeclaration.getBody().getStatements().stream()
                    .filter(statement -> statement instanceof J.VariableDeclarations)
                    .map(statement -> (J.VariableDeclarations) statement)
                    .flatMap(variableDeclarations -> variableDeclarations.getVariables().stream())
                    .filter(namedVariable -> namedVariable.getInitializer() != null && namedVariable.getInitializer().toString().contains(LOOKUP_MOJO_METHOD))
                    .flatMap(namedVariable -> ((J.MethodInvocation) namedVariable.getInitializer()).getArguments().stream())
                    .filter(statement -> statement instanceof J.Identifier)
                    .map(statement -> (J.Identifier) statement)
                    .anyMatch(statement -> statement.getSimpleName().toString().equals(argumentVarName));
        }
    }
}
