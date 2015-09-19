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

import java.util.Formatter;
import java.util.Locale;
import java.util.ResourceBundle;
import org.javatuples.Tuple;

/**
 *
 * @author Karol Bryd <karol.bryd@metasys.pl>
 */
public class MessageFormatter {

    static final public ResourceBundle  messages = ResourceBundle.getBundle("messages", Locale.getDefault());
    
    public static String formatMessage(String msgId, Tuple values) {
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);
        
        formatter.format(messages.getString(msgId), values.toArray());
        return sb.toString();
    }
}
