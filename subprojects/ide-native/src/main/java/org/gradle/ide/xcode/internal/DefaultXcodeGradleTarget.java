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

package org.gradle.ide.xcode.internal;

import org.gradle.ide.xcode.XcodeGradleTarget;

public class DefaultXcodeGradleTarget extends DefaultXcodeTarget implements XcodeGradleTarget {
    private String taskName;
    private String gradleCommand;

    public DefaultXcodeGradleTarget(String name) {
        super(name);
    }

    @Override
    public String getTaskName() {
        return taskName;
    }

    @Override
    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    @Override
    public String getGradleCommand() {
        return gradleCommand;
    }

    @Override
    public void setGradleCommand(String gradleCommand) {
        this.gradleCommand = gradleCommand;
    }
}
