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

import java.util.ArrayList;
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

        private static List<String> staticPlexusExtensionsMethod = new ArrayList<>() {
            {
                add("getTestFile");
                add("getTestPath");
                add("getBaseDir");
                add("getTestConfiguration");
            }
        };

        private static String NEW_CLASS = "org.codehaus.plexus.testing.PlexusExtension";
        private static String OLD_CLASS = "org.codehaus.plexus.PlexusTestCase";

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
//                    .staticImports(staticPlexusExtensionsMethod.stream().map(name -> NEW_CLASS + "." + name).toArray(String[]::new))
                    .build()
                    .apply(getCursor(), cd.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName)));
            maybeAddImport("org.codehaus.plexus.testing.PlexusTest"); // both imports methods are needed to add a new import
            staticPlexusExtensionsMethod.forEach( it -> {
                        doAfterVisit(new ChangeMethodTargetToStatic(OLD_CLASS + " " + it + "(..)", NEW_CLASS, null, null, true).getVisitor());
                    }
            );

            cd = cd.withExtends(null);
            maybeRemoveImport("org.codehaus.plexus.PlexusTestCase");

            return cd;
        }


//        @Override
//        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext o) {
//            J.MethodInvocation m = super.visitMethodInvocation(method, o);
//            if(!isPlexusTestCaseMethod(m)){
//                return m;
//            }
//
//            return m;
//        }
//
//        private boolean isPlexusTestCaseMethod(J.MethodInvocation method) {
//            return staticPlexusExtensionsMethod.contains(method.getSimpleName());
//        }


    }
}
