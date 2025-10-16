package dev.parsick.maven.rewrite.plexustestcase;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class AddPlexusTestConfigurationTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddPlexusTestConfiguration());
        spec.parser(JavaParser.fromJavaVersion()
                .classpathFromResources(new InMemoryExecutionContext(), "junit-4.13.2",
                        "org.eclipse.sisu.plexus-0.9.0.M4", "plexus-build-api-1.2.0", "javax.inject-1"));

    }

    @Test
    void doNothing() {
        rewriteRun(java(
                """
                        
                        import org.codehaus.plexus.PlexusTestCase;
                                               
                        public class MojoTest extends PlexusTestCase {
                            public void testModelloConvertersMojo() throws Exception {

                            }
                        }
                        
                         
        """));
    }

    @Test
    void customizeConfiguration() {
        rewriteRun(java(
                """
                        
                        import org.codehaus.plexus.PlexusTestCase;
                        import org.codehaus.plexus.ContainerConfiguration;
                                               
                        public class MojoTest extends PlexusTestCase {
                            public void testModelloConvertersMojo() throws Exception {

                            }
                            
                            @Override
                            protected void customizeContainerConfiguration(ContainerConfiguration containerConfiguration) {
                                
                            }
                        }""",
                """
                        import org.codehaus.plexus.PlexusTestCase;
                        import org.codehaus.plexus.testing.PlexusTestConfiguration;
                        import org.codehaus.plexus.ContainerConfiguration;
                        
                        public class MojoTest extends PlexusTestCase implements PlexusTestConfiguration {
                            public void testModelloConvertersMojo() throws Exception {

                            }
                            
                            @Override
                            protected void customizeContainerConfiguration(ContainerConfiguration containerConfiguration) {
                                
                            }
                        }"""
        ));
    }

    @Test
    void customizeContext() {
        rewriteRun(java(
                """
                        
                        import org.codehaus.plexus.PlexusTestCase;
                        import org.codehaus.plexus.context.Context;
                                               
                        public class MojoTest extends PlexusTestCase {
                            public void testModelloConvertersMojo() throws Exception {

                            }
                            
                            @Override
                            protected void customizeContext(Context context) {
                                
                            }
                        }""",
                """
                        import org.codehaus.plexus.PlexusTestCase;
                        import org.codehaus.plexus.context.Context;
                        import org.codehaus.plexus.testing.PlexusTestConfiguration;
                        
                        public class MojoTest extends PlexusTestCase implements PlexusTestConfiguration {
                            public void testModelloConvertersMojo() throws Exception {

                            }
                            
                            @Override
                            protected void customizeContext(Context context) {
                                
                            }
                        }"""
        ));
    }


    @Test
    void doOnce() {
        rewriteRun(java(
                """
                        
                        import org.codehaus.plexus.PlexusTestCase;
                        import org.codehaus.plexus.context.Context;
                        import org.codehaus.plexus.ContainerConfiguration;
                                               
                        public class MojoTest extends PlexusTestCase {
                            public void testModelloConvertersMojo() throws Exception {

                            }
                            
                            @Override
                            protected void customizeContext(Context context) {
                                
                            }
                            
                            @Override
                            protected void customizeContainerConfiguration(ContainerConfiguration containerConfiguration) {
                                
                            }
                        }""",
                """
                        import org.codehaus.plexus.PlexusTestCase;
                        import org.codehaus.plexus.context.Context;
                        import org.codehaus.plexus.testing.PlexusTestConfiguration;
                        import org.codehaus.plexus.ContainerConfiguration;
                        
                        public class MojoTest extends PlexusTestCase implements PlexusTestConfiguration {
                            public void testModelloConvertersMojo() throws Exception {

                            }
                            
                            @Override
                            protected void customizeContext(Context context) {
                                
                            }
                            
                            @Override
                            protected void customizeContainerConfiguration(ContainerConfiguration containerConfiguration) {
                                
                            }
                        }"""
        ));
    }



}