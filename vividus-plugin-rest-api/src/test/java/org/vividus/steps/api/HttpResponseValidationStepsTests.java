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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.vividus.steps.StringComparisonRule.CONTAINS;
import static org.vividus.steps.StringComparisonRule.DOES_NOT_CONTAIN;
import static org.vividus.steps.StringComparisonRule.IS_EQUAL_TO;
import static org.vividus.steps.StringComparisonRule.MATCHES;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.hamcrest.Matchers;
import org.jbehave.core.model.ExamplesTable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.vividus.context.VariableContext;
import org.vividus.http.ConnectionDetails;
import org.vividus.http.HttpTestContext;
import org.vividus.http.client.HttpResponse;
import org.vividus.model.ArchiveVariable;
import org.vividus.model.NamedEntry;
import org.vividus.model.OutputFormat;
import org.vividus.softassert.ISoftAssert;
import org.vividus.steps.ByteArrayValidationRule;
import org.vividus.steps.ComparisonRule;
import org.vividus.steps.StringComparisonRule;
import org.vividus.steps.SubSteps;
import org.vividus.util.ResourceUtils;
import org.vividus.util.json.JsonUtils;
import org.vividus.variable.VariableScope;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("checkstyle:MethodCount")
class HttpResponseValidationStepsTests
{
    private static final String SET_COOKIES_HEADER_NAME = "Set-Cookies";
    private static final String HEADER_IS_PRESENT = " header is present";
    private static final String RESPONSE_BODY = "testResponse";
    private static final String HTTP_RESPONSE_BODY = "HTTP response body";
    private static final String VARIABLE_NAME = "variableName";
    private static final String NUMBER_RESPONSE_HEADERS_WITH_NAME =
            "The number of the response headers with the name '%s'";
    private static final String HTTP_RESPONSE_IS_NOT_NULL = "HTTP response is not null";
    private static final String TLS_V1_2 = "TLSv1.2";
    private static final String CONNECTION_SECURE_ASSERTION = "Connection is secure";
    private static final String FILE_JSON = "file.json";
    private static final String IMAGE_PNG = "images/image.png";
    private static final String HTTP_RESPONSE_STATUS_CODE = "HTTP response status code";
    private static final String DUMMY = "dummy";
    private static final int RETRY_TIMES = 3;
    private static final int RESPONSE_CODE = 200;
    private static final int RESPONSE_CODE_ERROR = 404;
    private static final Duration DURATION = Duration.ofSeconds(10);

    @Mock
    private HttpTestContext httpTestContext;

    @Mock
    private ISoftAssert softAssert;

    @Mock
    private VariableContext variableContext;

    @Spy
    private final JsonUtils jsonUtils = new JsonUtils();

    @InjectMocks
    private HttpResponseValidationSteps httpResponseValidationSteps;

    private final HttpResponse httpResponse = new HttpResponse();

    @Test
    void testGetHeaderAttributes()
    {
        mockHttpResponse();
        Header header = mock(Header.class);
        when(header.getName()).thenReturn(SET_COOKIES_HEADER_NAME);
        httpResponse.setResponseHeaders(header);
        HeaderElement headerElement = mock(HeaderElement.class);
        when(header.getElements()).thenReturn(new HeaderElement[] { headerElement });
        when(softAssert.assertTrue(SET_COOKIES_HEADER_NAME + HEADER_IS_PRESENT, true)).thenReturn(true);
        String headerAttributeName = "HTTPOnly";
        when(headerElement.getName()).thenReturn(headerAttributeName);
        ExamplesTable attribute = new ExamplesTable("|attribute|\n|HTTPOnly|/|");
        httpResponseValidationSteps.assertHeaderContainsAttributes(SET_COOKIES_HEADER_NAME, attribute);
        verify(softAssert).assertThat(
                eq(SET_COOKIES_HEADER_NAME + " header contains " + headerAttributeName + " attribute"),
                eq(Collections.singletonList(headerAttributeName)),
                argThat(matcher -> matcher.toString().equals(Matchers.contains(headerAttributeName).toString())));
    }

