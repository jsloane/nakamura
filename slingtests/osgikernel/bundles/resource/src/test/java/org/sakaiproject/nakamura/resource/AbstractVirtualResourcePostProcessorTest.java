/*
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.sakaiproject.nakamura.resource;

import static org.junit.Assert.assertNotNull;
import static org.easymock.EasyMock.*;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.junit.Test;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;

import java.util.ArrayList;
import java.util.List;


/**
 *
 */
public class AbstractVirtualResourcePostProcessorTest extends AbstractEasyMockTest {
  
  @Test
  public void test() throws Exception {
    SlingHttpServletRequest request = createNiceMock(SlingHttpServletRequest.class);
    Resource resource = createNiceMock(Resource.class);
    List<Modification> changes = new ArrayList<Modification>();
    changes.add(new Modification(ModificationType.CREATE, "/asdsad", "/b"));
    
    expect(request.getResource()).andReturn(resource);
    expect(resource.getResourceType()).andReturn("sakai/testing");
    replay();
    
    TAbstractVirtualResourcePostProcessor ta = new TAbstractVirtualResourcePostProcessor();
    assertNotNull(ta.getResourceType());
    ta.process(request, changes);
    verify();
  }

}