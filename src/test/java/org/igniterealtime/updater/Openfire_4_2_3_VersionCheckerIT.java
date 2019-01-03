/*
 * Copyright (C) 2018 Ignite Realtime Foundation. All rights reserved.
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
package org.igniterealtime.updater;

import org.apache.http.Consts;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.jivesoftware.site.Versions;
import org.jivesoftware.util.Version;
import org.junit.Test;

import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Integration test that verifies that instances of Openfire 4.2.3 will be able
 * to perform queries to determine if updates are available.
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 */
public class Openfire_4_2_3_VersionCheckerIT
{
    private static final String URL_BASE = "http://localhost:8080/";

    /**
     * This verifies that Openfire version 4.2.3 and older can parse the output
     * as generated by the endpoint that it uses to check for the latest version
     * of Openfire.
     *
     * This test constructs a request in much of the same way that Openfire 4.2.3
     * does, and then processes the response, again in the same way that Openfire
     * 4.2.3 does.
     *
     * If this test fails, then it's likely that instances of Openfire 4.2.3 and
     * before can not check if their version of Openfire is up-to-date.
     */
    @Test
    public void testOpenfireVersionCheck() throws Exception
    {
        // Setup test fixture.
        final HttpClient client = HttpClients.createDefault();
        final HttpPost request = new HttpPost( URL_BASE + "projects/openfire/versions.jsp" );
        final List<NameValuePair> form = new ArrayList<>();
        form.add( new BasicNameValuePair( "type", "update" ) );
        form.add( new BasicNameValuePair( "query", "<version><openfire current=\"4.0.0\"/></version>" ) );
        request.setEntity( new UrlEncodedFormEntity( form, Consts.UTF_8 ) );

        // Execute system under test.
        final HttpResponse response = client.execute( request );

        // Verify results.
        assertEquals( HttpStatus.SC_OK, response.getStatusLine().getStatusCode() );
        assertTrue( response.getFirstHeader( "content-type" ).getValue().startsWith( "text/xml" ) );
        assertNotNull( response.getEntity() );
        final String versionString = processServerUpdateResponse( EntityUtils.toString( response.getEntity() ) );
        assertNotNull( versionString );
        assertEquals( Versions.getVersion( "openfire" ), versionString );
    }

    /**
     * This mimics how Openfire 4.2.3 (and older) processes the response to the
     * 'check latest version of Openfire' request that is made from Openfire.
     * <p>
     * This code was taken from the method org.jivesoftware.openfire.update.UpdateManager#processServerUpdateResponse
     * as it was defined in Openfire 4.2.3.
     *
     * @param response The server response containing the latest version of Openfire.
     * @return The latest version available on the server, as parsed from the response.
     */
    public static String processServerUpdateResponse( String response ) throws Exception
    {
        SAXReader xmlReader = new SAXReader();
        xmlReader.setEncoding( "UTF-8" );
        Element xmlResponse = xmlReader.read( new StringReader( response ) ).getRootElement();

        // Parse response and keep info as Update objects
        Element openfire = xmlResponse.element( "openfire" );
        return openfire.attributeValue( "latest" );
    }

    /**
     * This verifies that Openfire version 4.2.3 and older can parse the output
     * as generated by the endpoint that it uses to check for the latest set
     * of available plugins.
     *
     * This test constructs a request in much of the same way that Openfire 4.2.3
     * does, and then processes the response, again in the same way that Openfire
     * 4.2.3 does.
     *
     * If this test fails, then it's likely that instances of Openfire 4.2.3 and
     * before can not check for the availability of plugins.
     */
    @Test
    public void testOpenfirePluginCheck() throws Exception
    {
        // Setup test fixture.
        final HttpClient client = HttpClients.createDefault();
        final HttpPost request = new HttpPost( URL_BASE + "projects/openfire/versions.jsp" );
        final List<NameValuePair> form = new ArrayList<>();
        form.add( new BasicNameValuePair( "type", "available" ) );
        form.add( new BasicNameValuePair( "query", "<available><locale>en</locale></available>" ) );
        request.setEntity( new UrlEncodedFormEntity( form, Consts.UTF_8 ) );

        // Execute system under test.
        final HttpResponse response = client.execute( request );

        // Verify results.
        assertEquals( HttpStatus.SC_OK, response.getStatusLine().getStatusCode() );
        assertTrue( response.getFirstHeader( "content-type" ).getValue().startsWith( "text/xml" ) );
        assertNotNull( response.getEntity() );
        final Set<String> reportedPluginNames = processAvailablePluginsResponse( EntityUtils.toString( response.getEntity() ) );
        assertNotNull( reportedPluginNames );
        assertTrue( reportedPluginNames.contains( "Bookmarks" ) );
    }

