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
package com.metasys.utils;

import com.metasys.cmis.CMISConnector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Karol Bryd <karol.bryd@metasys.pl>
 */
public class ExceptionParser {
 
    /**
     * Extracts the error message from the Alfresco exception message content.
     * 
     * @param connector the connection
     * @param error the error message (as HTML)
     * @return the extracted message.
     */
    public static String parseException(CMISConnector connector, String error) {
        if(error == null)
            return null;
        
        if(connector.getExtensionName().equals(CMISConnector.ALFRESCO)) {
            Pattern pat = Pattern.compile(".*Exception:.*\\ \\-\\ (.*?)\\<.*");
            Matcher matcher = pat.matcher(error);
            if(matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }    
}
