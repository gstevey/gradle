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

import org.gradle.api.internal.file.FileResolver;
import org.gradle.ide.xcode.XcodeExtension;
import org.gradle.internal.reflect.Instantiator;

import javax.inject.Inject;

public class DefaultXcodeExtension implements XcodeExtension {
    private final DefaultXcodeProject project;

    @Inject
    public DefaultXcodeExtension(Instantiator instantiator, FileResolver fileResolver) {
        this.project = instantiator.newInstance(DefaultXcodeProject.class, instantiator, fileResolver);
    }

    @Override
    public DefaultXcodeProject getProject() {
        return project;
    }
}
