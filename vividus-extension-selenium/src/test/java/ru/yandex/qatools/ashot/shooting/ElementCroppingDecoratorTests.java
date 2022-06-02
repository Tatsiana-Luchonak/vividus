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

package ru.yandex.qatools.ashot.shooting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openqa.selenium.WebDriver;
import org.vividus.selenium.screenshot.ScreenshotCropper;

@ExtendWith(MockitoExtension.class)
class ElementCroppingDecoratorTests
{
    @Mock private ShootingStrategy shootingStrategy;
    @Mock private BufferedImage image;
    @Mock private WebDriver webDriver;
    @Mock private ScreenshotCropper screenshotCropper;

    @Test
    void shouldGetScreenshot()
    {
        ElementCroppingDecorator decorator = new ElementCroppingDecorator(shootingStrategy, screenshotCropper, Map.of(),
                0);
        when(shootingStrategy.getScreenshot(webDriver)).thenReturn(image);
        when(screenshotCropper.getScreenshot(image, Map.of(), 0)).thenReturn(image);

        BufferedImage result = decorator.getScreenshot(webDriver, Set.of());
        assertEquals(image, result);
    }
}
