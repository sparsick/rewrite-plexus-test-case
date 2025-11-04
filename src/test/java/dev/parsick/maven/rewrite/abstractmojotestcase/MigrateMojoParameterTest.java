package dev.parsick.maven.rewrite.abstractmojotestcase;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class MigrateMojoParameterTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateMojoParameter());
        spec.parser(JavaParser.fromJavaVersion()
                .classpathFromResources(new InMemoryExecutionContext(), "junit-4.13.2", "org.eclipse.sisu.plexus-0.9.0.M4", "plexus-build-api-1.2.0", "maven-plugin-testing-harness-3.4.0-SNAPSHOT", "maven-plugin-api-3.9.11"));
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
    void replaceOneSet() {
        rewriteRun(java("""
                                        import org.apache.maven.plugin.testing.AbstractMojoTestCase;
                                        import org.apache.maven.plugin.Mojo;
                
                                        public class MojoTest extends AbstractMojoTestCase {
                                            public void testModelloConvertersMojo() throws Exception {
                                                Mojo mojo = lookupMojo("jira-changes", "");
                                                setVariableValueToObject(mojo, "skip", true);
                                            }
                                        }
                
                """, """
                                        import org.apache.maven.plugin.testing.AbstractMojoTestCase;
                                        import org.apache.maven.api.plugin.testing.MojoParameter;
                                        import org.apache.maven.plugin.Mojo;
                
                                        public class MojoTest extends AbstractMojoTestCase {
                                            @MojoParameter(name = "skip", value = "true")
                                            public void testModelloConvertersMojo() throws Exception {
                                                Mojo mojo = lookupMojo("jira-changes", "");
                                            }
                                        }
                
                """



                ));
    }

    @Test
    void replaceMoreThanOneSet() {
        rewriteRun(java("""
                                        import org.apache.maven.plugin.testing.AbstractMojoTestCase;
                                        import org.apache.maven.plugin.Mojo;
                
                                        public class MojoTest extends AbstractMojoTestCase {
                                            public void testModelloConvertersMojo() throws Exception {
                                                Mojo mojo = lookupMojo("jira-changes", "");
                                                setVariableValueToObject(mojo, "skip", true);
                                                setVariableValueToObject(mojo, "other", "set");
                                            }
                                        }
                
                """, """
                                        import org.apache.maven.plugin.testing.AbstractMojoTestCase;
                                        import org.apache.maven.api.plugin.testing.MojoParameter;
                                        import org.apache.maven.plugin.Mojo;
                
                                        public class MojoTest extends AbstractMojoTestCase {
                                            @MojoParameter(name = "skip", value = "true")
                                            @MojoParameter(name = "other", value = "set")
                                            public void testModelloConvertersMojo() throws Exception {
                                                Mojo mojo = lookupMojo("jira-changes", "");
                                            }
                                        }
                
                """


        ));
    }
    @Test
    void skipComplexObject() {
            rewriteRun(java("""
                                            import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
                                            import org.apache.maven.plugin.testing.AbstractMojoTestCase;
                                            import org.apache.maven.plugin.Mojo;
                    
                                            public class MojoTest extends AbstractMojoTestCase {
                                                public void testModelloConvertersMojo() throws Exception {
                                                    Mojo mojo = lookupMojo("jira-changes", "");
                                                    setVariableValueToObject(mojo, "project", new MavenProjectStub());
                                                }
                                            }
                    
                    """));
    }

}