package dev.parsick.maven.rewrite.abstractmojotestcase;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ReplaceAbstractMojoTestCaseTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ReplaceAbstractMojoTestCase());
        spec.parser(JavaParser.fromJavaVersion()
                .classpathFromResources(new InMemoryExecutionContext(), "junit-4.13.2", "org.eclipse.sisu.plexus-0.9.0.M4", "plexus-build-api-1.2.0", "maven-plugin-testing-harness-3.4.0-SNAPSHOT"));
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
    void removeAbstractMojoTestCase() {
        rewriteRun(java(
                """
                        import org.apache.maven.plugin.testing.AbstractMojoTestCase;

                        public class MojoTest extends AbstractMojoTestCase {
                        }
        """,
                """
                        import org.apache.maven.api.plugin.testing.MojoTest;
                        
                        @MojoTest
                        public class MojoTest {
                        }
        """
        ));
    }

}