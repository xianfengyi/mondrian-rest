/*
 * Unless explicitly acquired and licensed from Licensor under another license, the contents of
 * this file are subject to the Reciprocal Public License ("RPL") Version 1.5, or subsequent
 * versions as allowed by the RPL, and You may not copy or use this file in either source code
 * or executable form, except in compliance with the terms and conditions of the RPL
 *
 * All software distributed under the RPL is provided strictly on an "AS IS" basis, WITHOUT
 * WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED, AND LICENSOR HEREBY DISCLAIMS ALL SUCH
 * WARRANTIES, INCLUDING WITHOUT LIMITATION, ANY WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE, QUIET ENJOYMENT, OR NON-INFRINGEMENT. See the RPL for specific language
 * governing rights and limitations under the RPL.
 *
 * http://opensource.org/licenses/RPL-1.5
 *
 * Copyright 2012-2017 Open Justice Broker Consortium
 */
package org.ojbc.mondrian.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Test;
import org.ojbc.mondrian.CellSetWrapper;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = { "requestAuthorizerBeanName=bearerTokenRequestAuthorizer" })
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RequestAuthorizerIntegrationTest extends AbstractMondrianRestControllerTest {
	
private final Log log = LogFactory.getLog(RequestAuthorizerIntegrationTest.class);
	
	@LocalServerPort
	private String port;
	
	@Test
	public void testAccess() throws Exception {
		
		Map<String, String> headerMap = new HashMap<>();
		headerMap.put("Authorization", "Bearer TOKEN1");
		
		HttpEntity<String> requestEntity = buildQueryRequestEntity("test", "select {[Measures].[F2_M1]} on columns from Test", headerMap);
		
		ResponseEntity<CellSetWrapper> response = restTemplate.postForEntity(new URI("http://localhost:" + port + "/query"), requestEntity, CellSetWrapper.class);
		assertEquals(200, response.getStatusCode().value());

		headerMap.put("Authorization", "Bearer TOKEN2");
		requestEntity = buildQueryRequestEntity("test", "select {[Measures].[F2_M1]} on columns from Test", headerMap);
		
		response = restTemplate.postForEntity(new URI("http://localhost:" + port + "/query"), requestEntity, CellSetWrapper.class);
		assertEquals(200, response.getStatusCode().value());
		
	}

	@Test
	public void testDeniedAccess() throws Exception {
		
		Map<String, String> headerMap = new HashMap<>();
		headerMap.put("Authorization", "Bearer TOKEN2");
		
		HttpEntity<String> requestEntity = buildQueryRequestEntity("test", "select {[Measures].[F2_M1]} on columns from Test_Secure", headerMap);
		
		ParameterizedTypeReference<Map<String, String>> responseType = new ParameterizedTypeReference<Map<String, String>>() {};
		
		requestEntity = buildQueryRequestEntity("test", "select {[Measures].[F2_M1]} on columns from Test_Secure", headerMap);
		ResponseEntity<Map<String, String>> errorResponse = restTemplate.exchange(new URI("http://localhost:" + port + "/query"), HttpMethod.POST, requestEntity, responseType);
		assertEquals(500, errorResponse.getStatusCode().value());
		
		Map<String, String> errorMap = errorResponse.getBody();
		
		String rootCauseReason = errorMap.get("rootCauseReason");
		assertTrue(rootCauseReason.matches(".+F2_M1.+not found in cube.+Test_Secure.+"));

	}

	@Test
	public void testForbidden() throws Exception {
		
		Map<String, String> headerMap = new HashMap<>();
		headerMap.put("Authorization", "Bearer TOKENDOESNTEXIST");
		
		HttpEntity<String> requestEntity = buildQueryRequestEntity("test", "select {[Measures].[F2_M1]} on columns from Test_Secure", headerMap);
		
		ParameterizedTypeReference<Map<String, String>> responseType = new ParameterizedTypeReference<Map<String, String>>() {};
		
		requestEntity = buildQueryRequestEntity("test", "select {[Measures].[F2_M1]} on columns from Test_Secure", headerMap);
		ResponseEntity<Map<String, String>> errorResponse = restTemplate.exchange(new URI("http://localhost:" + port + "/query"), HttpMethod.POST, requestEntity, responseType);
		assertEquals(403, errorResponse.getStatusCode().value());
		
	}

}
