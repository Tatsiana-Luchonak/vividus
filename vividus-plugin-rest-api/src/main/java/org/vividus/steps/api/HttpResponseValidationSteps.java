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

package org.vividus.steps.api;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.tika.Tika;
import org.hamcrest.Matchers;
import org.jbehave.core.annotations.Alias;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.jbehave.core.model.ExamplesTable;
import org.jbehave.core.steps.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vividus.context.VariableContext;
import org.vividus.http.ConnectionDetails;
import org.vividus.http.HttpTestContext;
import org.vividus.http.client.HttpResponse;
import org.vividus.model.ArchiveVariable;
import org.vividus.model.NamedEntry;
import org.vividus.softassert.ISoftAssert;
import org.vividus.steps.ByteArrayValidationRule;
import org.vividus.steps.ComparisonRule;
import org.vividus.steps.DataWrapper;
import org.vividus.steps.StringComparisonRule;
import org.vividus.steps.SubSteps;
import org.vividus.util.ResourceUtils;
import org.vividus.util.json.JsonUtils;
import org.vividus.util.wait.DurationBasedWaiter;
import org.vividus.util.wait.WaitMode;
import org.vividus.util.zip.ZipUtils;
import org.vividus.variable.VariableScope;

public class HttpResponseValidationSteps
{
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpResponseValidationSteps.class);
    private static final String HTTP_RESPONSE_STATUS_CODE = "HTTP response status code";
    private static final Tika TIKA = new Tika();

    private final HttpTestContext httpTestContext;
    private final VariableContext variableContext;
    private final ISoftAssert softAssert;
    private final JsonUtils jsonUtils;

    public HttpResponseValidationSteps(HttpTestContext httpTestContext, VariableContext variableContext,
            ISoftAssert softAssert, JsonUtils jsonUtils)
    {
        this.httpTestContext = httpTestContext;
        this.variableContext = variableContext;
        this.softAssert = softAssert;
        this.jsonUtils = jsonUtils;
    }

    /**
     * Checks HTTP response does not contain body
     * <p>
     * <b>Actions performed at this step:</b>
     * </p>
     * <ul>
     * <li>Checks that there is no body in response</li>
     * </ul>
     * <p><i>Step can't be used without previous step with HTTP-method</i></p>
     */
    @Then("the response does not contain body")
    public void doesResponseNotContainBody()
    {
        performIfHttpResponseIsPresent(
            response -> softAssert.assertNull("The response does not contain body", response.getResponseBody()));
    }

    /**
     * Checks HTTP response body matches to the specified content according to the provided string validation rule
     * <p>
     * <b>Actions performed at this step:</b>
     * </p>
     * <ul>
     * <li>Retrieves response body from HTTP response</li>
     * <li>Checks that response matches to the specified content according to the provided string validation rule</li>
     * </ul>
     * <p><i>Step can't be used without previous step with HTTP-method</i></p>
     * @param comparisonRule String validation rule: "is equal to", "contains", "does not contain"
     * @param content body content part
     */
    @Then("the response body $comparisonRule '$content'")
    public void doesResponseBodyMatch(StringComparisonRule comparisonRule, String content)
    {
        performIfHttpResponseIsPresent(response -> softAssert.assertThat("HTTP response body",
                response.getResponseBodyAsString(), comparisonRule.createMatcher(content)));
    }

    /**
     * Checks content type of HTTP response body matches to the specified expected content type according to
     * the provided string validation rule
     * @param comparisonRule String validation rule: "is equal to", "contains", "does not contain"
     * @param contentType Expected content type, e.g. text/html, application/xml and etc.
     */
    @Then("content type of response body $comparisonRule `$contentType`")
    public void assertContentTypeOfResponseBody(StringComparisonRule comparisonRule, String contentType)
    {
        performIfHttpResponseIsPresent(response ->
        {
            byte[] responseBody = response.getResponseBody();
            String actualContentType = TIKA.detect(responseBody);
            if ("text/plain".equals(actualContentType) && jsonUtils.isJson(
                    new String(responseBody, StandardCharsets.UTF_8)))
            {
                actualContentType = "application/json";
            }
            softAssert.assertThat("Content type of response body", actualContentType,
                    comparisonRule.createMatcher(contentType));
        });
    }

    /**
     * Checks HTTP response body as bytes matches to the specified content as bytes according to the provided
     * validation rule
     * <p>
     * <b>Actions performed at this step:</b>
     * </p>
     * <ul>
     * <li>Retrieves response body from HTTP response</li>
     * <li>Checks that response as bytes matches to the specified content as bytes according to the provided validation
     * rule</li>
     * </ul>
     * <p><i>Step can't be used without previous step with HTTP-method</i></p>
     * @param validationRule validation rule: "IS_EQUAL_TO", "IS_NOT_EQUAL_TO"
     * @param resourcePath path to the resource
     */
    @Then(value = "the response body $validationRule resource at '$resourcePath'", priority = 1)
    public void doesResponseBodyMatchResource(ByteArrayValidationRule validationRule, String resourcePath)
    {
        performIfHttpResponseIsPresent(response -> validationRule.assertMatchesRule(softAssert,
                ResourceUtils.loadResourceAsByteArray(getClass(), resourcePath), response.getResponseBody()));
    }

    /**
     * Compare size of decompressed HTTP response body with value <b>sizeInBytes</b>
     * @param comparisonRule The rule to match the variable value. The supported rules:
     *                       <ul>
     *                       <li>less than (&lt;)</li>
     *                       <li>less than or equal to (&lt;=)</li>
     *                       <li>greater than (&gt;)</li>
     *                       <li>greater than or equal to (&gt;=)</li>
     *                       <li>equal to (=)</li>
     *                       <li>not equal to (!=)</li>
     *                       </ul>
     * @param sizeInBytes    The expected size of the response body in bytes
     */
    @Then("size of decompressed response body is $comparisonRule `$sizeInBytes`")
    public void doesDecompressedResponseBodySizeConfirmRule(ComparisonRule comparisonRule, int sizeInBytes)
    {
        performIfHttpResponseIsPresent(response ->
            softAssert.assertThat("Size of decompressed HTTP response body", response.getResponseBody().length,
                    comparisonRule.getComparisonRule(sizeInBytes)));
    }

    /**
     * This step should be preceded with the step executing HTTP request
     * Step asserts that the response has the expected HTTP <b>responseCode</b>
     * @param comparisonRule The rule to match the variable value. The supported rules:
     *                       <ul>
     *                       <li>less than (&lt;)</li>
     *                       <li>less than or equal to (&lt;=)</li>
     *                       <li>greater than (&gt;)</li>
     *                       <li>greater than or equal to (&gt;=)</li>
     *                       <li>equal to (=)</li>
     *                       <li>not equal to (!=)</li>
     *                       </ul>
     * @param responseCode   The expected response code (for example 200, 404)
     */
    @Then("the response code is $comparisonRule '$responseCode'")
    public void assertResponseCode(ComparisonRule comparisonRule, int responseCode)
    {
        performIfHttpResponseIsPresent(response -> softAssert.assertThat(HTTP_RESPONSE_STATUS_CODE,
                response.getStatusCode(), comparisonRule.getComparisonRule(responseCode)));
    }

    /**
     * This step should be preceded with the step
     * 'When I issue a HTTP GET request for a resource with the URL '$url''
     * Step is validating the response time in milliseconds
     * @param responseTimeThresholdMs maximum response time in milliseconds
     */
    @Then("the response time should be less than '$responseTimeThresholdMs' milliseconds")
    public void thenTheResponseTimeShouldBeLessThan(long responseTimeThresholdMs)
    {
        performIfHttpResponseIsPresent(
            response -> softAssert.assertThat("The response time is less than response time threshold.",
                    response.getResponseTimeInMs(), Matchers.lessThan(responseTimeThresholdMs)));
    }

    /**
     * Validates that response <b>header</b> contains expected <b>attributes</b>.
     * <p>
     * <b>Actions performed at this step:</b>
     * </p>
     * <ul>
     * <li>Checks that HTTP response header contains expected attributes.</li>
     * </ul>
     * @param httpHeaderName HTTP header name. For example, <b>Content-Type</b>
     * @param attributes expected HTTP header attributes. For example, <b>charset=utf-8</b>
     */
    @Then("response header '$httpHeaderName' contains attribute: $attributes")
    public void assertHeaderContainsAttributes(String httpHeaderName, ExamplesTable attributes)
    {
        performIfHttpResponseIsPresent(response ->
        {
            getHeaderByName(response, httpHeaderName).ifPresent(header ->
            {
                List<String> actualAttributes = Stream.of(header.getElements()).map(HeaderElement::getName)
                        .collect(Collectors.toList());
                for (Parameters row : attributes.getRowsAsParameters(true))
                {
                    String expectedAttribute = row.valueAs("attribute", String.class);
                    softAssert.assertThat(httpHeaderName + " header contains " + expectedAttribute + " attribute",
                            actualAttributes, contains(expectedAttribute));
                }
            });
        });
    }

    /**
     * Validates that the value of specified response header matches to the expected value according to the
     * provided string validation rule
     * <p><b>Actions performed by this step:</b></p>
     * <ul><li>Checks that HTTP response header value matches to the expected one.</li></ul>
     * @param httpHeaderName HTTP header name. For example, <b>Content-Type</b>
     * @param comparisonRule String validation rule: "is equal to", "contains", "does not contain"
     * @param value expected HTTP header value. For example, <b>text/html</b>
     */
    @Then("the value of the response header '$httpHeaderName' $comparisonRule '$value'")
    @Alias("the value of the response header \"$httpHeaderName\" $comparisonRule \"$value\"")
    public void doesHeaderMatch(String httpHeaderName, StringComparisonRule comparisonRule, String value)
    {
        performIfHttpResponseIsPresent(response -> getHeaderValueByName(response, httpHeaderName)
                .ifPresent(actualValue -> softAssert.assertThat("'" + httpHeaderName + "' header value", actualValue,
                        comparisonRule.createMatcher(value))));
    }

    /**
     * Checks whether the response contains specified number of headers with the name <b>headerName</b>
     * @param headerName     HTTP header name. For example, <b>Content-Type</b>
     * @param comparisonRule The rule to match the quantity of headers. The supported rules:
     *                       <ul>
     *                       <li>less than (&lt;)</li>
     *                       <li>less than or equal to (&lt;=)</li>
     *                       <li>greater than (&gt;)</li>
     *                       <li>greater than or equal to (&gt;=)</li>
     *                       <li>equal to (=)</li>
     *                       <li>not equal to (!=)</li>
     *                       </ul>
     * @param value          The number to compare with
     */
    @Then("the number of the response headers with the name '$headerName' is $comparisonRule $value")
    public void isHeaderWithNameFound(String headerName, ComparisonRule comparisonRule, int value)
    {
        performIfHttpResponseIsPresent(response -> softAssert.assertThat(
                String.format("The number of the response headers with the name '%s'", headerName),
                (int) response.getHeadersByName(headerName).count(), comparisonRule.getComparisonRule(value)));
    }

    /**
     * Saves the value of specified response header into the variable with given scope and name.
     * @param httpHeaderName HTTP header name. For example, <b>Content-Type</b>.
     * @param scopes The set (comma separated list of scopes e.g.: STORY, NEXT_BATCHES) of variable's scope<br>
     * <i>Available scopes:</i>
     * <ul>
     * <li><b>STEP</b> - the variable will be available only within the step,
     * <li><b>SCENARIO</b> - the variable will be available only within the scenario,
     * <li><b>STORY</b> - the variable will be available within the whole story,
     * <li><b>NEXT_BATCHES</b> - the variable will be available starting from next batch
     * </ul>
     * @param variableName A variable name
     */
    @When("I save response header '$httpHeaderName' value to $scopes variable '$variableName'")
    public void saveHeaderValue(String httpHeaderName, Set<VariableScope> scopes, String variableName)
    {
        performIfHttpResponseIsPresent(response -> getHeaderValueByName(response, httpHeaderName)
                .ifPresent(value -> variableContext.putVariable(scopes, variableName, value)));
    }

    /**
     * Saves the value of response body into the variable with given scope and name.
     * @param scopes The set (comma separated list of scopes e.g.: STORY, NEXT_BATCHES) of variable's scope<br>
     * <i>Available scopes:</i>
     * <ul>
     * <li><b>STEP</b> - the variable will be available only within the step,
     * <li><b>SCENARIO</b> - the variable will be available only within the scenario,
     * <li><b>STORY</b> - the variable will be available within the whole story,
     * <li><b>NEXT_BATCHES</b> - the variable will be available starting from next batch
     * </ul>
     * @param variableName A variable name
     */
    @When("I save response body to the $scopes variable '$variableName'")
    public void saveResponseBody(Set<VariableScope> scopes, String variableName)
    {
        performIfHttpResponseIsPresent(
            response -> variableContext.putVariable(scopes, variableName, response.getResponseBodyAsString()));
    }

    /**
     * This step should be preceded with any step executing HTTP request
     * The step validates that HTTP connection was secured with the expected protocol
     * @param securityProtocol expected security protocol, e.g. TLSv1.2
     */
    @Then("the connection is secured using $securityProtocol protocol")
    public void isConnectionSecured(String securityProtocol)
    {
        ConnectionDetails connectionDetails = httpTestContext.getConnectionDetails();
        if (softAssert.assertTrue("Connection is secure", connectionDetails.isSecure()))
        {
            softAssert.assertEquals("Security protocol", securityProtocol,
                    connectionDetails.getSecurityProtocol());
        }
    }

    /**
     * Saves content of a file from archive in response to context variable in specified format
     * Example:
     * <p>
     * <code>
     * When I save files content from the response archive into variables with parameters:<br>
     * |path                        |variableName|scopes   |outputFormat|
     * |files/2011-11-11/skyrim.json|entityJson  |SCENARIO |TEXT        |
     * </code>
     * </p>
     * Currently available types are TEXT and BASE64
     *
     * @param parameters describes saving parameters
     * @deprecated Use instead:
     * {@link org.vividus.archive.steps.ArchiveSteps#saveArchiveEntriesToVariables(DataWrapper, List)}
     */
    @When("I save content of the response archive entries to the variables:$parameters")
    @Deprecated(since = "0.4.10", forRemoval = true)
    public void saveFilesContentToVariables(List<ArchiveVariable> parameters)
    {
        LOGGER.warn("The step: \"When I save content of the response archive entries to the variables:$parameters\""
                + " is deprecated and will be removed in VIVIDUS 0.5.0."
                + " Use instead: When I save content of `$archiveData` archive entries to variables:$parameters");
        List<String> expectedEntries = parameters.stream().map(ArchiveVariable::getPath).collect(Collectors.toList());
        Map<String, byte[]> zipEntries = ZipUtils.readZipEntriesFromBytes(getResponseBody(), expectedEntries::contains);
        parameters.forEach(arcVar ->
        {
            String path = arcVar.getPath();
            Optional.ofNullable(zipEntries.get(path)).ifPresentOrElse(
                data -> variableContext.putVariable(arcVar.getScopes(), arcVar.getVariableName(),
                        arcVar.getOutputFormat().convert(data)),
                () -> softAssert.recordFailedAssertion(
                        String.format("Unable to find entry by name %s in response archive", path)));
        });
    }

    /**
     * Verifies that at least one (or no one) entry in a response archive matches the specified string comparison rule.
     * If comparison rule column does not exist,
     * the verification that archive entries have the specified names is performed.
     * <p>
     * Usage example:
     * </p>
     * <p>
     * <code>
     * Then the response archive contains entries with the names:$parameters<br>
     * |rule      |name                                    |<br>
     * |contains  |2011-11-11/skyrim.json                  |<br>
     * |matches   |files/2011-11-11/logs/papyrus\.\d+\.log |<br>
     * </code>
     * </p>
     *
     * @param parameters The ExampleTable that contains specified string comparison <b>rule</b> and entry <b>name</b>
     *                   pattern that should be found using current <b>rule</b>. Available columns:
     *                   <ul>
     *                   <li>rule - String comparison rule: "is equal to", "contains", "does not contain", "matches"
     *                   .</li>
     *                   <li>name - Desired entry name pattern used with current <b>rule</b>.</li>
     *                   </ul>
     * @deprecated Use instead:
     *             {@link org.vividus.archive.steps.ArchiveSteps#verifyArchiveContainsEntries(DataWrapper, List)}
     */
    @Then("response archive contains entries with names:$parameters")
    @Deprecated(since = "0.4.10", forRemoval = true)
    public void verifyArchiveContainsEntries(List<NamedEntry> parameters)
    {
        LOGGER.warn("The step: \"Then response archive contains entries with names:$parameters\" is deprecated and will"
                  + " be removed in VIVIDUS 0.5.0."
                  + " Use instead: Then `$archiveData` archive contains entries with names:$parameters");
        Set<String> entryNames = ZipUtils.readZipEntryNamesFromBytes(getResponseBody());

        parameters.forEach(entry ->
        {
            String expectedName = entry.getName();
            if (entry.getRule() != null)
            {
                StringComparisonRule comparisonRule = entry.getRule();
                softAssert.assertThat(String.format(
                        "The response archive contains entry matching the comparison rule '%s' with name pattern '%s'",
                        comparisonRule, expectedName), entryNames, hasItem(comparisonRule.createMatcher(expectedName)));
            }
            else
            {
                softAssert.assertThat("The response archive contains entry with name " + expectedName, entryNames,
                        hasItem(expectedName));
            }
        });
    }

    /**
     * Waits for the specified number of times until HTTP response code is equal to what is expected.
     * <p>
     * <b>Actions performed:</b>
     * </p>
     * <ul>
     * <li>Execute sub-steps</li>
     * <li>Check if HTTP response code is equal to what is expected</li>
     * </ul>
     * @param responseCode The expected HTTP status code.
     * @param duration The time duration to wait in ISO-8601 format.
     * @param retryTimes The number of times the request will be retried: `duration/retryTimes = timeout` is a polling
     * timeout between requests.
     * @param stepsToExecute The steps to execute at each wait iteration.
     */
    @When("I wait for response code `$responseCode` for `$duration` duration retrying $retryTimes times"
            + "$stepsToExecute")
    public void waitForResponseCode(int responseCode, Duration duration, int retryTimes,
                                    SubSteps stepsToExecute)
    {
        new DurationBasedWaiter(new WaitMode(duration, retryTimes)).wait(
                () -> stepsToExecute.execute(Optional.empty()),
                () -> isResponseCodeIsEqualToExpected(httpTestContext.getResponse(), responseCode)
        );
        performIfHttpResponseIsPresent(
                response -> softAssert.assertEquals(HTTP_RESPONSE_STATUS_CODE, response.getStatusCode(), responseCode));
    }

    private boolean isResponseCodeIsEqualToExpected(HttpResponse response, int expectedResponseCode)
    {
        return response != null && response.getStatusCode() == expectedResponseCode;
    }

    private Optional<String> getHeaderValueByName(HttpResponse response, String httpHeaderName)
    {
        return getHeaderByName(response, httpHeaderName).map(Header::getValue);
    }

    private Optional<Header> getHeaderByName(HttpResponse response, String httpHeaderName)
    {
        Optional<Header> header = response.getHeaderByName(httpHeaderName);
        softAssert.assertTrue(httpHeaderName + " header is present", header.isPresent());
        return header;
    }

    private HttpResponse getResponse()
    {
        return httpTestContext.getResponse();
    }

    private byte[] getResponseBody()
    {
        return getResponse().getResponseBody();
    }

    private void performIfHttpResponseIsPresent(Consumer<HttpResponse> responseConsumer)
    {
        HttpResponse response = getResponse();
        if (softAssert.assertNotNull("HTTP response is not null", response))
        {
            responseConsumer.accept(response);
        }
    }
}
