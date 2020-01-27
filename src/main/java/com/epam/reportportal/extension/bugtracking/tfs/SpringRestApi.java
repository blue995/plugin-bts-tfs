/*
 * Copyright 2018 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.reportportal.extension.bugtracking.tfs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;


/**
 * @author Tobias Blaufuss
 */
public class SpringRestApi implements IRestApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringRestApi.class);
    
    @Override
    public <T> T get(String url, Map<String, String> urlParameters, Class<T> responseType) throws RestApiException {
        final String uri = getUriBuilder(url, urlParameters).toUriString();
        final RestTemplate template = new RestTemplate();
        try {
            final T result = template.getForObject(uri, responseType);
            return result;
        } catch (RestClientException e) {
            final StringBuilder builder = new StringBuilder();
            builder.append(e.getMessage());
            builder.append(", URI:");
            builder.append(uri);
            final String errorMessage = builder.toString();
            LOGGER.error(errorMessage, e);
            throw new RestApiException(errorMessage, e);
        }
    }

    @Override
    public <T> List<T> getAsList(String url, Map<String, String> urlParameters, Class<T> responseType) throws RestApiException {
        final String uri = getUriBuilder(url, urlParameters).toUriString();
        final RestTemplate template = new RestTemplate();
        final HttpMethod httpMethod = HttpMethod.GET;
        try {
            final ResponseEntity<List<T>> response = template.exchange(uri, httpMethod, null, new ParameterizedTypeReference<List<T>>(){});
            final List<T> result = response.getBody();
            return result;
        } catch (RestClientException e) {
            final StringBuilder builder = new StringBuilder();
            builder.append(e.getMessage());
            builder.append(", URI:");
            builder.append(uri);
            builder.append(", Method:");
            builder.append(httpMethod);
            final String errorMessage = builder.toString();
            LOGGER.error(errorMessage, e);
            throw new RestApiException(errorMessage, e);
        }
    }

    @Override
    public <T, B> T post(String url, Map<String, String> urlParameters, B body, Class<T> responseType) throws RestApiException {
        final String uri = getUriBuilder(url, urlParameters).toUriString();
        final RestTemplate template = new RestTemplate();
        try {
            final T result = template.postForObject(uri, body, responseType);
            return result;
        } catch (RestClientException e) {
            final StringBuilder builder = new StringBuilder();
            builder.append(e.getMessage());
            builder.append(", URI:");
            builder.append(uri);
            builder.append(", Body:");
            builder.append(body);
            final String errorMessage = builder.toString();
            LOGGER.error(errorMessage, e);
            throw new RestApiException(errorMessage, e);
        }
    }

    private static UriComponentsBuilder getUriBuilder(String url, Map<String, String> urlParams){
        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(url);
        for (Map.Entry<String, String> entry : urlParams.entrySet()) {
            uriComponentsBuilder.queryParam(entry.getKey(), entry.getValue());
        }
        return uriComponentsBuilder;
    }
}