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

import com.metasys.cmis.CMISConnector;
import java.io.File;
import java.io.FileNotFoundException;
import org.ikayzo.sdl.Tag;

/**
 *
 * @author Karol Bryd <karol.bryd@metasys.pl>
 */
public class ExecuteStage extends Stage {
	
    public ExecuteStage(CMISConnector connector) {
        super(connector);
        
        methodMappings.put("import-files", "importFiles");
        methodMappings.put("import-file", "importFiles");
        methodMappings.put("add-aspect", "addAspects");
        methodMappings.put("remove-aspect", "removeAspects");
        methodMappings.put("replace-content", "replaceContent");
        methodMappings.put("delete-file", "deleteFile");
        methodMappings.put("delete-folder", "deleteFile");
        methodMappings.put("update-properties", "updateProperties");
        methodMappings.put("link-to-folder", "linkToFolder");
        methodMappings.put("unlink-from-folder", "unlinkFromFolder");
    }

    public void importFiles(Tag tag) throws FileNotFoundException {
        String destinationFolder = (String)tag.getValue();
        
        if(tag.hasChildren()) {
            for(Tag child : tag.getChildren()) {
                logger.info("Importing " + destinationFolder + " " + child.getValue() + " " + child.getAttributes());
                createDocument(new File((String)child.getValue()), 
                        (String)child.getValues().get(2), 
                        destinationFolder, 
                        (String)child.getValues().get(3), 
                        (String)child.getValues().get(1));
            }
        } else {
            logger.info("Importing " + tag.getValues().get(0) + " " + tag.getValues().get(1) + " " + tag.getAttributes());
            createDocument(new File((String)tag.getValue()), 
                    (String)tag.getValues().get(3), 
                    (String)tag.getValues().get(1), 
                    (String)tag.getValues().get(4), 
                    (String)tag.getValues().get(2));
        }
    }
    
    public void deleteFile(Tag tag) {
        boolean allVersions = false;
        
        if(tag.hasAttribute("all-versions") && tag.getAttribute("all-versions").equals("true"))
            allVersions = true;
        
        for(Object documentPath : tag.getValues()) {
            logger.info("Deleting document: " + documentPath + " allVersions? " + (allVersions?"yes":"no"));
            deleteFile((String)documentPath, allVersions);
        }
    }
    
    public void replaceContent(Tag tag) throws FileNotFoundException {
        replaceContent((String)tag.getValues().get(0), 
                (String)tag.getValues().get(1),
                new File((String)tag.getValues().get(2)),
                (String)tag.getValues().get(3));
        
    }
    
    public void updateProperties(Tag tag) {
        for(Object documentPath : tag.getValues()) {
            logger.info("Updating properties: " + documentPath);
            updateProperties((String)documentPath, tag.getAttributes());
        }
    }

    public void addAspects(Tag tag) {
        String documentPath = (String)tag.getValue();
        addAspects(documentPath, tag.getValues().subList(1, tag.getValues().size()));
    }

    public void removeAspects(Tag tag) {
        String documentPath = (String)tag.getValue();
        removeAspects(documentPath, tag.getValues().subList(1, tag.getValues().size()));
    }
    
    public void linkToFolder(Tag tag) {
        String documentPath = (String)tag.getValue();
        linkToFolder(documentPath, (String)tag.getValues().get(1));
    }
    
    public void unlinkFromFolder(Tag tag) {
        String documentPath = (String)tag.getValue();
        unlinkFromFolder(documentPath, (String)tag.getValues().get(1));
    }
    
}