    @Test
    void testGetHeaderAttributesHeaderNotFound()
    {
        mockHttpResponse();
        Header header = mock(Header.class);
        when(header.getName()).thenReturn("Vary");
        httpResponse.setResponseHeaders(header);
        ExamplesTable attribute = new ExamplesTable("|attribute|\n|Secure|/|");
        httpResponseValidationSteps.assertHeaderContainsAttributes(SET_COOKIES_HEADER_NAME, attribute);
        verify(softAssert).assertTrue(SET_COOKIES_HEADER_NAME + HEADER_IS_PRESENT, false);
    }

    @Test
    void testGetHeaderAttributesNoHttpResponse()
    {
        httpResponseValidationSteps.assertHeaderContainsAttributes(SET_COOKIES_HEADER_NAME, ExamplesTable.empty());
        verifyNoHttpResponse();
    }

    @Test
    void testThenTheResponseTimeShouldBeLessThan()
    {
        mockHttpResponse();
        long responseTime = 1000L;
        httpResponse.setResponseTimeInMs(responseTime);
        httpResponseValidationSteps.thenTheResponseTimeShouldBeLessThan(responseTime);
        verify(softAssert).assertThat(eq("The response time is less than response time threshold."),
                eq(responseTime), argThat(matcher -> lessThan(responseTime).toString().equals(matcher.toString())));
    }

    @Test
    void testThenTheResponseTimeShouldBeLessThanNoHttpResponse()
    {
        httpResponseValidationSteps.thenTheResponseTimeShouldBeLessThan(1000L);
        verifyNoHttpResponse();
    }

    @Test
    void testDecompressedResponseBodySizeNoHttpResponse()
    {
        httpResponseValidationSteps.doesDecompressedResponseBodySizeConfirmRule(ComparisonRule.LESS_THAN, 10);
        verifyNoHttpResponse();
    }

    @Test
    void testDecompressedResponseBodySizeEqualTo()
    {
        when(httpTestContext.getResponse()).thenReturn(httpResponse);
        String body = RESPONSE_BODY;
        when(softAssert.assertNotNull(HTTP_RESPONSE_IS_NOT_NULL, httpResponse)).thenReturn(true);
        httpResponse.setResponseBody(body.getBytes(StandardCharsets.UTF_8));
        httpResponseValidationSteps.doesDecompressedResponseBodySizeConfirmRule(ComparisonRule.EQUAL_TO, 10);
        verify(softAssert).assertThat(eq("Size of decompressed HTTP response body"),
                eq(body.getBytes(StandardCharsets.UTF_8).length),
                argThat(m -> "a value equal to <10>".equals(m.toString())));
    }

    @Test
    void testThenTheResponseCodeShouldBeEqualTo()
    {
        mockHttpResponse();
        int validCode = 200;
        httpResponse.setStatusCode(validCode);
        httpResponseValidationSteps.assertResponseCode(ComparisonRule.EQUAL_TO, validCode);
        verify(softAssert).assertThat(eq(HTTP_RESPONSE_STATUS_CODE), eq(validCode),
                argThat(matcher -> matcher.toString().equals("a value equal to <" + validCode + ">")));
    }

    @Test
    void testThenTheResponseCodeShouldBeEqualToNoHttpResponse()
    {
        httpResponseValidationSteps.assertResponseCode(ComparisonRule.EQUAL_TO, 200);
        verifyNoHttpResponse();
    }

    @Test
    void testDoesResponseBodyEqualToContent()
    {
        mockHttpResponse();
        String body = RESPONSE_BODY;
        httpResponse.setResponseBody(body.getBytes(StandardCharsets.UTF_8));
        httpResponseValidationSteps.doesResponseBodyMatch(IS_EQUAL_TO, body);
        verify(softAssert).assertThat(eq(HTTP_RESPONSE_BODY), eq(body),
                argThat(arg -> arg.toString().equals("\"testResponse\"")));
    }

    @Test
    void testDoesResponseBodyEqualToContentNoHttpResponse()
    {
        httpResponseValidationSteps.doesResponseBodyMatch(IS_EQUAL_TO, StringUtils.EMPTY);
        verifyNoHttpResponse();
    }

