package dev.parsick.maven.rewrite.abstractmojotestcase;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class MigrateAbstractMojoTestCaseTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(MigrateAbstractMojoTestCaseTest.class.getResourceAsStream("/META-INF/rewrite/rewrite.yml"), "dev.parsick.maven.rewrite.abstractmojotestcase.MigrateAbstractMojoTestCase");
        spec.parser(JavaParser.fromJavaVersion()
                .classpathFromResources(new InMemoryExecutionContext(),
                        "junit-4.13.2", "org.eclipse.sisu.plexus-0.9.0.M4", "plexus-build-api-1.2.0", "maven-plugin-testing-harness-3.4.0-SNAPSHOT", "maven-plugin-api-3.9.11"));
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
    void roundTrip() {
        rewriteRun(java(
                """
                        import java.io.File;

                        import org.apache.maven.plugin.testing.AbstractMojoTestCase;
                        import org.codehaus.plexus.build.BuildContext;
                        import org.apache.maven.plugin.Mojo;

                        public class MojoTest extends AbstractMojoTestCase {
                            public void testModelloConvertersMojo() throws Exception {

                                BuildContext buildContext = (BuildContext) lookup(BuildContext.class);
                                Mojo mojo = lookupMojo("jira-changes", "");
                                setVariableValueToObject(mojo, "skip", true);
                                setVariableValueToObject(mojo, "other", "set");

                                File javaFile = new File("org/codehaus/mojo/modello/javatest/v1_0_0/convert/VersionConverter.java");

                                assertTrue("The generated java file doesn't exist: '" + javaFile.getAbsolutePath() + "'.", javaFile.exists());

                                assertFalse("The generated java file doesn't exist: '" + javaFile.getAbsolutePath() + "'.", javaFile.exists());

                            }

                        }
        """,
                """
                          import java.io.File;
                        
                          import static org.junit.jupiter.api.Assertions.assertFalse;
                          import static org.junit.jupiter.api.Assertions.assertTrue;
                          import org.codehaus.plexus.build.BuildContext;
                          import org.junit.jupiter.api.Test;
                          import org.apache.maven.api.plugin.testing.InjectMojo;
                          import org.apache.maven.api.plugin.testing.MojoParameter;
                          import org.apache.maven.api.plugin.testing.MojoTest;
                          import org.apache.maven.plugin.Mojo;
                        
                          import javax.inject.Inject;
                        
                          @MojoTest
                          public class MojoTest {
                              @Inject
                              private BuildContext buildContext;
                        
                              @InjectMojo(goal = "jira-changes", pom = "")
                              @MojoParameter(name = "skip", value = "true")
                              @MojoParameter(name = "other", value = "set")
                              @Test
                              public void testModelloConvertersMojo(Mojo mojo) throws Exception {
                        
                                  File javaFile = new File("org/codehaus/mojo/modello/javatest/v1_0_0/convert/VersionConverter.java");
                        
                                  assertTrue(javaFile.exists(), "The generated java file doesn't exist: '" + javaFile.getAbsolutePath() + "'.");
                        
                                  assertFalse(javaFile.exists(), "The generated java file doesn't exist: '" + javaFile.getAbsolutePath() + "'.");
                        
                              }
                        
                          }"""
        ));
    }

    @Test
    void abstractClasses() {
        rewriteRun(java(
                """
                        import org.apache.maven.plugin.testing.AbstractMojoTestCase;

                        public abstract class MojoTest extends AbstractMojoTestCase {
                            public abstract void testModelloConvertersMojo() throws Exception;
                        }
        """,
                """
                        import org.apache.maven.api.plugin.testing.MojoTest;
                        import org.junit.jupiter.api.Test;
                        
                        @MojoTest
                        public abstract class MojoTest {
                            @Test
                            public abstract void testModelloConvertersMojo() throws Exception;
                        }"""
        ));
    }

}

