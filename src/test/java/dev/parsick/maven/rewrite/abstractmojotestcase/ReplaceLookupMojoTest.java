package dev.parsick.maven.rewrite.abstractmojotestcase;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ReplaceLookupMojoTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ReplaceLookupMojo());
        spec.parser(JavaParser.fromJavaVersion()
                .classpathFromResources(new InMemoryExecutionContext(), "junit-4.13.2", "org.eclipse.sisu.plexus-0.9.0.M4", "plexus-build-api-1.2.0", "maven-plugin-testing-harness-3.4.0-SNAPSHOT", "maven-plugin-api-3.9.11"));
    }


    @Test
    void doNothing1() {
        rewriteRun(java(
                """
                                        public class MojoTest {
                        
                                        }
                        """));
    }

    @Test
    void doNothing2() {
        rewriteRun(java(
                """
                                        public class MojoTest {
                                            public void testModelloConvertersMojo() throws Exception {
                        
                                            }    
                                        }
                        """));
    }


    @Test
    void doNothing3() {
        rewriteRun(java(
                """
                                        public class MojoTest {
                                            public void testModelloConvertersMojo() throws Exception {
                                                    lookupMojo("","");
                                            }    
                        
                                            private void lookupMojo(String x, String y) throws Exception {}
                                        }
                        """));
    }

    @Test
    void lookupMojo() {
        rewriteRun(java(
                """
                                        import org.apache.maven.plugin.testing.AbstractMojoTestCase;
                                        import org.apache.maven.plugin.Mojo;
                                        
                                        public class MojoTest extends AbstractMojoTestCase {
                                            public void testModelloConvertersMojo() throws Exception {
                                                Mojo mojo = lookupMojo("jira-changes", "src/test/resources/plugin-configs/check-plugin-config.xml");
                                            }    
                                        }
                        """,
                """
                                        import org.apache.maven.plugin.testing.AbstractMojoTestCase;
                                        import org.apache.maven.api.plugin.testing.InjectMojo;
                                        import org.apache.maven.plugin.Mojo;
                                        
                                        public class MojoTest extends AbstractMojoTestCase {
                                            @InjectMojo(goal = "jira-changes", pom = "src/test/resources/plugin-configs/check-plugin-config.xml")
                                            public void testModelloConvertersMojo(Mojo mojo) throws Exception {
                                            }    
                                        }"""));
    }

    @Test
    void lookupMojoWithPomFile() {
        rewriteRun(java(
                """
                                        import java.io.File;
                                        import org.apache.maven.plugin.testing.AbstractMojoTestCase;
                                        import org.apache.maven.plugin.Mojo;
                                        
                                        public class MojoTest extends AbstractMojoTestCase {
                                            public void testModelloConvertersMojo() throws Exception {
                                                File pluginXmlFile = new File(getBasedir(), "src/test/resources/plugin-configs/check-plugin-config.xml");
                
                                                Mojo mojo = lookupMojo("check", pluginXmlFile);
                                            }    
                                        }
                        """,
                """
                                        import org.apache.maven.plugin.testing.AbstractMojoTestCase;
                                        import org.apache.maven.api.plugin.testing.InjectMojo;
                                        import org.apache.maven.plugin.Mojo;
                                        
                                        public class MojoTest extends AbstractMojoTestCase {
                                            @InjectMojo(goal = "check", pom = "src/test/resources/plugin-configs/check-plugin-config.xml")
                                            public void testModelloConvertersMojo(Mojo mojo) throws Exception {
                                            }    
                                        }"""));
    }
}