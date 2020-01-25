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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import java.lang.reflect.Type;

/**
 * @author Tobias Blaufuss
 */
public class ApacheRestApi implements IRestApi {

	private static final Logger LOGGER = LoggerFactory.getLogger(ApacheRestApi.class);

    private final Gson gson = new Gson();
    
    @Override
    public <T> T get(String url, Map<String, String> urlParameters, Class<T> responseType) throws RestApiException {
        try {
            final HttpResponse response = performGet(url, urlParameters);
            final T result = getResponseAsObject(response, responseType);
            return result;
        } catch (URISyntaxException | IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw new RestApiException(e.getMessage(), e);
        }
    }

    @Override
    public <T> List<T> getAsList(String url, Map<String, String> urlParameters, Class<T> responseType) throws RestApiException {
        try {
            final HttpResponse response = performGet(url, urlParameters);
            final List<T> result = getResponseAsList(response, responseType);
            return result;
        } catch (URISyntaxException | IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw new RestApiException(e.getMessage(), e);
        }
    }

    @Override
    public <T, B> T post(String url, Map<String, String> urlParameters, B body, Class<T> responseType) throws RestApiException {
        try {
            final HttpResponse response = performPost(url, urlParameters, body);
            final T result = getResponseAsObject(response, responseType);
            return result;
        } catch (URISyntaxException | IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw new RestApiException(e.getMessage(), e);
        }
    }

    private HttpResponse performGet(String url, Map<String, String> urlParameters) throws URISyntaxException, IOException{
        final HttpClient client = HttpClientBuilder.create().build();
        final URIBuilder uriBuilder = getUriWithParams(url, urlParameters);
        final URI uri = uriBuilder.build();

        final HttpGet httpGet = new HttpGet(uri);
        
        LOGGER.info("Executing GET request: " + uri);
        final HttpResponse response = client.execute(httpGet);
        LOGGER.info("Result of GET request: " + EntityUtils.toString(response.getEntity()));
        return response;
    }

    private <B> HttpResponse performPost(String url, Map<String, String> urlParameters, B body) throws URISyntaxException, IOException{
        final HttpClient client = HttpClientBuilder.create().build();
        final URIBuilder uriBuilder = getUriWithParams(url, urlParameters);
        final URI uri = uriBuilder.build();

        final HttpPost httpPost = new HttpPost(uri);
        final String bodyAsJson = gson.toJson(body);
        final HttpEntity entity = new StringEntity(bodyAsJson);
        httpPost.setEntity(entity);

        LOGGER.info("Executing POST request: " + uri);
        LOGGER.info("Entity: " + bodyAsJson);
        final HttpResponse response = client.execute(httpPost);
        LOGGER.info("Result of POST request: " + EntityUtils.toString(response.getEntity()));
        return response;
    }

    private URIBuilder getUriWithParams(final String url, final Map<String, String> urlParams) throws URISyntaxException {
		final URI uri = new URI(url);
        final URIBuilder uriBuilder = new URIBuilder(uri);
        
        for (Map.Entry<String, String> entry : urlParams.entrySet()) {
            uriBuilder.addParameter(entry.getKey(), entry.getValue());
        }

        return uriBuilder;
	}

	private <T> T getResponseAsObject(final HttpResponse response, final Class<T> clazz) throws IOException, ParseException{
		final HttpEntity entity = response.getEntity();
		if(entity == null) {
			return null;
		}
		final String jsonString = EntityUtils.toString(entity);
		final T result = gson.fromJson(jsonString, clazz);
		return result;
	}

	private <T> List<T> getResponseAsList(final HttpResponse response, final Class<T> clazz) throws IOException, ParseException{
		final HttpEntity entity = response.getEntity();
		if(entity == null) {
			return null;
		}
		final String jsonString = EntityUtils.toString(entity);
		final Type listType = new TypeToken<ArrayList<T>>(){}.getType();
		final List<T> result = gson.fromJson(jsonString, listType);
		return result;
	}
}