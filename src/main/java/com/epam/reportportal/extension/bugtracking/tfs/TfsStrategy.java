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
import com.epam.ta.reportportal.ws.model.ErrorType;
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
 * @author Tobias Blaufuss
 */
@Extension
@Component
public class TfsStrategy implements ReportPortalExtensionPoint, BtsExtension {

	private static final Logger LOGGER = LoggerFactory.getLogger(TfsStrategy.class);

	private final Gson gson = new Gson();
	private final IRestApi api = new SpringRestApi();
	private static final String EXTERNAL_API_URI = "https://myurl.com";

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

	@Override
	public boolean testConnection(final Integration integration) {
		try {
			String url = getUrl("/api/welcome");
			Map<String, String> urlParameters = getUrlParameters(integration);
			final Boolean result = api.get(url , urlParameters, Boolean.class);
			if(result == null) {
				throw new ReportPortalException(UNABLE_INTERACT_WITH_INTEGRATION, "Check of TFS server returned null.");
			}
			return result;
		} catch (RestApiException e) {
			e.printStackTrace();
			LOGGER.error(e.getMessage(), e);
			throw new ReportPortalException(UNABLE_INTERACT_WITH_INTEGRATION, e.getMessage());
		}
	}

	@Override
	public Optional<Ticket> getTicket(final String id, final Integration integration) {
		try {
			final String url = getUrl("/api/ticket/" + id);
			final Map<String, String> urlParameters = getUrlParameters(integration);
			final Ticket result = api.get(url, urlParameters, Ticket.class);
			if(result == null) {
				throw new ReportPortalException(UNABLE_INTERACT_WITH_INTEGRATION, "GetTicket from TFS server returned null.");
			}
			return Optional.of(result);
		} catch (RestApiException e) {
			e.printStackTrace();
			LOGGER.error(e.getMessage(), e);
			throw new ReportPortalException(UNABLE_INTERACT_WITH_INTEGRATION, e.getMessage());
		}
	}

	@Override
	public Ticket submitTicket(final PostTicketRQ ticketRQ, final Integration integration) {
		try {
			final String url = getUrl("/api/ticket");
			final Map<String, String> urlParameters = getUrlParameters(integration);
			final Ticket result = api.post(url, urlParameters, ticketRQ, Ticket.class);
			if(result == null) {
				throw new ReportPortalException(UNABLE_INTERACT_WITH_INTEGRATION, "SubmitTicket from TFS server returned null.");
			}
			return result;
		} catch (RestApiException e) {
			e.printStackTrace();
			LOGGER.error(e.getMessage(), e);
			throw new ReportPortalException(UNABLE_INTERACT_WITH_INTEGRATION, e.getMessage());
		}
	}

	@Override
	public List<PostFormField> getTicketFields(final String ticketType, final Integration details) {
		try {
			final String url = getUrl("/api/ticketfields");
			final Map<String, String> urlParameters = getUrlParameters(details);
			urlParameters.put("type", ticketType);
			final List<PostFormField> result = api.getAsList(url, urlParameters, PostFormField.class);
			if(result == null) {
				throw new ReportPortalException(UNABLE_INTERACT_WITH_INTEGRATION, "GetTicketFields from TFS server returned null.");
			}
			return result;
		} catch (RestApiException e) {
			e.printStackTrace();
			LOGGER.error(e.getMessage(), e);
			throw new ReportPortalException(UNABLE_INTERACT_WITH_INTEGRATION, e.getMessage());
		}
	}

	@Override
	public List<String> getIssueTypes(final Integration integration) {
		try {
			final String url = getUrl("/api/issuetypes");
			final Map<String, String> urlParameters = getUrlParameters(integration);
			final List<String> result = api.getAsList(url, urlParameters, String.class);
			if(result == null) {
				throw new ReportPortalException(UNABLE_INTERACT_WITH_INTEGRATION, "GetIssueTypes from TFS server returned null.");
			}
			return result;
		} catch (RestApiException e) {
			e.printStackTrace();
			LOGGER.error(e.getMessage(), e);
			throw new ReportPortalException(UNABLE_INTERACT_WITH_INTEGRATION, e.getMessage());
		}
	}

	private String getUrl(String relativePath) {
		return EXTERNAL_API_URI + relativePath;
	}

	private Map<String, String> getUrlParameters(Integration integration) {
		Map<String, String> urlParameters = new HashMap<>();
		final String url = BtsConstants.URL.getParam(integration.getParams(), String.class)
		.orElseThrow(() -> new ReportPortalException(UNABLE_INTERACT_WITH_INTEGRATION, "Tfs Project value cannot be NULL"));
		urlParameters.put("uri", url);

		final String project = BtsConstants.PROJECT.getParam(integration.getParams(), String.class)
				.orElseThrow(() -> new ReportPortalException(UNABLE_INTERACT_WITH_INTEGRATION, "Tfs Project value cannot be NULL"));
		urlParameters.put("project", project);
		return urlParameters;
	}
}