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

package org.vividus.selenium.screenshot;

import static ru.yandex.qatools.ashot.shooting.ShootingStrategies.cutting;

import java.util.Map;
import java.util.function.BiFunction;

import javax.inject.Inject;

import org.apache.commons.lang3.Validate;
import org.vividus.selenium.screenshot.strategies.ScreenshotShootingStrategy;
import org.vividus.ui.screenshot.ScreenshotParameters;

import ru.yandex.qatools.ashot.shooting.ShootingStrategies;
import ru.yandex.qatools.ashot.shooting.ShootingStrategy;
import ru.yandex.qatools.ashot.shooting.cutter.CutStrategy;
import ru.yandex.qatools.ashot.shooting.cutter.FixedCutStrategy;

public abstract class AbstractAshotFactory<T extends ScreenshotParameters> implements AshotFactory<T>
{
    private Map<String, ScreenshotShootingStrategy> strategies;
    private String screenshotShootingStrategy;

    protected ShootingStrategy decorateWithFixedCutStrategy(ShootingStrategy original, int headerToCut, int footerToCut)
    {
        return decorateWithCutStrategy(original, headerToCut, footerToCut, FixedCutStrategy::new);
    }

    protected ShootingStrategy decorateWithCutStrategy(ShootingStrategy original, int headerToCut, int footerToCut,
            BiFunction<Integer, Integer, CutStrategy> cutStrategyFactory)
    {
        return footerToCut > 0 || headerToCut > 0
                ? cutting(original, cutStrategyFactory.apply(headerToCut, footerToCut))
                : original;
    }

    protected ScreenshotShootingStrategy getStrategyBy(String strategyName)
    {
        ScreenshotShootingStrategy strategy = strategies.get(strategyName);
        Validate.isTrue(null != strategy, "Unable to find the strategy with the name: %s", strategyName);
        return strategy;
    }

    protected ShootingStrategy getBaseShootingStrategy()
    {
        return ShootingStrategies.scaling((float) this.getDpr());
    }

    protected abstract double getDpr();

    @Inject
    public void setStrategies(Map<String, ScreenshotShootingStrategy> strategies)
    {
        this.strategies = strategies;
    }

    protected String getScreenshotShootingStrategy()
    {
        return screenshotShootingStrategy;
    }

    public void setScreenshotShootingStrategy(String screenshotShootingStrategy)
    {
        this.screenshotShootingStrategy = screenshotShootingStrategy;
    }
}
