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

import com.epam.reportportal.extension.IntegrationGroupEnum;
import com.epam.reportportal.extension.PluginCommand;
import com.epam.reportportal.extension.ReportPortalExtensionPoint;
import com.epam.reportportal.extension.bugtracking.BtsConstants;
import com.epam.reportportal.extension.bugtracking.BtsExtension;
import com.epam.reportportal.extension.bugtracking.InternalTicketAssembler;
import com.epam.ta.reportportal.binary.impl.AttachmentDataStoreService;
import com.epam.ta.reportportal.dao.LogRepository;
import com.epam.ta.reportportal.dao.TestItemRepository;
import com.epam.ta.reportportal.entity.integration.Integration;
import com.epam.ta.reportportal.exception.ReportPortalException;
import com.epam.ta.reportportal.filesystem.DataEncoder;
import com.epam.ta.reportportal.ws.model.externalsystem.PostFormField;
import com.epam.ta.reportportal.ws.model.externalsystem.PostTicketRQ;
import com.epam.ta.reportportal.ws.model.externalsystem.Ticket;
import com.google.common.base.Suppliers;
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
import org.pf4j.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Supplier;

import static com.epam.ta.reportportal.ws.model.ErrorType.UNABLE_INTERACT_WITH_INTEGRATION;
import java.lang.reflect.Type;
/**
 * @author Dzmitry_Kavalets
 */
@Extension
@Component
public class TfsStrategy implements ReportPortalExtensionPoint, BtsExtension {

	private static final Logger LOGGER = LoggerFactory.getLogger(TfsStrategy.class);

	private final Gson gson = new Gson();
	private static final String EXTERNAL_API_URI = "https://myurl.com";

	@Autowired
	private AttachmentDataStoreService attachmentDataStoreService;

	@Autowired
	private TestItemRepository testItemRepository;

	@Autowired
	private LogRepository logRepository;

	@Autowired
	private DataEncoder dataEncoder;

	@Override
	public Map<String, ?> getPluginParams() {
		return Collections.emptyMap();
	}

	@Override
	public PluginCommand getCommandToExecute(final String commandName) {
		return null;
	}

	@Override
	public IntegrationGroupEnum getIntegrationGroup() {
		return IntegrationGroupEnum.BTS;
	}

	private final Supplier<InternalTicketAssembler> ticketAssembler = Suppliers.memoize(() -> new InternalTicketAssembler(logRepository,
			testItemRepository,
			attachmentDataStoreService,
			dataEncoder
	));

	@Override
	public boolean testConnection(final Integration integration) {
		final HttpClient client = HttpClientBuilder.create().build();
		try {
			final URIBuilder uriBuilder = getUriWithParams(integration, "/api/welcome");
			final URI uri = uriBuilder.build();
			final HttpResponse response = client.execute(new HttpGet(uri));
			final Boolean result = getResponseAsObject(response, Boolean.class);
			if(result != null) {
				return result;
			}
		} catch (URISyntaxException | IOException e) {
			e.printStackTrace();
			LOGGER.error(e.getMessage(), e);
		}
		return false;
	}

	@Override
	public Optional<Ticket> getTicket(final String id, final Integration integration) {
		final HttpClient client = HttpClientBuilder.create().build();
		try {
			final URIBuilder uriBuilder = getUriWithParams(integration, "/api/ticket/" + id);
			final URI uri = uriBuilder.build();
			final HttpResponse response = client.execute(new HttpGet(uri));
			final Ticket result = getResponseAsObject(response, Ticket.class);
			if(result != null) {
				return Optional.of(result);
			}
		} catch (URISyntaxException | IOException e) {
			e.printStackTrace();
			LOGGER.error(e.getMessage(), e);
		}
		return Optional.empty();
	}

	@Override
	public Ticket submitTicket(final PostTicketRQ ticketRQ, final Integration integration) {
		final HttpClient client = HttpClientBuilder.create().build();
		try {
			final URIBuilder uriBuilder = getUriWithParams(integration, "/api/ticket/");
			final URI uri = uriBuilder.build();

			final HttpPost httpPost = new HttpPost(uri);
			final HttpEntity entity = new StringEntity(gson.toJson(ticketRQ));
			httpPost.setEntity(entity);
			final HttpResponse response = client.execute(httpPost);
			final Ticket result = getResponseAsObject(response, Ticket.class);
			if(result != null) {
				return result;
			}
		} catch (URISyntaxException | IOException e) {
			e.printStackTrace();
			LOGGER.error(e.getMessage(), e);
		}
		return null;
	}

	@Override
	public List<PostFormField> getTicketFields(final String ticketType, final Integration details) {
		final HttpClient client = HttpClientBuilder.create().build();
		try {
			final URIBuilder uriBuilder = getUriWithParams(details, "/api/ticketfields/");
			uriBuilder.addParameter("type", ticketType);
			final URI uri = uriBuilder.build();
			final HttpResponse response = client.execute(new HttpGet(uri));
			
			final List<PostFormField> result = getResponseAsList(response, PostFormField.class);
			if(result != null) {
				return result;
			}
		} catch (URISyntaxException | IOException e) {
			e.printStackTrace();
			LOGGER.error(e.getMessage(), e);
		}
		return null;
	}

	@Override
	public List<String> getIssueTypes(final Integration integration) {
		final HttpClient client = HttpClientBuilder.create().build();
		try {
			final URIBuilder uriBuilder = getUriWithParams(integration, "/api/issuetypes");
			final URI uri = uriBuilder.build();
			final HttpResponse response = client.execute(new HttpGet(uri));

			final List<String> result = getResponseAsList(response, String.class);
			if(result != null) {
				return result;
			}
		} catch (URISyntaxException | IOException e) {
			e.printStackTrace();
			LOGGER.error(e.getMessage(), e);
		}
		return null;
	}

	private URIBuilder getUriWithParams(final Integration integration, final String relativePath) throws URISyntaxException {
		final URI uri = new URI(EXTERNAL_API_URI + relativePath);
		final URIBuilder uriBuilder = new URIBuilder(uri);
		
		final String url = BtsConstants.URL.getParam(integration.getParams(), String.class)
		.orElseThrow(() -> new ReportPortalException(UNABLE_INTERACT_WITH_INTEGRATION, "Tfs Project value cannot be NULL"));
		uriBuilder.addParameter("uri", url);

		final String project = BtsConstants.PROJECT.getParam(integration.getParams(), String.class)
				.orElseThrow(() -> new ReportPortalException(UNABLE_INTERACT_WITH_INTEGRATION, "Tfs Project value cannot be NULL"));
		uriBuilder.addParameter("project", project);

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