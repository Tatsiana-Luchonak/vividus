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

package org.vividus.visual.model;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;

import org.openqa.selenium.SearchContext;
import org.vividus.ui.action.search.Locator;
import org.vividus.ui.screenshot.ScreenshotParameters;
import org.vividus.visual.screenshot.IgnoreStrategy;

public class VisualCheck
{
    private String baselineName;
    private VisualActionType action;
    private OptionalDouble acceptableDiffPercentage = OptionalDouble.empty();
    private OptionalDouble requiredDiffPercentage = OptionalDouble.empty();
    private Map<IgnoreStrategy, Set<Locator>> elementsToIgnore = Map.of();
    private Optional<ScreenshotParameters> screenshotParameters = Optional.empty();
    private SearchContext searchContext;

    public VisualCheck()
    {
      // Necessary for JBehave object instantiation;
    }

    public VisualCheck(String baselineName, VisualActionType action)
    {
        this.baselineName = baselineName;
        this.action = action;
    }

    public String getBaselineName()
    {
        return baselineName;
    }

    public Map<IgnoreStrategy, Set<Locator>> getElementsToIgnore()
    {
        return elementsToIgnore;
    }

    public void setElementsToIgnore(Map<IgnoreStrategy, Set<Locator>> elementsToIgnore)
    {
        this.elementsToIgnore = elementsToIgnore;
    }

    public OptionalDouble getAcceptableDiffPercentage()
    {
        return acceptableDiffPercentage;
    }

    public void setAcceptableDiffPercentage(OptionalDouble acceptableDiffPercentage)
    {
        this.acceptableDiffPercentage = acceptableDiffPercentage;
    }

    public VisualActionType getAction()
    {
        return action;
    }

    public Optional<ScreenshotParameters> getScreenshotParameters()
    {
        return screenshotParameters;
    }

    public void setScreenshotParameters(Optional<ScreenshotParameters> screenshotParameters)
    {
        this.screenshotParameters = screenshotParameters;
    }

    public SearchContext getSearchContext()
    {
        return searchContext;
    }

    public void setSearchContext(SearchContext searchContext)
    {
        this.searchContext = searchContext;
    }

    public OptionalDouble getRequiredDiffPercentage()
    {
        return requiredDiffPercentage;
    }

    public void setRequiredDiffPercentage(OptionalDouble requiredDiffPercentage)
    {
        this.requiredDiffPercentage = requiredDiffPercentage;
    }
}
