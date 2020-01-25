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

import java.util.List;
import java.util.Map;

/**
 * @author Tobias Blaufuss
 */
public interface IRestApi {
    <T> T get(String url, Map<String, String> urlParameters, Class<T> responseType) throws RestApiException;
    <T> List<T> getAsList(String url, Map<String, String> urlParameters, Class<T> responseType) throws RestApiException;
    <T, B> T post(String url, Map<String, String> urlParameters, B body, Class<T> responseType) throws RestApiException;
}