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
import org.openrewrite.java.tree.TypeTree;

import java.util.List;

public class AddPlexusTestConfiguration extends Recipe {
    @Override
    public @NlsRewrite.DisplayName String getDisplayName() {
        return "Introduce PlexusTestConfiguration interface";
    }

    @Override
    public @NlsRewrite.Description String getDescription() {
        return "Migrate custom configuration methods to PlexusTestConfiguration's methods.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new AddPlexusTestConfigurationVisitor();
    }

    private class AddPlexusTestConfigurationVisitor extends JavaIsoVisitor<ExecutionContext> {
        private static final String FULLY_QUALIFIED_NAME_PLEXUS_TEST_CONFIGURATION = "org.codehaus.plexus.testing.PlexusTestConfiguration";
        private static final List<String> PLEXUS_TEST_CASE_CUSTOMIZE_METHOD = List.of("customizeContainerConfiguration", "customizeContext");

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
            J.ClassDeclaration cd = classDecl;
            if(!hasCustomMethod(cd)) {
                return cd;
            }


            List<TypeTree> interfaceList= cd.getImplements();
            if (interfaceList == null || interfaceList.stream().map(Object::toString).noneMatch(name -> name.contains("PlexusTestConfiguration"))) {
                cd = JavaTemplate.builder("PlexusTestConfiguration")
                        .javaParser(JavaParser.fromJavaVersion()
                                .classpath("plexus-testing"))
                        .imports(FULLY_QUALIFIED_NAME_PLEXUS_TEST_CONFIGURATION)
                        .build()
                        .apply(getCursor(), cd.getCoordinates().replaceImplementsClause());

                maybeAddImport(FULLY_QUALIFIED_NAME_PLEXUS_TEST_CONFIGURATION);
            }
            return cd;
        }

        private boolean hasCustomMethod(J.ClassDeclaration cd) {
            return cd.getBody().getStatements().stream()
                    .filter(statement -> statement instanceof J.MethodDeclaration)
                    .map(md -> ((J.MethodDeclaration) md).getSimpleName())
                    .anyMatch(s -> PLEXUS_TEST_CASE_CUSTOMIZE_METHOD.contains(s));
        }

    }
}