    /**
     * This mimics how Openfire 4.2.3 (and older) processes the response to the
     * 'retrieve list of available plugins' request that is made from Openfire.
     * <p>
     * This code was taken from the method org.jivesoftware.openfire.update.UpdateManager#processAvailablePluginsResponse
     * as it was defined in Openfire 4.2.3.
     *
     * @param response The server response containing available plugins
     * @return The available plugin names, as parsed from the response.
     */
    public static Set<String> processAvailablePluginsResponse( String response ) throws Exception
    {
        final Set<String> availablePlugins = new HashSet<>();

        // Parse response and keep info as AvailablePlugin objects
        SAXReader xmlReader = new SAXReader();
        xmlReader.setEncoding("UTF-8");
        Element xmlResponse = xmlReader.read(new StringReader(response)).getRootElement();
        Iterator plugins = xmlResponse.elementIterator( "plugin");
        while (plugins.hasNext()) {
            Element plugin = (Element) plugins.next();

            String pluginName = plugin.attributeValue("name");
            org.jivesoftware.util.Version latestVersion = null;
            String latestVersionValue = plugin.attributeValue("latest");
            if ( latestVersionValue != null && !latestVersionValue.isEmpty() )
            {
                latestVersion = new org.jivesoftware.util.Version( latestVersionValue );
            }

            URL icon = null;
            String iconValue = plugin.attributeValue("icon");
            if ( iconValue != null && !iconValue.isEmpty() )
            {
                try
                {
                    icon = new URL( iconValue );
                }
                catch ( MalformedURLException e )
                {
                    e.printStackTrace();
                }
            }

            URL readme = null;
            String readmeValue = plugin.attributeValue("readme");
            if ( readmeValue != null && !readmeValue.isEmpty() )
            {
                try
                {
                    readme = new URL( readmeValue );
                }
                catch ( MalformedURLException e )
                {
                    e.printStackTrace();
                }
            }

            URL changelog = null;
            String changelogValue = plugin.attributeValue("changelog");
            if ( changelogValue != null && !changelogValue.isEmpty() )
            {
                try
                {
                    changelog = new URL( changelogValue );
                }
                catch ( MalformedURLException e )
                {
                    e.printStackTrace();
                }
            }
            URL downloadUrl = null;
            String downloadUrlValue = plugin.attributeValue("url");
            if ( downloadUrlValue != null && !downloadUrlValue.isEmpty() )
            {
                try
                {
                    downloadUrl = new URL( downloadUrlValue );
                }
                catch ( MalformedURLException e )
                {
                    e.printStackTrace();
                }
            }

            String license = plugin.attributeValue("licenseType");
            String description = plugin.attributeValue("description");
            String author = plugin.attributeValue("author");

            org.jivesoftware.util.Version minServerVersion = null;
            String minServerVersionValue = plugin.attributeValue("minServerVersion");
            if ( minServerVersionValue != null && !minServerVersionValue.isEmpty() )
            {
                minServerVersion = new org.jivesoftware.util.Version( minServerVersionValue );
            }

            org.jivesoftware.util.Version priorToServerVersion = null;
            String priorToServerVersionValue = plugin.attributeValue("priorToServerVersion");
            if ( priorToServerVersionValue != null && !priorToServerVersionValue.isEmpty() )
            {
                priorToServerVersion = new Version( priorToServerVersionValue );
            }

            //JavaSpecVersion minJavaVersion = null;
            String minJavaVersionValue = plugin.attributeValue( "minJavaVersion" );
            if ( minJavaVersionValue != null && !minJavaVersionValue.isEmpty() )
            {
                //minJavaVersion = new JavaSpecVersion( minJavaVersionValue );
            }

            long fileSize = -1;
            String fileSizeValue = plugin.attributeValue("fileSize");
            if ( fileSizeValue != null && !fileSizeValue.isEmpty() )
            {
                fileSize = Long.parseLong( fileSizeValue );
            }

            String canonical = downloadUrlValue != null ? downloadUrlValue.substring( downloadUrlValue.lastIndexOf( '/' ) + 1, downloadUrlValue.lastIndexOf( '.' ) ) : null;

            availablePlugins.add(pluginName);

        }
        return availablePlugins;
    }
}
