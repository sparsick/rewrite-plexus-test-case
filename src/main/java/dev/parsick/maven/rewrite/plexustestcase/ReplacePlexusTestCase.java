package dev.parsick.maven.rewrite.plexustestcase;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.ChangeMethodTargetToStatic;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.Comparator;
import java.util.List;

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

        private static List<String> STATIC_PLEXUS_EXTENSIONS_METHOD = List.of(
                "getTestFile",
                "getTestPath",
                "getBaseDir",
                "getTestConfiguration"
        );

        private static final String FULLY_QUALIFIED_NAME_PLEXUS_TEST = "org.codehaus.plexus.testing.PlexusTest";
        private static final String FULLY_QUALIFIED_NAME_PLEXUS_EXTENSION = "org.codehaus.plexus.testing.PlexusExtension";
        private static final String FULLY_QUALIFIED_NAME_PLEXUS_TEST_CASE = "org.codehaus.plexus.PlexusTestCase";

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
            J.ClassDeclaration cd = classDecl;
            if (cd.getExtends()==null || !((JavaType.Class)cd.getExtends().getType()).getFullyQualifiedName().equals(FULLY_QUALIFIED_NAME_PLEXUS_TEST_CASE))  {
                return cd;
            }

            cd = JavaTemplate.builder("@PlexusTest")
                    .javaParser(JavaParser.fromJavaVersion()
                            .classpath("plexus-testing"))
                    .imports(FULLY_QUALIFIED_NAME_PLEXUS_TEST)
                    .build()
                    .apply(getCursor(), cd.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName)));
            maybeAddImport(FULLY_QUALIFIED_NAME_PLEXUS_TEST); // both imports methods are needed to add a new import
            STATIC_PLEXUS_EXTENSIONS_METHOD.forEach(it -> doAfterVisit(new ChangeMethodTargetToStatic(FULLY_QUALIFIED_NAME_PLEXUS_TEST_CASE + " " + it + "(..)", FULLY_QUALIFIED_NAME_PLEXUS_EXTENSION, null, null, true).getVisitor())
            );

            cd = cd.withExtends(null);
            maybeRemoveImport(FULLY_QUALIFIED_NAME_PLEXUS_TEST_CASE);

            return cd;
        }





    }
}
