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
import org.openrewrite.java.tree.JavaType;

import java.util.Comparator;
import java.util.List;

public class ReplaceLookupMojo extends Recipe {



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
        return new ReplaceLookupMojoVisitor();
    }

    private class ReplaceLookupMojoVisitor extends JavaIsoVisitor<ExecutionContext> {

//        @Override
//        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
//            if(classDecl.getExtends() == null || !classDecl.getExtends().getType().toString().equals("org.apache.maven.plugin.testing.AbstractMojoTestCase")) {
//                return classDecl;
//            }
//
//            J.ClassDeclaration classDeclaration = classDecl;
//
//            List<J.VariableDeclarations.NamedVariable> lookupMethod = findLookupMethod(classDeclaration);
//
//            if  (!lookupMethod.isEmpty()) {
//                for(J.VariableDeclarations.NamedVariable var : lookupMethod) {
//                    var newFieldName = var.getVariableType().getName();
//                    var newFieldType = ((JavaType.Class) var.getVariableType().getType()).getClassName();
//                    var newImport =((JavaType.Class) var.getVariableType().getType()).getFullyQualifiedName();
//
//                    String newFieldCode = String.format("""
//                        @Inject
//                        private %s %s;
//                        """, newFieldType, newFieldName);
//                    classDeclaration = JavaTemplate.builder(newFieldCode)
//                        .javaParser(JavaParser.fromJavaVersion().classpath("javax.inject", "plexus-build-api"))
//                        .imports("javax.inject.Inject", newImport)
//                        .build().apply(updateCursor(classDeclaration), classDeclaration.getBody().getCoordinates().firstStatement());
//                    maybeAddImport(newImport); // both imports methods are needed to add a new import
//                }
//             maybeAddImport("javax.inject.Inject"); // both imports methods are needed to add a new import
//            }
//
//
//            return super.visitClassDeclaration(classDeclaration, executionContext);
//        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
            if(!extendsAbstractMojoTestCase()){
                return method;
            }
            J.MethodDeclaration md = method;
            List<J.VariableDeclarations.NamedVariable> lookupMojoMethods = findLookupMojoMethod(md);
            if(!lookupMojoMethods.isEmpty()) {
                for(J.VariableDeclarations.NamedVariable var : lookupMojoMethods) {
                    var newParamName = var.getVariableType().getName();
                    var newParamType = ((JavaType.Class) var.getVariableType().getType()).getClassName();
                    var newImport =((JavaType.Class) var.getVariableType().getType()).getFullyQualifiedName();
                    var goalName = ((J.MethodInvocation) var.getInitializer()).getArguments().getFirst().toString();

                    String newAnnotationCode = String.format("""
                        @InjectMojo(goal="%s")""", goalName);
                    md = JavaTemplate.builder(newAnnotationCode)
                            .javaParser(JavaParser.fromJavaVersion().classpath("maven-plugin-testing-harness"))
                            .imports("org.apache.maven.api.plugin.testing.InjectMojo")
                            .build().apply(updateCursor(method), method.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName)));

                    String newParameter = String.format("""
                            %s %s""", newParamType, newParamName);
                    md = JavaTemplate.builder(newParameter)
                            .javaParser(JavaParser.fromJavaVersion().classpath("maven-plugin-api"))
                            .imports(newImport)
                            .build().apply(updateCursor(md), md.getCoordinates().replaceParameters());
                    maybeAddImport(newImport); // both imports methods are needed to add a new import
                }
                maybeAddImport("org.apache.maven.api.plugin.testing.InjectMojo"); // both imports methods are needed to add a new import
            }
            return super.visitMethodDeclaration(md, executionContext);
        }

        private boolean extendsAbstractMojoTestCase() {
            J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
            return classDecl.getExtends() != null && classDecl.getExtends().getType().toString().equals("org.apache.maven.plugin.testing.AbstractMojoTestCase");
        }

        private @NotNull List<J.VariableDeclarations.NamedVariable> findLookupMojoMethod(J.MethodDeclaration md) {
            List<J.VariableDeclarations.NamedVariable> namedVariables = md.getBody().getStatements().stream()
                    .filter(st -> st instanceof J.VariableDeclarations)
                    .flatMap(vd -> ((J.VariableDeclarations) vd).getVariables().stream()).toList(); // find all lines of code that init new local variable
            return namedVariables.stream().filter(nv -> nv.getInitializer().toString().contains("lookupMojo")).toList(); // find all lines of code that init a new local variable with a lookupMojo method
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
