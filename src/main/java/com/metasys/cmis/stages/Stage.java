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
package com.metasys.cmis.stages;

import com.metasys.CMISInBatch;
import com.metasys.cmis.CMISConnector;
import com.metasys.utils.ExceptionParser;
import com.metasys.utils.MessageFormatter;
import org.alfresco.cmis.client.AlfrescoDocument;
import org.apache.chemistry.opencmis.client.api.*;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.exceptions.CmisBaseException;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;
import org.apache.commons.beanutils.MethodUtils;
import org.apache.log4j.Logger;
import org.ikayzo.sdl.Tag;
import org.javatuples.Pair;
import org.javatuples.Triplet;
import org.javatuples.Unit;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author karol.bryd@metasys.pl
 */
public abstract class Stage {

    protected final Logger logger = Logger.getLogger(Stage.class);
    protected CMISConnector connector;
    protected HashMap<String, String> methodMappings = new HashMap<String, String>();
    protected ItemIterable<ObjectType> typesTree;

    public Stage(CMISConnector connector) {
        this.connector = connector;
        typesTree = connector.getSession().getTypeChildren(null, true);
    }

    public boolean objectExists(String path) {
        try {
            return connector.getSession().getObjectByPath(convert2UniquePath(path)) != null;
        } catch (Exception e) {
            // ignore
        }
        return false;
    }

    public Folder createFolders(String path) throws CmisBaseException {
        String _path[] = path.split("/");
        String curpath = "/";
        Folder last = null;
        for (int n = 1; n < _path.length; n++) {
            try {
                last = createFolder(!curpath.equals("/") ? curpath.substring(0, curpath.length() - 1) : curpath, _path[n], "cmis:folder");
            } catch (CmisBaseException o) {
                //pass
            }

            curpath += _path[n] + "/";
        }
        return last;
    }

    public Folder createFolder(String path, String folderName) throws CmisBaseException {
        return createFolder(path, folderName, "cmis:folder");
    }

    public Folder createFolder(String path, String folderName, String objectTypeId) throws CmisBaseException {
        if (logger.isDebugEnabled()) {
            logger.debug("Creating folder '" + folderName + "' in location '" + path + "' folder type is '" + objectTypeId + "'");
        }

        String tmp = path;
        if (tmp.endsWith("/")) {
            tmp += folderName;
        } else {
            tmp += "/" + folderName;
        }

        if (objectExists(tmp)) {
            String message = MessageFormatter.formatMessage("folderExists", new Pair<>(folderName, path));
            logger.error(message);
            throw new OperationFailedException(message);
        }

        Folder root;
        if (path == null) {
            root = connector.getSession().getRootFolder();
        } else {
            root = (Folder) connector.getSession().getObjectByPath(path);
        }

        Map<String, String> newFolderProps = new HashMap<String, String>();
        newFolderProps.put(PropertyIds.OBJECT_TYPE_ID, objectTypeId);
        newFolderProps.put(PropertyIds.NAME, folderName);
        Folder folder = root.createFolder(newFolderProps, null, null, null, connector.getSession().getDefaultContext());
        if (logger.isDebugEnabled()) {
            logger.debug("Created folder " + folder.getId());
        }
        logger.info(MessageFormatter.formatMessage("createFolder", new Triplet<>(folderName, objectTypeId, path)));
        return folder;
    }

