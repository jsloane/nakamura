/**
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
package org.sakaiproject.nakamura.files.migrator;

import com.google.common.collect.ImmutableMap;
import junit.framework.Assert;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.api.files.FilesConstants;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessControlManager;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AclModification;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.resource.lite.LiteJsonImporter;
import org.sakaiproject.nakamura.lite.BaseMemoryRepository;
import org.sakaiproject.nakamura.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.InputStream;

public class DocMigratorTest extends Assert {
  private static final Logger LOGGER = LoggerFactory.getLogger(DocMigratorTest.class);

  private static final int ALL_ACCESS = 28679;
  
  private Repository repository;
  
  private DocMigrator docMigrator;

  private Session session;

  private JSONObject readJSONFromFile(String fileName) throws IOException, JSONException {
    InputStream in = getClass().getClassLoader().getResourceAsStream(fileName);
    return new JSONObject(IOUtils.readFully(in, "utf-8"));
  }

  @Before
  public void setup() throws Exception {
    docMigrator = new DocMigrator();
    repository = new BaseMemoryRepository().getRepository();
    docMigrator.repository = repository;
    session = repository.loginAdministrative();
  }

  @Test
  public void detect_requires_migration() throws Exception {
    boolean requiresMigration = docMigrator.fileContentNeedsMigration(new Content("/some/path",
        ImmutableMap.of("structure0", "{}", FilesConstants.SCHEMA_VERSION, (Object) 2)));
    assertFalse(requiresMigration);
  }
  
  @Test
  public void test_requires_migration() throws Exception {
    ContentManager contentManager = mock(ContentManager.class);
    boolean requiresMigration = docMigrator.requiresMigration(
        readJSONFromFile("StructureZero.json"), new Content("/foo/bar",
        ImmutableMap.of("_lastModifiedBy", (Object) "zach")), contentManager);
    assertTrue(requiresMigration);
  }

  @Test
  public void use_pure_java_migrator() throws Exception {
    JSONObject oldStructure = readJSONFromFile("SampleOldStructure.json");
    JSONObject structure0 = readJSONFromFile("StructureZero.json");
    JSONObject newStructure = docMigrator.createNewPageStructure(structure0, oldStructure);
    JSONObject convertedStructure = (JSONObject) docMigrator.convertArraysToObjects(newStructure);
    docMigrator.validateStructure(convertedStructure);
  }
  
  @Test
  public void test_doc_with_discussion() throws Exception {
    JSONObject docStructure = readJSONFromFile("DocWithAdditionalPage.json");
    JSONObject docStructureZero = readJSONFromFile("DocWithAdditionalPageStructureZero.json");
    JSONObject newStructure = docMigrator.createNewPageStructure(docStructureZero, docStructure);
    JSONObject convertedStructure = (JSONObject) docMigrator.convertArraysToObjects(newStructure);
    LOGGER.info(newStructure.toString(2));
    docMigrator.validateStructure(convertedStructure);
    assertTrue("We expect sakai:pooled-content-viewer to be an array of Strings.",
        convertedStructure.get("sakai:pooled-content-viewer") instanceof JSONArray);
  }
  
  @Test
  public void make_sure_croby_pubspace_will_migrate() throws Exception {
    final String CROBY_NAME = "croby";
    final String CROBY_PATH = "a:" + CROBY_NAME;
    final String CROBY_PUBSPACE_PATH = CROBY_PATH + "/public/pubspace";    
    session.getAuthorizableManager().createUser(CROBY_NAME, CROBY_NAME, "shhhh", null);
    session.getAccessControlManager().setAcl("CO", CROBY_PATH, new AclModification[]{new AclModification(CROBY_NAME + "@g", ALL_ACCESS, AclModification.Operation.OP_REPLACE)});
    session.logout();
    session = repository.loginAdministrative(CROBY_NAME);
    ContentManager contentManager = session.getContentManager();
    AccessControlManager accessControlManager = session.getAccessControlManager();
    JSONObject crobyPubspace = readJSONFromFile("CrobyPubspace.json");
    LiteJsonImporter jsonImporter = new LiteJsonImporter();
    jsonImporter.internalImportContent(contentManager, crobyPubspace, CROBY_PUBSPACE_PATH, true, accessControlManager);
    Content crobyPubspaceContent = contentManager.get(CROBY_PUBSPACE_PATH);
    assertTrue(docMigrator.fileContentNeedsMigration(crobyPubspaceContent));
    docMigrator.migrateFileContent(crobyPubspaceContent);
    crobyPubspaceContent = contentManager.get(CROBY_PUBSPACE_PATH);
    assertEquals(CROBY_NAME, crobyPubspaceContent.getProperty("_lastModifiedBy"));
  }

  @Test
  public void handle_missing_page_element() throws Exception {
    JSONObject docStructure = readJSONFromFile("MissingPageElement.json");
    JSONObject docStructureZero = readJSONFromFile("MissingPageElementStructureZero.json");
    JSONObject newStructure = docMigrator.createNewPageStructure(docStructureZero, docStructure);
  }
  
  @Test
  public void testListSpanningComments() throws Exception {
    // KERN-2672: list content that spans a comments widget goes missing after migration
    JSONObject doc = readJSONFromFile("ListSpanningComments.json"); 
    JSONObject migrated = docMigrator.createNewPageStructure(
        new JSONObject(doc.getString("structure0")), doc);
    LOGGER.info("Migrated kern2672=" + migrated.toString(2));
    // TODO fix logic and write asserts to check it
  }

  @Test
  public void testCommentSettings() throws Exception {
    // KERN-2674: comments widget settings not honored after migration
    JSONObject doc = readJSONFromFile("CommentSettingsNotHonored.json");
    JSONObject migrated = docMigrator.createNewPageStructure(
        new JSONObject(doc.getString("structure0")), doc);
    LOGGER.info("Migrated kern2674=" + migrated.toString(2));
    // TODO fix logic and write asserts to check it
  }

  @Test
  public void testDiscussionSettings() throws Exception {
    // KERN-2675: discussion widget settings not honored after migration
    JSONObject doc = readJSONFromFile("DiscussionSettingsNotHonored.json");
    JSONObject migrated = docMigrator.createNewPageStructure(
        new JSONObject(doc.getString("structure0")), doc);
    LOGGER.info("Migrated kern2675=" + migrated.toString(2));
    // TODO fix logic and write asserts to check it
  }

}
