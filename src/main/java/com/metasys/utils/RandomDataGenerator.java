package com.metasys.utils;

import com.google.common.collect.Lists;
import com.metasys.cmis.stages.ExecuteStage;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.log4j.Logger;
import org.ikayzo.sdl.Tag;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Random data importer.
 *
 * Created by kbryd on 5/15/16.
 */
public class RandomDataGenerator {

    protected final String FILE_NAME = "${file_name}";
    protected final String FILE_SIZE = "${file_size}";
    protected final String FILE_PATH = "${file_path}";
    protected final String FILE_EXT = "${file_ext}";
    protected final String FILE_MIMETYPE = "${file_mime}";

    private final Logger logger = Logger.getLogger(RandomDataGenerator.class);
    private final String linkingRule;
    private final String namingRule;
    private final String contentPath;
    private final String docType;
    private boolean randomizeOrder = false;
    private long maxObjects = -1;
    private boolean overwriteMode = false;
    private int threadNumber = 1;
    private Tag tag;
    private Map<String, List<String>> dictionaries = new HashMap<>();
    private Collection<File> files;

    private Map<String, String> mapping = new HashMap<>();

    public RandomDataGenerator(Tag tag) {
        this.tag = tag;
        if (tag.hasChild("max-objects")) {
            this.maxObjects = tag.getChild("max-objects").intValue();
        }
        if (tag.hasChild("randomize-order")) {
            this.randomizeOrder = tag.getChild("randomize-order").booleanValue();
        }
        if (tag.hasChild("overwrite-mode")) {
            this.overwriteMode = tag.getChild("overwrite-mode").booleanValue();
        }
        if (tag.hasChild("thread-number")) {
            threadNumber = tag.getChild("thread-number").intValue();
        }

        this.docType = tag.getChild("doc-type").stringValue();
        this.linkingRule = tag.getChild("linking-rule").stringValue();
        this.namingRule = tag.getChild("naming-rule").stringValue();
        this.contentPath = tag.getChild("content-path").stringValue();

        if (tag.hasChild("mapping")) {
            parseMapping(tag);
        }
    }

    public void execute(ExecuteStage executeStage) throws IOException {
        boolean processing = true;
        int currentIndex = 0;
        long availableFilesCount;
        List<String> variables;

        loadDictionaries();
        logger.info("Content path: " + contentPath);
        availableFilesCount = scanFiles();
        logger.info("Available files: " + availableFilesCount);

        logger.info("Linking rule: " + linkingRule);
        variables = extractVariables(linkingRule);
        logger.info("Number of variables: " + variables.toString());
        logger.info("Will process " + maxObjects + " objects.");

        List<List<String>> values = new ArrayList<>();
        for (List<String> dictionary : dictionaries.values()) {
            values.add(dictionary);
        }
        List<List<String>> results = Lists.cartesianProduct(values);

        if (randomizeOrder) {
            results = Lists.newLinkedList(results);
            Collections.shuffle(results);
        }

        if (maxObjects == -1) {
            maxObjects = availableFilesCount;
        }

        File[] filesArr = files.toArray(new File[]{});

        while (processing) {
            for (List<String> result : results) {
                File file = filesArr[currentIndex];

                ItemContext itemContext = new ItemContext();
                itemContext.file = file;

                String targetPath = processVariableString(result, linkingRule, itemContext);
                String targetName = processVariableString(result, namingRule, itemContext);

                currentIndex++;

                executeStage.createFolders(targetPath);
                boolean exists = executeStage.objectExists(targetPath + "/" + targetName);

                if(overwriteMode || !exists) {
                    if(exists) {
                        executeStage.deleteFile(targetPath + "/" + targetName, true);
                    }
                    Document object = executeStage.createDocument(file, Files.probeContentType(file.toPath()), targetPath, targetName, docType);

                    if (!mapping.keySet().isEmpty()) {
                        Map<String, Object> newProps = new HashMap<>();

                        for (String key : mapping.keySet()) {
                            newProps.put(key, processVariableString(result, mapping.get(key), itemContext));
                        }

                        object.updateProperties(newProps);
                    }
                    logger.info("Imported document to: '" + targetPath + "/" + targetName + "' with mime/type: '" + Files.probeContentType(file.toPath()) + "'");
                } else {
                    logger.info("Document exists at location: '" + targetPath + "/" + targetName + "'. Skipped.'");
                }

                if (currentIndex == maxObjects) {
                    processing = false;
                    break;
                }
            }
        }
    }

    private String processVariableString(List<String> variables, String rule, ItemContext itemContext) throws IOException {
        String result = rule;
        int n = 0;

        for (String dictionaryName : dictionaries.keySet()) {
            result = result.replace(dictionaryName, variables.get(n++));
        }

        List<String> remainingVariables = extractVariables(result);
        if (remainingVariables.size() > 0) {
            for (String variable : remainingVariables) {
                result = result.replace(variable, getVariableValue(variable, itemContext));
            }
        }
        return result;
    }

    private String getVariableValue(String variable, ItemContext itemContext) throws IOException {
        if (variable.equals(FILE_NAME)) {
            return itemContext.file.getName();
        } else if (variable.equals(FILE_PATH)) {
            return itemContext.file.getAbsolutePath();
        } else if (variable.equals(FILE_SIZE)) {
            return "" + itemContext.file.length();
        } else if (variable.equals(FILE_MIMETYPE)) {
            return "" + Files.probeContentType(itemContext.file.toPath());
        } else if (variable.equals(FILE_EXT)) {
            String name = itemContext.file.getName();
            int lastDot;
            if ((lastDot = name.lastIndexOf(".")) > 0) {
                return name.substring(lastDot);
            }
            return "";
        }

        logger.warn("Unable to resolve variable '" + variable + "'");
        return variable;
    }

    private void parseMapping(Tag tag) {
        Tag mappingTag = tag.getChild("mapping");

        Map<String, Object> childMap = mappingTag.getChildMap();

        for (String propKey : childMap.keySet()) {
            String result = "";

            for (Tag child : mappingTag.getChild(propKey).getChildren()) {
                result += child.stringValue() + " ";
            }
            result = result.trim();
            mapping.put(propKey, result);
            logger.info("Map property '" + propKey + "' to '" + result + "'");
        }
    }

    private void loadDictionaries() throws IOException {
        Tag dictionaries = tag.getChild("dictionaries");
        Map<String, String> map = dictionaries.getChildStringMap();
        for (String dictName : map.keySet()) {
            this.dictionaries.put("${" + dictName + "}", loadDictionary(map.get(dictName)));
        }
    }

    private List<String> loadDictionary(String file) throws IOException {
        FileReader fileReader = new FileReader(new File(file));
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        String line;

        List<String> dictionaryValues = new ArrayList<>();

        while ((line = bufferedReader.readLine()) != null) {
            dictionaryValues.add(line.trim());
        }
        return dictionaryValues;
    }

    private int scanFiles() {
        File dir = new File(contentPath);

        files = FileUtils.listFiles(dir, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);

        return files.size();
    }

    private List<String> extractVariables(String rule) {
        List<String> result = new ArrayList<>();

        String exp = "\\$\\{(\\w+?)\\}";
        Pattern p = Pattern.compile(exp);
        Matcher m = p.matcher(rule);
        while (m.find()) {
            result.add(m.group());
        }
        return result;
    }

    class ItemContext {
        File file;
    }
}
