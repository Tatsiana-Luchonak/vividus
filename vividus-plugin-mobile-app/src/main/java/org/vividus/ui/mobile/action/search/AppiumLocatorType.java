/*
 * Copyright 2019-2020 the original author or authors.
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

package org.vividus.ui.mobile.action.search;

import org.vividus.ui.action.search.IElementAction;
import org.vividus.ui.action.search.LocatorType;

public enum AppiumLocatorType implements LocatorType
{
    XPATH("Appium XPath", ByAppiumLocatorSearch.class),
    ACCESSIBILITY_ID("Accessibility Id", ByAppiumLocatorSearch.class);

    private final String attributeName;
    private final Class<? extends IElementAction> actionClass;

    AppiumLocatorType(String attributeName, Class<? extends IElementAction> actionClass)
    {
        this.attributeName = attributeName;
        this.actionClass = actionClass;
    }

    @Override
    public String getKey()
    {
        return this.name();
    }

    @Override
    public String getAttributeName()
    {
        return attributeName;
    }

    @Override
    public Class<? extends IElementAction> getActionClass()
    {
        return actionClass;
    }
}