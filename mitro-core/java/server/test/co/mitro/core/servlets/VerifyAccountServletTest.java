/*******************************************************************************
 * Copyright (c) 2013 Lectorius, Inc.
 * Authors:
 * Vijay Pandurangan (vijayp@mitro.co)
 * Evan Jones (ej@mitro.co)
 * Adam Hilss (ahilss@mitro.co)
 *
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *     
 *     You can contact the authors at inbound@mitro.co.
 *******************************************************************************/
package co.mitro.core.servlets;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;

import co.mitro.test.MockHttpServletRequest;
import co.mitro.test.MockHttpServletResponse;

import com.google.common.collect.Maps;

public class VerifyAccountServletTest extends MemoryDBFixture {
  private VerifyAccountServlet servlet;

  @Before
  public void setUp() {
    replaceDefaultManagerDbForTest();
    servlet = new VerifyAccountServlet();
  }

  private MockHttpServletResponse makeRequest(Map<String, String> arguments)
      throws ServletException, IOException {
    MockHttpServletRequest httpRequest = new MockHttpServletRequest();
    for (Map.Entry<String, String> argument : arguments.entrySet()) {
      httpRequest.setParameter(argument.getKey(), argument.getValue());
    }

    MockHttpServletResponse httpResponse = new MockHttpServletResponse();
    servlet.doGet(httpRequest, httpResponse);
    return httpResponse;
  }

  private void assertInvalidArguments(Map<String, String> arguments)
      throws ServletException, IOException {
    MockHttpServletResponse httpResponse = makeRequest(arguments);
    assertEquals(HttpServletResponse.SC_BAD_REQUEST, httpResponse.getStatus());
    assertThat(httpResponse.getOutput(), containsString("Invalid arguments"));
  }

  private static HashMap<String, String> makeArguments(String user, String code) {
    HashMap<String, String> args = Maps.newHashMap();
    if (user != null) {
      args.put("user", user);
    }

    if (code != null) {
      args.put("code", code);
    }

    return args;
  }

  @Test
  public void testArguments() throws Exception {
    // valid arguments but user doesn't exist
    HashMap<String, String> args = makeArguments("user", "code");
    MockHttpServletResponse response = makeRequest(args);
    assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
    assertThat(response.getOutput(), containsString("Invalid code"));

    // missing code
    assertInvalidArguments(makeArguments("user", null));
    // empty code
    assertInvalidArguments(makeArguments("user", ""));

    // missing user
    assertInvalidArguments(makeArguments(null, "code"));
    // empty name
    assertInvalidArguments(makeArguments("", "code"));

    // no arguments
    assertInvalidArguments(new HashMap<String, String>());

    // extra arguments
    args.put("extra", "");
    assertInvalidArguments(args);
  }

  @Test
  public void testSuccess() throws Exception {
    assertTrue(testIdentity.isVerified());
    testIdentity.setVerified(false);
    manager.identityDao.update(testIdentity);
    manager.commitTransaction();

    assertFalse(testIdentity.isVerified());
    MockHttpServletResponse response = makeRequest(
        makeArguments(testIdentity.getName(), testIdentity.getVerificationUid()));
    // success is a redirect
    assertEquals(HttpServletResponse.SC_FOUND, response.getStatus());
    assertEquals(VerifyAccountServlet.SUCCESS_DESTINATION, response.getHeader("Location"));

    manager.identityDao.refresh(testIdentity);
    assertTrue(testIdentity.isVerified());
  }
}