    @Test
    void testDoesResponseBodyMatchResource()
    {
        mockHttpResponse();
        when(softAssert.assertEquals("Arrays size", 6, 6)).thenReturn(true);
        httpResponse.setResponseBody(new byte[] { 123, 98, 111, 100, 121, 125 });
        httpResponseValidationSteps.doesResponseBodyMatchResource(ByteArrayValidationRule.IS_EQUAL_TO,
                "/requestBody.txt");
        verify(softAssert).recordPassedAssertion("Expected and actual arrays are equal");
    }

    @Test
    void testDoesResponseBodyMatchResourceNoHttpResponse()
    {
        httpResponseValidationSteps.doesResponseBodyMatchResource(ByteArrayValidationRule.IS_EQUAL_TO, "body.txt");
        verifyNoHttpResponse();
    }

    @Test
    void testContentTypeOfResponseBody()
    {
        mockHttpResponse();
        httpResponse.setResponseBody(ResourceUtils.loadResourceAsByteArray(getClass(), "swf.swf"));
        testContentTypeOfResponseBody("application/x-shockwave-flash");
    }

    @ParameterizedTest
    @CsvSource({
            "{},             application/json",
            "[],             application/json",
            "' {\"key\":1}', application/json",
            "{',             text/plain",
            "1',             text/plain"
    })
    void testContentTypeOfResponseBodyWithText(String responseBody, String expectedContentType)
    {
        mockHttpResponse();
        httpResponse.setResponseBody(responseBody.getBytes(StandardCharsets.UTF_8));
        testContentTypeOfResponseBody(expectedContentType);
    }

    @Test
    void testContentTypeOfResponseBodyNoHttpResponse()
    {
        httpResponseValidationSteps.assertContentTypeOfResponseBody(IS_EQUAL_TO, "text/plain");
        verifyNoHttpResponse();
    }

    private void testContentTypeOfResponseBody(String contentType)
    {
        httpResponseValidationSteps.assertContentTypeOfResponseBody(IS_EQUAL_TO, contentType);
        verify(softAssert).assertThat(eq("Content type of response body"), eq(contentType),
                argThat(matcher -> matcher.toString().contains(contentType)));
    }

    @Test
    void testSaveHeaderValue()
    {
        mockHttpResponse();
        String headerValue = mockHeaderRetrieval();
        Set<VariableScope> scopes = Set.of(VariableScope.SCENARIO);
        httpResponseValidationSteps.saveHeaderValue(SET_COOKIES_HEADER_NAME, scopes, VARIABLE_NAME);
        verify(variableContext).putVariable(scopes, VARIABLE_NAME, headerValue);
    }

    @Test
    void testSaveHeaderValueNoHttpResponse()
    {
        httpResponseValidationSteps.saveHeaderValue(SET_COOKIES_HEADER_NAME, Set.of(VariableScope.SCENARIO),
                VARIABLE_NAME);
        verifyNoHttpResponse();
    }

    @Test
    void testDoesHeaderEqualToValue()
    {
        mockHttpResponse();
        String headerValue = mockHeaderRetrieval();
        httpResponseValidationSteps.doesHeaderMatch(SET_COOKIES_HEADER_NAME, IS_EQUAL_TO,
                headerValue);
        verify(softAssert).assertThat(eq("'" + SET_COOKIES_HEADER_NAME + "' header value"), eq(headerValue),
                argThat(matcher -> matcher.toString().equals(equalTo(headerValue).toString())));
    }

    @Test
    void testDoesHeaderEqualToValueNoHttpResponse()
    {
        httpResponseValidationSteps.doesHeaderMatch(SET_COOKIES_HEADER_NAME, IS_EQUAL_TO, "value");
        verifyNoHttpResponse();
    }

    @Test
    void testSaveHeaderValueHeaderNotFound()
    {
        mockHttpResponse();
        httpResponse.setResponseHeaders();
        httpResponseValidationSteps.saveHeaderValue(SET_COOKIES_HEADER_NAME, Set.of(VariableScope.SCENARIO),
                VARIABLE_NAME);
        verify(softAssert).assertTrue(SET_COOKIES_HEADER_NAME + HEADER_IS_PRESENT, false);
        verifyNoInteractions(variableContext);
    }

