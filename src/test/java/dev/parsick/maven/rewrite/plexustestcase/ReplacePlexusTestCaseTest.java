package dev.parsick.maven.rewrite.plexustestcase;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ReplacePlexusTestCaseTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ReplacePlexusTestCase());
        spec.parser(JavaParser.fromJavaVersion()
                .classpathFromResources(new InMemoryExecutionContext(), "junit-4.13.2", "org.eclipse.sisu.plexus-0.9.0.M4", "plexus-build-api-1.2.0"));
    }


    @Test
    void doNothing() {
        rewriteRun(java(
                """
                                        public class MojoTest {
                        
                                        }
                        """));
    }

    @Test
    void removePlexusTestCase() {
        rewriteRun(java(
                """
                        import org.codehaus.plexus.PlexusTestCase;

                        public class MojoTest extends PlexusTestCase {
                        }
        """,
                """
                        import org.codehaus.plexus.testing.PlexusTest;
                        
                        @PlexusTest
                        public class MojoTest {
                        }
        """
        ));
    }

 
}

