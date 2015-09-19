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
import com.metasys.utils.MessageFormatter;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.ikayzo.sdl.Tag;
import org.javatuples.Pair;
import org.javatuples.Unit;

/**
 *
 * @author Karol Bryd <karol.bryd@metasys.pl>
 */
public class PrevalidateStage extends Stage {
    public PrevalidateStage(CMISConnector connector) {
        super(connector);

        methodMappings.put("fail-when-folder", "existsObject");
        methodMappings.put("fail-when-file", "existsObject");
        methodMappings.put("fail-when-object", "existsObject");
        methodMappings.put("fail-when-type", "existsType");
        methodMappings.put("fail-when-user", "existsUser");
        methodMappings.put("fail-when-group", "existsGroup");
    }

    public void existsObject(Tag tag) throws ValidationFailedException {
        try {
            boolean negator = tag.getValue().equals("exists")?true:false;
            
            super.objectExists((String)tag.getValues().get(1));
        } catch(CmisObjectNotFoundException notFound) {
            throw new ValidationFailedException(MessageFormatter.formatMessage("objectNotExisting", new Pair<String, String>((String)tag.getValues().get(1), notFound.getMessage())));
        }
    }
    
    public void existsType(Tag tag) throws ValidationFailedException {
        boolean negator = tag.getValue().equals("exists")?true:false;
        
        if(negator == false && !super.typeExists((String)tag.getValues().get(1)))
            throw new ValidationFailedException(MessageFormatter.formatMessage("typeNotExisting", new Unit<String>((String)tag.getValues().get(1))));
        
        if(negator == true && super.typeExists((String)tag.getValues().get(1)))
            throw new ValidationFailedException(MessageFormatter.formatMessage("typeExisting", new Unit<String>((String)tag.getValues().get(1))));
        
    }
    
    public void existsUser(Tag tag) {
    }
    
    public void existsGroup(Tag tag) {
    }
    
}
