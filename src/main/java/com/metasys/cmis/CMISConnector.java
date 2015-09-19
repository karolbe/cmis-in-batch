/*
 * Copyright (C) 2011 Metasys.pl
 * Written by Karol Bryd <karol.bryd@metasys.pl>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.metasys.cmis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.Repository;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.client.runtime.OperationContextImpl;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.enums.BindingType;

/**
 *
 * @author Karol Bryd <karol.bryd@metasys.pl>
 */
public class CMISConnector {
    public final static String ALFRESCO = "alfresco";
    public final static String DOCUMENTUM = "documentum";

    protected Session session;
    protected String serviceMode = "rest";
    protected String extensionName = "";
    
    public Session getSession() {
        return session;
    }

    public String getRepositoryId() {
        return session.getRepositoryInfo().getId();
    }

    public String getServiceMode() {
        return serviceMode;
    }

    public void setServiceMode(String serviceMode) {
        this.serviceMode = serviceMode;
    }

    public String getExtensionName() {
        return extensionName;
    }

    public void setExtensionName(String extensionName) {
        this.extensionName = extensionName;
    }

    public void connect(String url, String userName, String password, String repositoryId) throws Exception {
        Map<String, String> sessionParameters = null;
        
        if(serviceMode.equalsIgnoreCase("webservice"))
            sessionParameters = createWebServiceParameters(url, userName, password, repositoryId);
        else
            sessionParameters = createAtomPubParameters(url, userName, password, repositoryId);
        
        if(getExtensionName().equalsIgnoreCase(ALFRESCO))
            sessionParameters.put(SessionParameter.OBJECT_FACTORY_CLASS, "org.alfresco.cmis.client.impl.AlfrescoObjectFactoryImpl");

        SessionFactory sessionFactory = SessionFactoryImpl.newInstance();
        if(repositoryId == null) {
            List<Repository> repositories = sessionFactory.getRepositories(sessionParameters);
            sessionParameters.put(SessionParameter.REPOSITORY_ID, repositories.get(0).getId());
        }
        session = sessionFactory.createSession(sessionParameters);
        OperationContext context = new OperationContextImpl();
        context.setMaxItemsPerPage(100);
        session.setDefaultContext(context);
    }

    private Map<String, String> createAtomPubParameters(String url, String userName, String password, String repository) {
        Map<String, String> sessionParameters = new HashMap<String, String>();
        sessionParameters.put(SessionParameter.ATOMPUB_URL, url);
        sessionParameters.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());
        sessionParameters.put(SessionParameter.USER, userName);
        sessionParameters.put(SessionParameter.PASSWORD, password);
        if(repository != null)
            sessionParameters.put(SessionParameter.REPOSITORY_ID, repository);
        return sessionParameters;
    }

    private Map<String, String> createWebServiceParameters(String url, String userName, String password, String repository) {
        Map<String, String> sessionParameters = new HashMap<String, String>();
        sessionParameters.put(SessionParameter.BINDING_TYPE, BindingType.WEBSERVICES.value());
        sessionParameters.put(SessionParameter.WEBSERVICES_ACL_SERVICE, url + "/ACLService?wsdl");
        sessionParameters.put(SessionParameter.WEBSERVICES_DISCOVERY_SERVICE, url + "/DiscoveryService?wsdl");
        sessionParameters.put(SessionParameter.WEBSERVICES_MULTIFILING_SERVICE, url + "/MultiFilingService?wsdl");
        sessionParameters.put(SessionParameter.WEBSERVICES_NAVIGATION_SERVICE, url + "/NavigationService?wsdl");
        sessionParameters.put(SessionParameter.WEBSERVICES_OBJECT_SERVICE, url + "/ObjectService?wsdl");
        sessionParameters.put(SessionParameter.WEBSERVICES_POLICY_SERVICE, url + "/PolicyService?wsdl");
        sessionParameters.put(SessionParameter.WEBSERVICES_RELATIONSHIP_SERVICE, url + "/RelationshipService?wsdl");
        sessionParameters.put(SessionParameter.WEBSERVICES_REPOSITORY_SERVICE, url + "/RepositoryService?wsdl");
        sessionParameters.put(SessionParameter.WEBSERVICES_VERSIONING_SERVICE, url + "/VersioningService?wsdl");

        sessionParameters.put(SessionParameter.USER, userName);
        sessionParameters.put(SessionParameter.PASSWORD, password);
        if(repository != null)
            sessionParameters.put(SessionParameter.REPOSITORY_ID, repository);
        return sessionParameters;
    }
}
