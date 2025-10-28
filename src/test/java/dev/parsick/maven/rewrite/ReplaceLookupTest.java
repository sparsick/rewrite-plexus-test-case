package dev.parsick.maven.rewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class ReplaceLookupTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ReplaceLookup());
        spec.parser(JavaParser.fromJavaVersion()
                .classpathFromResources(new InMemoryExecutionContext(), "junit-4.13.2", "org.eclipse.sisu.plexus-0.9.0.M4", "plexus-build-api-1.2.0", "javax.inject-1", "maven-plugin-testing-harness-3.4.0-SNAPSHOT", "maven-plugin-api-3.9.11"));
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
                                                    lookup();
                                            }    
                                            
                                            private void lookup() throws Exception {}
                                        }
                        """));
    }

    @Test
    void doNothing4() {
        rewriteRun(java(
                """             
                                        import java.util.List;
                                        import java.util.ArrayList;
                                           
                                        public class CustomList extends ArrayList<String>
                                                implements List<String> {
                                        }
                        """));
    }

    @Test
    void doNothing5() {
        rewriteRun(java(
                """             
                                        import org.apache.maven.plugin.testing.AbstractMojoTestCase;
                                        import org.apache.maven.plugin.Mojo;
                                        
                                        public class MojoTest extends AbstractMojoTestCase {
                                            public void testModelloConvertersMojo() throws Exception {
                                                Mojo mojo = lookupMojo("jira-changes", "");
                                            }    
                                        }
                        """));
    }

    @Test
    void lookupPlexusTestCase() {
        rewriteRun(java(
                """
                        import org.codehaus.plexus.build.BuildContext;
                        import org.codehaus.plexus.PlexusTestCase;

                        public class MojoTest extends PlexusTestCase {
                        
                            public void testModelloConvertersMojo() throws Exception {

                                BuildContext buildContext = (BuildContext) lookup(BuildContext.class);
                                BuildContext buildContext1 = (BuildContext) lookup(BuildContext.class);
                            }    
                        }
        """,
                """
                        import org.codehaus.plexus.build.BuildContext;
                        import org.codehaus.plexus.PlexusTestCase;
                        
                        import javax.inject.Inject;
                        
                        public class MojoTest extends PlexusTestCase {
                        
                            @Inject
                            private BuildContext buildContext1;
                            
                            @Inject
                            private BuildContext buildContext;
                            
                            public void testModelloConvertersMojo() throws Exception {  
                            }
                        }
        """
        ));
    }

    @Test
    void lookupAbstractMojoTestCase() {
        rewriteRun(java(
                """
                        import org.codehaus.plexus.build.BuildContext;
                        import org.apache.maven.plugin.testing.AbstractMojoTestCase;

                        public class MojoTest extends AbstractMojoTestCase {
                        
                            public void testModelloConvertersMojo() throws Exception {

                                BuildContext buildContext = (BuildContext) lookup(BuildContext.class);
                                BuildContext buildContext1 = (BuildContext) lookup(BuildContext.class);
                            }    
                        }
        """,
                """
                        import org.codehaus.plexus.build.BuildContext;
                        import org.apache.maven.plugin.testing.AbstractMojoTestCase;
                        
                        import javax.inject.Inject;
                        
                        public class MojoTest extends AbstractMojoTestCase {
                        
                            @Inject
                            private BuildContext buildContext1;
                            
                            @Inject
                            private BuildContext buildContext;
                            
                            public void testModelloConvertersMojo() throws Exception {  
                            }
                        }
        """
        ));
    }
}
