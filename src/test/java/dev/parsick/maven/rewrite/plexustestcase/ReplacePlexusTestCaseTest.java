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

    @Test
    void addStaticImport() {
        rewriteRun(java(
                """
                        import java.io.File;
                        import java.util.Arrays;
                        
                        import org.codehaus.plexus.PlexusTestCase;
                                               
                        public class MojoTest extends PlexusTestCase {
                            public void testModelloConvertersMojo() throws Exception {
                                File outputDirectory = getTestFile("target/converters-test");

                                String models[] = new String[1];
                                models[0] = getTestPath("src/test/resources/java-model.mdo");

                            }
                        }
        """,
                """
                        import java.io.File;
                        import java.util.Arrays;

                        import org.codehaus.plexus.testing.PlexusTest;
                        
                        import static org.codehaus.plexus.testing.PlexusExtension.getTestFile;
                        import static org.codehaus.plexus.testing.PlexusExtension.getTestPath;
                        
                        @PlexusTest
                        public class MojoTest {
                            public void testModelloConvertersMojo() throws Exception {
                                File outputDirectory = getTestFile("target/converters-test");

                                String models[] = new String[1];
                                models[0] = getTestPath("src/test/resources/java-model.mdo");

                            }
                        }"""
        ));
    }

 
}

