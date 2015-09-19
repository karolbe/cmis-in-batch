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
package com.metasys;

import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;
import org.ikayzo.sdl.SDLParseException;
import org.ikayzo.sdl.Tag;

import com.metasys.cmis.CMISConnector;
import com.metasys.cmis.stages.ExecuteStage;
import com.metasys.cmis.stages.OperationFailedException;
import com.metasys.cmis.stages.PrepareStage;
import com.metasys.cmis.stages.PrevalidateStage;
import com.metasys.cmis.stages.ValidateStage;
import java.io.FileInputStream;

/**
 *
 * @author Karol Bryd <karol.bryd@metasys.pl>
 */
public class CMISInBatch {

    private static final Logger logger = Logger.getLogger(CMISInBatch.class);
    public static boolean stopOnFail = false;

    public void init(String scriptPath) throws Exception {
        Tag root = null;

        try {
            logger.info("Loading script...");
            root = new Tag("root").read(new InputStreamReader(
                    new FileInputStream(scriptPath),
                    "UTF8"));

            Tag connectionRoot = root.getChild("connection");
            Tag globalRoot = root.getChild("global-settings");
            Tag prevalidateRoot = root.getChild("prevalidate");
            Tag prepareRoot = root.getChild("prepare");
            Tag executeRoot = root.getChild("execute");
            Tag validateRoot = root.getChild("validate");

            CMISConnector connector = new CMISConnector();

            if(connectionRoot.getChild("extension-mode") != null)
                connector.setExtensionName(connectionRoot.getChild("extension-mode").stringValue());
            
            if(connectionRoot.getChild("service-mode") != null)
                connector.setServiceMode(connectionRoot.getChild("service-mode").stringValue());
            
            logger.info("Connecting to: " + connectionRoot.getChild("host") + " "
                    + connectionRoot.getChild("login") + " "
                    + connectionRoot.getChild("password") + " "
                    + connectionRoot.getChild("repository-id"));

            String repositoryId = null;
            if(connectionRoot.getChild("repository-id") != null)
                repositoryId = connectionRoot.getChild("repository-id").stringValue();
            
            if(connectionRoot.getChild("host") != null &&
                    connectionRoot.getChild("login") != null &&
                    connectionRoot.getChild("password") != null)
                connector.connect(connectionRoot.getChild("host").stringValue(),
                        connectionRoot.getChild("login").stringValue(),
                        connectionRoot.getChild("password").stringValue(),
                        repositoryId);
            else {
                System.out.println("Host, login and/or password was not defined.");
                System.exit(20);
            }

            logger.info("Connected to the repository " + connector.getRepositoryId());
            if(connectionRoot.getChild("stop-on-fail") != null)
                stopOnFail = Boolean.parseBoolean(connectionRoot.getChild("stop-on-fail").stringValue());
            
            if (globalRoot != null) {
                for (Tag child : globalRoot.getChildren()) {
                    if (child.getName().equalsIgnoreCase("stop-on-fail")) {
                        if (child.getValue().equals("true")) {
                            stopOnFail = true;
                        }
                    }
                }
            }

            if (prevalidateRoot != null) {
                PrevalidateStage prevalidateStage = new PrevalidateStage(connector);
                logger.info("Executing prevalidate stage...");
                prevalidateStage.execute(prevalidateRoot);
            }

            if (prepareRoot != null) {
                PrepareStage prepareStage = new PrepareStage(connector);
                logger.info("Executing prepare stage...");
                prepareStage.execute(prepareRoot);
            }

            if (executeRoot != null) {
                ExecuteStage executeStage = new ExecuteStage(connector);
                logger.info("Executing execute stage...");
                executeStage.execute(executeRoot);
            }

            if (validateRoot != null) {
                logger.info("Executing validate stage...");
                ValidateStage validateStage = new ValidateStage(connector);
                validateStage.execute(validateRoot);
            }

        } catch (IOException ioe) {
            logger.error("Problem reading config file" + ioe.getMessage());
        } catch (SDLParseException spe) {
            logger.error("Problem parsing config file " + spe.getMessage());
        } catch (OperationFailedException ofe) {
            logger.error("Encountered on an error: " + ofe.getMessage());
        }
    }

    public static void main(String args[]) throws Exception {
        CMISInBatch mainClass = new CMISInBatch();
        mainClass.init(args[0]);
    }
}
