package dev.parsick.maven.rewrite.plexustestcase;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

public class ReplaceLookupTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ReplaceLookup()).typeValidationOptions(TypeValidation.none());
        spec.parser(JavaParser.fromJavaVersion()
                .classpathFromResources(new InMemoryExecutionContext(), "junit-4.13.2", "org.eclipse.sisu.plexus-0.9.0.M4", "plexus-build-api-1.2.0"));
    }

    @Test
    void lookup() {
        rewriteRun(java(
                """
                        import org.codehaus.plexus.build.BuildContext;
                        import org.codehaus.plexus.PlexusTestCase;

                        public class MojoTest extends PlexusTestCase {
                        
                            public void testModelloConvertersMojo() throws Exception {

                                BuildContext buildContext = (BuildContext) lookup(BuildContext.class);
                            }    
                        }
        """,
                """
                        import org.codehaus.plexus.build.BuildContext;
                        import org.codehaus.plexus.PlexusTestCase;
                        
                        import javax.inject.Inject;
                        
                        public class MojoTest extends PlexusTestCase {
                        
                            @Inject
                            private BuildContext buildContext;
                            
                            public void testModelloConvertersMojo() throws Exception {  
                            }
                        }
        """
        ));
    }
}
