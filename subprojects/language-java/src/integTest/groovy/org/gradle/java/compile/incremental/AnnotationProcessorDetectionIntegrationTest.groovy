/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.java.compile.incremental

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.fixtures.CompilationOutputsFixture

class AnnotationProcessorDetectionIntegrationTest extends AbstractIntegrationSpec {

    CompilationOutputsFixture outputs

    def "presence of non-incremental AP should preclude incremental build"() {
        given:
        incapProject()

        when:
        succeeds 'build'

        then:
        executedAndNotSkipped ':inc:compileJava'
        executedAndNotSkipped ':noninc:compileJava'
        executedAndNotSkipped ':app:compileJava'

        // TODO(stevey):
        //  - make sure these files are created:
        //   ./app/build/generated-sources/AppIncremental.java
        //   ./app/build/generated-sources/AppNonIncremental.java

        // Then modify ./app/src/main/java/App.java in some trivial way and:
        when:
        succeeds 'build'

        //   - verify that it was NON-incremental, but that :app was rebuilt:
        skipped ':inc:compileJava'
        skipped ':noninc:compileJava'

        ///    - output should warn about nonIncapProcessor being non-incremental

        // Other tests:
        // when:
        //   - change app/build.gradle to remove @NonIncremental annotationProcessor
        //   - verify that the output says "all annotation processors are incremental"

        // Add a test to verify correct behavior for Issue #105.
    }

    private void incapProject() {
        incapProcessor()
        nonIncapProcessor()
        appWithIncap()
    }

    private void appWithIncap() {
        subproject('app') {
            'build.gradle'("""
                apply plugin: 'java'

                configurations {
                  annotationProcessor
                }

                dependencies {
                  compile project(':inc')
                  compile project(':noninc')
                  annotationProcessor project(':inc')
                  annotationProcessor project(':noninc')
                }

                compileJava {
                  // Use forking to work around javac's jar cache
                  options.fork = true
                  options.annotationProcessorPath = configurations.annotationProcessor
                  options.annotationProcessorGeneratedSourcesDirectory = file("build/generated-sources")
                }
            """)
            src {
                main {
                    java {
                        'App.java'("""
                           @Incremental
                           @NonIncremental
                           class App { }
                         """)
                    }
                }
            }
        }
    }
    
    private void incapProcessor() {
        subproject('inc') {
            'build.gradle'("""
             apply plugin: 'java-library'

             repositories {
               maven {
                 url 'https://dl.bintray.com/incap/incap'
               }
             }

             dependencies {
               api deps.incap
             }
            """)
            src {
                main {
                    java {
                        'IncapProcessor.java'("\n${incapProcessorClass()}")
                        'Incremental.java'("public @interface Incremental { }")
                    }
                    resources {
                        'META-INF' {
                            services {
                                'javax.annotation.processing.Processor'("IncapProcessor")
                            }
                            'incap'("")
                        }
                    }
                }
            }
        }
    }

    private void nonIncapProcessor() {
        subproject('noninc') {
            'build.gradle'("""
             apply plugin: 'java-library'
            """)
            src {
                main {
                    java {
                        'NonIncapProcessor.java'("\n${nonIncapProcessorClass()}")
                        'NonIncremental.java'("public @interface NonIncremental { }")
                    }
                    resources {
                        'META-INF' {
                            services {
                                'javax.annotation.processing.Processor'("NonIncapProcessor")
                            }
                        }
                    }
                }
            }
        }
    }

