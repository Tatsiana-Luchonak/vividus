/*
 * Copyright 2019-2021 the original author or authors.
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

package org.vividus.steps;

import org.vividus.ui.web.action.IWindowsActions;

public enum WindowsStrategy
{
    CLOSE_ALL_EXCEPT_ONE
    {
        @Override
        public void apply(IWindowsActions windowsActions)
        {
            windowsActions.closeAllWindowsExceptOne();
        }
    },
    DO_NOTHING
    {
        @Override
        public void apply(IWindowsActions windowsActions)
        {
            // nothing to do
        }
    };

    public abstract void apply(IWindowsActions windowsActions);
}
