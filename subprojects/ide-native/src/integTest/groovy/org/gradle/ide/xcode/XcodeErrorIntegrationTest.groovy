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

package org.gradle.ide.xcode

import org.gradle.ide.xcode.fixtures.AbstractXcodeIntegrationSpec
import org.gradle.util.Requires
import org.gradle.util.TestPrecondition

import static org.gradle.util.Matchers.containsText

class XcodeErrorIntegrationTest extends AbstractXcodeIntegrationSpec {
    @Requires(TestPrecondition.XCODE)
    def "fails to build when project code is broken"() {
        executer.requireGradleDistribution()

        given:
        buildFile << """
            apply plugin: 'swift-executable'
         """

        and:
        file("src/main/swift/broken.swift") << "broken!"

        and:
        succeeds("xcode")

        expect:
        def failure = xcodebuild
            .withProject(rootXcodeProject)
            .withScheme("App Executable")
            .fails()
        failure.assertHasDescription("Execution failed for task ':compileDebugSwift'.")
        failure.assertHasCause("A build operation failed.")
        failure.assertThatCause(containsText("Swift compiler failed while compiling swift file(s)"))
    }
}