package dev.parsick.maven.rewrite.plexustestcase;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class MigratePlexusTestCaseToPlexusExtensionTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigratePlexusTestCaseToPlexusExtension());
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

                        public class MojoTest {
                        }
        """
        ));
    }


    @Test
    void plexusExtension() {
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

                            }

                            @Override
                            protected void customizeContainerConfiguration(ContainerConfiguration containerConfiguration) {
                                containerConfiguration.setClassPathScanning("cache");
                            }
                        }
        """,
                """
                        import org.codehaus.plexus.ContainerConfiguration;
                        import org.codehaus.plexus.build.BuildContext;
                        import org.codehaus.plexus.testing.PlexusExtension;
                        import org.junit.jupiter.api.Test;
                        import org.junit.jupiter.api.extension.RegisterExtension;

                        import javax.inject.Inject;
                        import java.io.File;
                        import java.util.Arrays;

                        import static org.junit.jupiter.api.Assertions.assertFalse;
                        import static org.junit.jupiter.api.Assertions.assertTrue;

                        public class MojoTest {

                            @Inject
                            BuildContext buildContext;

                            @RegisterExtension
                            PlexusExtension plexusExtension = new PlexusExtension() {
                                @Override
                                protected void customizeContainerConfiguration(ContainerConfiguration containerConfiguration) {
                                    containerConfiguration.setClassPathScanning("cache");
                                }
                            };

                            public void testModelloConvertersMojo() throws Exception {
                                File outputDirectory = PlexusExtension.getTestFile("target/converters-test");

                                String models[] = new String[1];
                                models[0] = PlexusExtension.getTestPath("src/test/resources/java-model.mdo");

                            }


                        }

        """
        ));
    }
}

