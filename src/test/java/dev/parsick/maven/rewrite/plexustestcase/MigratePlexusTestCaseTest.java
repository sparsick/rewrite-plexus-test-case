package dev.parsick.maven.rewrite.plexustestcase;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

class MigratePlexusTestCaseTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(MigratePlexusTestCaseTest.class.getResourceAsStream("/META-INF/rewrite/rewrite.yml"), "dev.parsick.maven.rewrite.plexustestcase.MigratePlexusTestCase");
        spec.parser(JavaParser.fromJavaVersion()
                .classpathFromResources(new InMemoryExecutionContext(),
                        "junit-4.13.2", "org.eclipse.sisu.plexus-0.9.0.M4", "plexus-build-api-1.2.0", "plexus-testing-1.7.0"));
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
                        import java.util.Arrays;

                        import org.codehaus.plexus.ContainerConfiguration;
                        import org.codehaus.plexus.PlexusTestCase;
                        import org.codehaus.plexus.build.BuildContext;

                        public class MojoTest extends PlexusTestCase {
                            public void testModelloConvertersMojo() throws Exception {

                                BuildContext buildContext = (BuildContext) lookup(BuildContext.class);

                                File outputDirectory = getTestFile("target/converters-test");

                                String models[] = new String[1];
                                models[0] = getTestPath("src/test/resources/java-model.mdo");

                                File javaFile =
                                        new File(outputDirectory, "org/codehaus/mojo/modello/javatest/v1_0_0/convert/VersionConverter.java");

                                assertTrue("The generated java file doesn't exist: '" + javaFile.getAbsolutePath() + "'.", javaFile.exists());

                                assertFalse("The generated java file doesn't exist: '" + javaFile.getAbsolutePath() + "'.", javaFile.exists());

                            }

                            @Override
                            protected void customizeContainerConfiguration(ContainerConfiguration containerConfiguration) {
                                containerConfiguration.setClassPathScanning("cache");
                            }
                        }
        """,
                """
                        import java.io.File;
                        import java.util.Arrays;
                        
                        import org.codehaus.plexus.ContainerConfiguration;
                        
                        import static org.codehaus.plexus.testing.PlexusExtension.getTestFile;
                        import static org.codehaus.plexus.testing.PlexusExtension.getTestPath;
                        import static org.junit.jupiter.api.Assertions.assertFalse;
                        import static org.junit.jupiter.api.Assertions.assertTrue;
                        import org.codehaus.plexus.build.BuildContext;
                        import org.codehaus.plexus.testing.PlexusTest;
                        import org.codehaus.plexus.testing.PlexusTestConfiguration;
                        import org.junit.jupiter.api.Test;
                        
                        import javax.inject.Inject;
                        
                        @PlexusTest
                        public class MojoTest implements PlexusTestConfiguration {
                            @Inject
                            private BuildContext buildContext;
                            @Test
                            public void testModelloConvertersMojo() throws Exception {
                        
                                File outputDirectory = getTestFile("target/converters-test");
                        
                                String models[] = new String[1];
                                models[0] = getTestPath("src/test/resources/java-model.mdo");
                        
                                File javaFile =
                                        new File(outputDirectory, "org/codehaus/mojo/modello/javatest/v1_0_0/convert/VersionConverter.java");
                        
                                assertTrue(javaFile.exists(), "The generated java file doesn't exist: '" + javaFile.getAbsolutePath() + "'.");
                        
                                assertFalse(javaFile.exists(), "The generated java file doesn't exist: '" + javaFile.getAbsolutePath() + "'.");
                        
                            }
                        
                            @Override
                            public void customizeConfiguration(ContainerConfiguration containerConfiguration) {
                                containerConfiguration.setClassPathScanning("cache");
                            }
                        }"""
        ));
    }
}

