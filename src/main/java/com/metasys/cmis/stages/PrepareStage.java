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
import org.ikayzo.sdl.Tag;

/**
 *
 * @author Karol Bryd <karol.bryd@metasys.pl>
 */
public class PrepareStage extends Stage {

    public PrepareStage(CMISConnector connector) {
        super(connector);
        methodMappings.put("create-folders", "createFolders");
        methodMappings.put("create-folder", "createFolders");
    }

    public void createFolders(Tag tag) {
        String baseFolder = null;
        
        if(tag.getAttributes().containsKey("baseFolder"))
            baseFolder = (String)tag.getAttribute("baseFolder");
        
        for(Object folderToCreate : tag.getValues()) {
            if(tag.getAttributes().containsKey("baseType"))
                createFolder(baseFolder, (String)folderToCreate, (String)tag.getAttribute("baseType"));
            else
                createFolder(baseFolder, (String)folderToCreate);
        }
    }
}