    public Document createDocument(File content, String mimeType, String path, String documentName, String objectTypeId) throws CmisBaseException, FileNotFoundException {
        if (logger.isDebugEnabled()) {
            logger.debug("Creating document '" + documentName + "' in location '" + path + "' document type is '" + objectTypeId + "'");
        }

        String tmp = path;
        if (tmp.endsWith("/")) {
            tmp += documentName;
        } else {
            tmp += "/" + documentName;
        }

        if (objectExists(tmp)) {
            String message = MessageFormatter.formatMessage("documentExists", new Pair<>(documentName, path));
            logger.info(message);
            throw new OperationFailedException(message);
        }

        Folder root;
        if (path == null) {
            root = connector.getSession().getRootFolder();
        } else {
            root = (Folder) connector.getSession().getObjectByPath(path);
        }

        Map<String, String> newDocProps = new HashMap<String, String>();
        newDocProps.put(PropertyIds.OBJECT_TYPE_ID, objectTypeId);
        newDocProps.put(PropertyIds.NAME, documentName);

        ContentStream contentStream = new ContentStreamImpl(documentName, null, mimeType, new FileInputStream(content));
        Document doc = root.createDocument(newDocProps, contentStream, null, null, null, null, connector.getSession().getDefaultContext());
        logger.info(MessageFormatter.formatMessage("createDocument", new Triplet<>(documentName, objectTypeId, path)));
        return doc;
    }

    public void updateProperties(String path, Map<String, Object> properties) throws CmisBaseException {
        if (logger.isDebugEnabled()) {
            logger.debug("Updating properties of document '" + path);
        }
        CmisObject object;
        if (path == null) {
            return;
        } else {
            object = connector.getSession().getObjectByPath(convert2UniquePath(path));
        }
        Map<String, Object> newProps = new HashMap<String, Object>();
        for (String key : properties.keySet()) {
            if (key.contains("_")) {
                newProps.put(key.replace('_', ':'), properties.get(key));
            } else {
                newProps.put(key, properties.get(key));
            }
        }

        object.updateProperties(newProps);
        logger.info(MessageFormatter.formatMessage("updateProperties", new Unit<String>(path)));
    }
/*
    private void printContent(CmisObject path, int level) {
        ItemIterable<CmisObject> files = ((Folder)path).getChildren();
        for(CmisObject file : files) {
            for(int n = 0; n < level; n++) 
                System.out.print(" ");
            System.out.print(file.getName() + " " + file.getBaseTypeId());
            if(file.getBaseTypeId().equals(BaseTypeId.CMIS_FOLDER)) {
                System.out.println(" " + ((Folder)file).getPath());
                printContent(file, level + 1);
            } else
                System.out.println();
        }
    }
*/

    /**
     * Helper for Documentum CMIS. It converts the path to a 'unique' path which contains object IDs instead of plain object names.
     * E.g. path /dmadmin/images/picture.jpg will be converted to /dmadmin/images/0900000b80001234_picture.jpg
     *
     * @param path
     * @return converted path
     */
    public String convert2UniquePath(String path) {

        if (!connector.getExtensionName().equalsIgnoreCase(CMISConnector.DOCUMENTUM)) {
            return path;
        }

        StringBuilder sb = new StringBuilder();

        if (path.indexOf("/") == -1) {
            return sb.append(path).toString();
        }

        Folder folder = (Folder) connector.getSession().getObjectByPath(path.substring(0, path.lastIndexOf("/")));
        String objectName = path.substring(path.lastIndexOf("/") + 1);

        sb.append(folder.getPath());
        sb.append("/");

        ItemIterable<CmisObject> files = folder.getChildren();
        for (CmisObject obj : files) {
            if (obj.getBaseTypeId().equals(BaseTypeId.CMIS_DOCUMENT) && obj.getName().equals(objectName)) {
                sb.append(obj.getId());
                sb.append("_");
                sb.append(obj.getName());
                return sb.toString();
            }
        }
        return path;
    }

    public void deleteFile(String path, boolean allVersions) throws CmisBaseException {
        CmisObject object = connector.getSession().getObjectByPath(convert2UniquePath(path));
        if (object != null) {
            object.delete(allVersions);
            logger.info(MessageFormatter.formatMessage("deleteFile", new Unit<>(path)));
        }
    }

    public void addAspects(String path, List<Object> aspects) throws CmisBaseException {
        CmisObject object = connector.getSession().getObjectByPath(convert2UniquePath(path));
        if (object != null) {
            for (Object aspect : aspects) {
                AlfrescoDocument doc = (AlfrescoDocument) object;
                doc.addAspect((String) aspect);
                logger.info(MessageFormatter.formatMessage("addAspect", new Unit<>((String) aspect)));
            }
        }
    }