    @Test
    void testDoesResponseNotContainBody()
    {
        mockHttpResponse();
        httpResponse.setResponseBody(null);
        httpResponseValidationSteps.doesResponseNotContainBody();
        verify(softAssert).assertNull("The response does not contain body", null);
    }

    @Test
    void testDoesResponseNotContainBodyNoHttpResponse()
    {
        httpResponseValidationSteps.doesResponseNotContainBody();
        verifyNoHttpResponse();
    }

    @Test
    void testSaveResponseBody()
    {
        mockHttpResponse();
        httpResponse.setResponseBody(RESPONSE_BODY.getBytes(StandardCharsets.UTF_8));
        Set<VariableScope> scopes = Set.of(VariableScope.SCENARIO);
        httpResponseValidationSteps.saveResponseBody(scopes, VARIABLE_NAME);
        verify(variableContext).putVariable(scopes, VARIABLE_NAME, RESPONSE_BODY);
    }

    @Test
    void testSaveResponseBodyNoHttpResponse()
    {
        httpResponseValidationSteps.saveResponseBody(Set.of(VariableScope.SCENARIO), VARIABLE_NAME);
        verifyNoHttpResponse();
    }

    @Test
    void testResponseNotContainsHeadersWithName()
    {
        mockHttpResponse();
        httpResponse.setResponseHeaders();
        httpResponseValidationSteps.isHeaderWithNameFound(SET_COOKIES_HEADER_NAME, ComparisonRule.EQUAL_TO, 1);
        verify(softAssert).assertThat(eq(String.format(NUMBER_RESPONSE_HEADERS_WITH_NAME,
                SET_COOKIES_HEADER_NAME)), eq(0),
                argThat(m -> ComparisonRule.EQUAL_TO.getComparisonRule(1).toString().equals(m.toString())));
    }

    @Test
    void testResponseContainsHeadersWithName()
    {
        mockHttpResponse();
        Header header = mock(Header.class);
        when(header.getName()).thenReturn(SET_COOKIES_HEADER_NAME);
        httpResponse.setResponseHeaders(header, header);
        httpResponseValidationSteps.isHeaderWithNameFound(SET_COOKIES_HEADER_NAME, ComparisonRule.EQUAL_TO, 2);
        verify(softAssert).assertThat(eq(String.format(NUMBER_RESPONSE_HEADERS_WITH_NAME,
                SET_COOKIES_HEADER_NAME)), eq(2),
                argThat(m -> ComparisonRule.EQUAL_TO.getComparisonRule(2).toString().equals(m.toString())));
    }

    @Test
    void testResponseContainsHeadersWithNameNoHttpResponse()
    {
        httpResponseValidationSteps.isHeaderWithNameFound(SET_COOKIES_HEADER_NAME, ComparisonRule.EQUAL_TO, 1);
        verifyNoHttpResponse();
    }

    @Test
    void shouldValidateSecuredConnection()
    {
        boolean secure = true;
        String actualProtocol = "TLSv1.3";
        ConnectionDetails connectionDetails = new ConnectionDetails();
        connectionDetails.setSecure(secure);
        connectionDetails.setSecurityProtocol(actualProtocol);

        when(httpTestContext.getConnectionDetails()).thenReturn(connectionDetails);
        when(softAssert.assertTrue(CONNECTION_SECURE_ASSERTION, secure)).thenReturn(Boolean.TRUE);
        httpResponseValidationSteps.isConnectionSecured(TLS_V1_2);
        verify(softAssert).assertEquals("Security protocol", TLS_V1_2, actualProtocol);
    }

    @Test
    void shouldValidateNonSecuredConnection()
    {
        boolean secure = false;
        ConnectionDetails connectionDetails = new ConnectionDetails();
        connectionDetails.setSecure(secure);

        when(httpTestContext.getConnectionDetails()).thenReturn(connectionDetails);
        when(softAssert.assertTrue(CONNECTION_SECURE_ASSERTION, secure)).thenReturn(Boolean.FALSE);
        httpResponseValidationSteps.isConnectionSecured(TLS_V1_2);
        verifyNoMoreInteractions(softAssert);
    }

