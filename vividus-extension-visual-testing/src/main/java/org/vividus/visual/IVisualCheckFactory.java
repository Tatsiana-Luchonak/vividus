/*
 * Copyright 2019-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vividus.visual;

import java.util.Optional;
import java.util.function.BiFunction;

import org.vividus.ui.screenshot.ScreenshotConfiguration;
import org.vividus.visual.model.VisualActionType;
import org.vividus.visual.model.VisualCheck;

public interface IVisualCheckFactory
{
    VisualCheck create(String baselineName, VisualActionType actionType);

    VisualCheck create(String baselineName, VisualActionType actionType,
            Optional<ScreenshotConfiguration> configuration);

    <T extends VisualCheck> T create(String baselineName, VisualActionType actionType, BiFunction<String,
            VisualActionType, T> checkFactory);
}