    public void removeAspects(String path, List<Object> aspects) throws CmisBaseException {
        CmisObject object = connector.getSession().getObjectByPath(convert2UniquePath(path));
        if (object != null) {
            for (Object aspect : aspects) {
                AlfrescoDocument doc = (AlfrescoDocument) object;
                doc.removeAspect((String) aspect);
                logger.info(MessageFormatter.formatMessage("removeAspect", new Unit<>((String) aspect)));
            }
        }
    }

    public void replaceContent(String path, String version, File newContent, String mimeType) throws FileNotFoundException, CmisBaseException {
        Document document = (Document) connector.getSession().getObjectByPath(convert2UniquePath(path));

        List<Document> allVersions = document.getAllVersions();

        for (Document versionDoc : allVersions) {
            if (version.equalsIgnoreCase("all")
                    || versionDoc.getVersionLabel().contains(version)) {
                logger.info("Processing version " + versionDoc.getVersionLabel() + " " + versionDoc.getId());
                ContentStream contentStream = new ContentStreamImpl(newContent.getName(), null, mimeType, new FileInputStream(newContent));
                versionDoc.setContentStream(contentStream, true, true);
            }
        }
    }

    public void linkToFolder(String path, String folderPath) {
        Document document = (Document) connector.getSession().getObjectByPath(convert2UniquePath(path));
        Folder folder = (Folder) connector.getSession().getObjectByPath(folderPath);
        document.addToFolder(folder, true);
        logger.info(MessageFormatter.formatMessage("linkToFolder", new Pair<>(path, folderPath)));
    }

    public void unlinkFromFolder(String path, String folderPath) {
        Document document = (Document) connector.getSession().getObjectByPath(convert2UniquePath(path));
        Folder folder = (Folder) connector.getSession().getObjectByPath(folderPath);
        document.removeFromFolder(folder);
        logger.info(MessageFormatter.formatMessage("unlinkFromFolder", new Pair<>(path, folderPath)));
    }

    private boolean checkType(ObjectType type, String searchedType) {
        if (type.getChildren().getTotalNumItems() > 0) {
            for (ObjectType o : type.getChildren()) {
                if (checkType(o, searchedType)) {
                    return true;
                }
            }
        } else {
            if (type.getQueryName().equals(searchedType)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Found type '" + searchedType + "' in the repository");
                }
                return true;
            }
        }
        return false;
    }

    public boolean typeExists(String typeName) {
        for (ObjectType type : typesTree) {
            if (checkType(type, typeName)) {
                return true;
            }
        }
        return false;
    }

    public void execute(Tag root) throws OperationFailedException {
        List<Tag> children = root.getChildren();

        for (Tag c : children) {
            if (!methodMappings.containsKey(c.getName())) {
                logger.error("Invalid action specified: " + c.getName());
                continue;
            }
            boolean failed = false;
            String errorMessage = "";
            try {
                MethodUtils.invokeMethod(this, methodMappings.get(c.getName()), c);
            } catch (NoSuchMethodException noMethod) {
                logger.error("Error:", noMethod);
                failed = true;
            } catch (IllegalAccessException illegalMethod) {
                logger.error("Error:", illegalMethod);
                failed = true;
            } catch (InvocationTargetException invocationMethod) {
                logger.error("Error:", invocationMethod);
                Throwable cause = invocationMethod.getCause();
                if (cause != null && cause instanceof CmisBaseException) {
                    errorMessage = ExceptionParser.parseException(connector, ((CmisBaseException) cause).getErrorContent());
                    if (errorMessage == null)
                        errorMessage = cause.getLocalizedMessage();
                } else
                    errorMessage = cause.getMessage();
                failed = true;
            }

            String exceptionMessage = "Failed during operation '" + c.getName() + "' with error: " + errorMessage;
            if (CMISInBatch.stopOnFail && failed)
                throw new OperationFailedException(exceptionMessage);
            else if (failed)
                logger.error(exceptionMessage);
        }
    }
}