    @Test
    void testSaveFilesContentToVariables()
    {
        mockHttpResponseWithArchive();

        String json = "json";
        String image = "image";
        String base64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABAQMAAAAl21bKAAAABlBMVEUAAAD///+l2Z/dAAAACXBIWXMAAA7EAAAOxAGVK"
                + "w4bAAAACklEQVQImWNgAAAAAgAB9HFkpgAAAABJRU5ErkJggg==";

        httpResponseValidationSteps.saveFilesContentToVariables(
                List.of(createVariable(IMAGE_PNG, image, OutputFormat.BASE64),
                        createVariable(FILE_JSON, json, OutputFormat.TEXT)));
        Set<VariableScope> scopes = Set.of(VariableScope.SCENARIO);
        verify(variableContext).putVariable(scopes, image, base64);
        verify(variableContext).putVariable(scopes, json,
                "{\"plugin\": \"vividus-plugin-rest-api\"}\n");
        verifyNoInteractions(softAssert);
        verifyNoMoreInteractions(variableContext);
    }

    @Test
    void testSaveFilesContentToVariablesInvalidPath()
    {
        mockHttpResponseWithArchive();

        String path = "path";
        httpResponseValidationSteps.saveFilesContentToVariables(
                List.of(createVariable(path, path, OutputFormat.BASE64)));
        verify(softAssert).recordFailedAssertion(
                String.format("Unable to find entry by name %s in response archive", path));
        verifyNoInteractions(variableContext);
        verifyNoMoreInteractions(softAssert);
    }

    @Test
    void testVerifyArchiveContainsEntries()
    {
        mockHttpResponseWithArchive();
        Set<String> archiveEntries = Set.of(IMAGE_PNG, FILE_JSON);
        String message = "The response archive contains entry with name ";
        httpResponseValidationSteps.verifyArchiveContainsEntries(List.of(
                createEntry(FILE_JSON, null),
                createEntry(IMAGE_PNG, null),
                createEntry(DUMMY, null)
        ));
        verify(softAssert).assertThat(eq(message + FILE_JSON), eq(archiveEntries),
                argThat(e -> e.matches(archiveEntries)));
        verify(softAssert).assertThat(eq(message + IMAGE_PNG), eq(archiveEntries),
                argThat(e -> e.matches(archiveEntries)));
        verify(softAssert).assertThat(eq(message + DUMMY), eq(archiveEntries),
                argThat(e -> !e.matches(archiveEntries)));
        verifyNoMoreInteractions(softAssert);
    }

    @Test
    void testVerifyArchiveContainsEntriesWithUserRules()
    {
        String matchesPattern = ".+\\.png";
        String containsPattern = "file";

        mockHttpResponseWithArchive();
        Set<String> archiveEntries = Set.of(IMAGE_PNG, FILE_JSON);
        String message = "The response archive contains entry matching the comparison rule '%s' with name pattern '%s'";
        httpResponseValidationSteps.verifyArchiveContainsEntries(List.of(
                createEntry(matchesPattern, MATCHES),
                createEntry(containsPattern, CONTAINS),
                createEntry(DUMMY, IS_EQUAL_TO),
                createEntry(DUMMY, DOES_NOT_CONTAIN)
        ));
        verify(softAssert).assertThat(eq(String.format(message, MATCHES, matchesPattern)), eq(archiveEntries),
                argThat(e -> e.matches(archiveEntries)));
        verify(softAssert).assertThat(eq(String.format(message, CONTAINS, containsPattern)), eq(archiveEntries),
                argThat(e -> e.matches(archiveEntries)));
        verify(softAssert).assertThat(eq(String.format(message, IS_EQUAL_TO, DUMMY)), eq(archiveEntries),
                argThat(e -> !e.matches(archiveEntries)));
        verify(softAssert).assertThat(eq(String.format(message, DOES_NOT_CONTAIN, DUMMY)), eq(archiveEntries),
                argThat(e -> e.matches(archiveEntries)));
        verifyNoMoreInteractions(softAssert);
    }