    // Incap-compliant annotation processor.  Handles an "@Incremental" annotation.
    private String incapProcessorClass() {
        """
        import java.util.Set;
        import java.util.Collections;
        import java.io.Writer;
        import javax.lang.model.SourceVersion;
        import javax.lang.model.util.Elements;
        import javax.annotation.processing.Filer;
        import javax.annotation.processing.Messager;
        import javax.lang.model.element.Element;
        import javax.lang.model.element.TypeElement;
        import javax.tools.JavaFileObject;
        import javax.annotation.processing.ProcessingEnvironment;
        import javax.annotation.processing.RoundEnvironment;
        import javax.tools.Diagnostic;
        import org.gradle.incap.BaseIncrementalAnnotationProcessor;

        public class IncapProcessor extends BaseIncrementalAnnotationProcessor {
            private Elements elementUtils;
            private Filer filer;
            private Messager messager;

            @Override
            public Set<String> getSupportedAnnotationTypes() {
                return Collections.singleton(Incremental.class.getName());
            }

            @Override
            public SourceVersion getSupportedSourceVersion() {
                return SourceVersion.latestSupported();
            }

            @Override
            public synchronized void init(ProcessingEnvironment processingEnv) {
                elementUtils = processingEnv.getElementUtils();
                messager = processingEnv.getMessager();
                super.init(processingEnv);
            }

            @Override
            public boolean incrementalProcess(Set<? extends TypeElement> elements, RoundEnvironment roundEnv) {
                filer = incrementalProcessingEnvironment.getFiler();
                for (TypeElement annotation : elements) {
                    if (annotation.getQualifiedName().toString().equals(Incremental.class.getName())) {
                        for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                            TypeElement typeElement = (TypeElement) element;
                            String helperName = typeElement.getSimpleName().toString() + "Incremental";
                            try {
                                JavaFileObject sourceFile = filer.createSourceFile(helperName, element);
                                Writer writer = sourceFile.openWriter();
                                try {
                                    writer.write("class " + helperName + " {");
                                    writer.write("    String getValue() { return \"incremental\"; }");
                                    writer.write("}");
                                } finally {
                                    writer.close();
                                }
                            } catch (Exception e) {
                                messager.printMessage(Diagnostic.Kind.ERROR, "Failed to generate source file " + helperName, element);
                            }
                        }
                    }
                }
                return true;
            }
        }
        """
    }

    // A regular annotation processor.  Handles a "@NonIncremental" annotation.
    private String nonIncapProcessorClass() {
        """
        import javax.annotation.processing.AbstractProcessor;
        import java.util.Set;
        import java.util.Collections;
        import java.io.Writer;
        import javax.lang.model.SourceVersion;
        import javax.lang.model.util.Elements;
        import javax.annotation.processing.Filer;
        import javax.annotation.processing.Messager;
        import javax.lang.model.element.Element;
        import javax.lang.model.element.TypeElement;
        import javax.tools.JavaFileObject;
        import javax.annotation.processing.ProcessingEnvironment;
        import javax.annotation.processing.RoundEnvironment;
        import javax.tools.Diagnostic;

        public class NonIncapProcessor extends AbstractProcessor {
            private Elements elementUtils;
            private Filer filer;
            private Messager messager;

            @Override
            public Set<String> getSupportedAnnotationTypes() {
                return Collections.singleton(NonIncremental.class.getName());
            }

            @Override
            public SourceVersion getSupportedSourceVersion() {
                return SourceVersion.latestSupported();
            }

            @Override
            public synchronized void init(ProcessingEnvironment processingEnv) {
                elementUtils = processingEnv.getElementUtils();
                filer = processingEnv.getFiler();
                messager = processingEnv.getMessager();
            }

            @Override
            public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
                for (TypeElement annotation : annotations) {
                    if (annotation.getQualifiedName().toString().equals(NonIncremental.class.getName())) {
                        for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                            TypeElement typeElement = (TypeElement) element;
                            String helperName = typeElement.getSimpleName().toString() + "NonIncremental";
                            try {
                                JavaFileObject sourceFile = filer.createSourceFile(helperName, element);
                                Writer writer = sourceFile.openWriter();
                                try {
                                    writer.write("class " + helperName + " {");
                                    writer.write("    String getValue() { return \"non-incremental\"; }");
                                    writer.write("}");
                                } finally {
                                    writer.close();
                                }
                            } catch (Exception e) {
                                messager.printMessage(Diagnostic.Kind.ERROR, "Failed to generate source file " + helperName, element);
                            }
                        }
                    }
                }
                return true;
            }
        }
        """
    }
}
