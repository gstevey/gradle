/*
 * Copyright 2017 the original author or authors.
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

package org.gradle.workers.internal

import spock.lang.Unroll

class WorkerExecutorNestingIntegrationTest extends AbstractWorkerExecutorIntegrationTest {

    @Unroll
    def "workers with no isolation can spawn more work with #nestedIsolationMode"() {
        buildFile << """
            ${getRunnableWithNesting("IsolationMode.NONE", nestedIsolationMode)}
            task runInWorker(type: NestingWorkerTask)
        """.stripIndent()

        when:
        def gradle = executer.withTasks("runInWorker").start()

        then:
        gradle.waitForFinish()

        and:
        gradle.standardOutput.contains("Second level")

        where:
        nestedIsolationMode << ISOLATION_MODES
    }

    String getRunnableWithNesting(String isolationMode, String nestedIsolationMode) {
        return """
            import javax.inject.Inject
            import org.gradle.workers.WorkerExecutor

            class FirstLevelRunnable implements Runnable {
            
                WorkerExecutor executor
                
                @Inject
                public FirstLevelRunnable(WorkerExecutor executor) {
                    this.executor = executor
                }

                public void run() {
                    executor.submit(SecondLevelRunnable) {
                        isolationMode = $nestedIsolationMode
                    }
                }
            }

            class SecondLevelRunnable implements Runnable {
                
                @Inject
                public SecondLevelRunnable() {
                }

                public void run() {
                    System.out.println("Second level")
                }
            }

            class NestingWorkerTask extends DefaultTask {

                WorkerExecutor executor

                @Inject
                NestingWorkerTask(WorkerExecutor executor) {
                    this.executor = executor
                }

                @TaskAction
                public void runInWorker() {
                    executor.submit(FirstLevelRunnable) {
                        isolationMode = $isolationMode
                    }
                }
            }
        """.stripIndent()
    }
}