    @Test
    void testWaitForResponseCode()
    {
        mockHttpResponse();
        SubSteps stepsToExecute = mock(SubSteps.class);
        httpResponseValidationSteps.waitForResponseCode(RESPONSE_CODE, DURATION, RETRY_TIMES, stepsToExecute);
        verify(stepsToExecute, atLeast(2)).execute(Optional.empty());
    }

    @Test
    void testWaitForResponseCodeWhenResponseCodeIsEqualToExpected()
    {
        SubSteps stepsToExecute = mock(SubSteps.class);
        HttpResponse httpResponse = mock(HttpResponse.class);
        when(softAssert.assertNotNull(HTTP_RESPONSE_IS_NOT_NULL, httpResponse)).thenReturn(true);
        when(httpResponse.getStatusCode()).thenReturn(RESPONSE_CODE_ERROR, RESPONSE_CODE);
        when(httpTestContext.getResponse()).thenReturn(httpResponse);
        httpResponseValidationSteps.waitForResponseCode(RESPONSE_CODE, DURATION, RETRY_TIMES, stepsToExecute);
        verify(stepsToExecute, times(2)).execute(Optional.empty());
        verify(softAssert).assertEquals(HTTP_RESPONSE_STATUS_CODE, RESPONSE_CODE, RESPONSE_CODE);
    }

    @Test
    void testWaitForResponseCodeWhenResponseCodeIsNotEqualToExpected()
    {
        SubSteps stepsToExecute = mock(SubSteps.class);
        HttpResponse httpResponse = mock(HttpResponse.class);
        when(softAssert.assertNotNull(HTTP_RESPONSE_IS_NOT_NULL, httpResponse)).thenReturn(true);
        when(httpResponse.getStatusCode()).thenReturn(RESPONSE_CODE_ERROR, RESPONSE_CODE_ERROR, RESPONSE_CODE_ERROR);
        when(httpTestContext.getResponse()).thenReturn(httpResponse);
        httpResponseValidationSteps.waitForResponseCode(RESPONSE_CODE, DURATION, RETRY_TIMES, stepsToExecute);
        verify(stepsToExecute, times(3)).execute(Optional.empty());
        verify(softAssert).assertEquals(HTTP_RESPONSE_STATUS_CODE, RESPONSE_CODE_ERROR, RESPONSE_CODE);
    }

    private static NamedEntry createEntry(String name, StringComparisonRule rule)
    {
        NamedEntry entry = new NamedEntry();
        entry.setName(name);
        entry.setRule(rule);
        return entry;
    }

    private void mockHttpResponseWithArchive()
    {
        byte[] data = ResourceUtils.loadResourceAsByteArray(getClass(), "/org/vividus/steps/api/archive.zip");
        HttpResponse response = mock(HttpResponse.class);
        when(httpTestContext.getResponse()).thenReturn(response);
        when(response.getResponseBody()).thenReturn(data);
    }

    private static ArchiveVariable createVariable(String path, String variableName, OutputFormat outputFormat)
    {
        Set<VariableScope> scopes = Set.of(VariableScope.SCENARIO);
        ArchiveVariable variable = new ArchiveVariable();
        variable.setPath(path);
        variable.setOutputFormat(outputFormat);
        variable.setScopes(scopes);
        variable.setVariableName(variableName);
        return variable;
    }

    private String mockHeaderRetrieval()
    {
        String headerValue = "headerValue";
        Header header = mock(Header.class);
        when(header.getName()).thenReturn(SET_COOKIES_HEADER_NAME);
        httpResponse.setResponseHeaders(header);
        when(header.getValue()).thenReturn(headerValue);
        when(softAssert.assertTrue(SET_COOKIES_HEADER_NAME + HEADER_IS_PRESENT, true)).thenReturn(true);
        return headerValue;
    }

    private void mockHttpResponse()
    {
        when(httpTestContext.getResponse()).thenReturn(httpResponse);
        when(softAssert.assertNotNull(HTTP_RESPONSE_IS_NOT_NULL, httpResponse)).thenReturn(true);
    }

    private void verifyNoHttpResponse()
    {
        verify(softAssert).assertNotNull(HTTP_RESPONSE_IS_NOT_NULL, null);
        verifyNoMoreInteractions(softAssert);
    }
}
