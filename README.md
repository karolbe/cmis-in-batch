# cmis-in-batch

This tool allows importing documents, creating folders, editing of properties and other operations which are allowed by the CMIS standard.

Tool is currently tested with Alfresco repositories only but it should work fine with Documentum 6.7, Nuxeo and other CMIS repositories supported by OpenCMIS.

## Introduction
cmis-in-batch was created as a very simple tool for uploading files into CMIS compliant repository. The problem was that my project (Stamper - PDF watermarking solution for Alfresco) expected some configuration files in the repository before it could be used. 

Of course I could import those files using Alfresco's ACP files but in order to achieve the same in Nuxeo or even Documentum I would have to prepare (and maintain) separate installation packages. Since I am lazy I decided to write one tool which would import initial configuration and sample files to all CMIS compatible repositories.

This tool was written also as an exercise to learn OpenCMIS APIs.

## Details
cmis-in-batch allows:

* importing files (custom types are available as well)
* creating folders (custom types are also available)
* updating properties of existing objects
* deleting files
* adding/removing aspects (currently available only for Alfresco repositories through alfresco-cmis-extensions)
checking for existence of types
* Additionally it can connect to a CMIS repository using webservices or REST. Basically any repository which is supported by OpenCMIS is also supported by cmis-in-batch.
* Generating (importing) sample data based on dictionaries, linking and naming rules.

By default Maven will create an executable JAR file, to run it execute command like this:

```
java -jar cmis-in-batch-main.jar <path to the script>
```

## Example script

```
connection {
    host "http://localhost:8080/alfresco/service/cmis"
    login "admin"
    password "admin"
    extension-mode "alfresco"
    service-mode "rest"
    stop-on-fail "false"
}

prevalidate {
    exists-folder "/Data Dictionary"
    exists-file "/Folder a"
    fail-when-type "not-exists" "metasys:layer"
}

prepare {
    create-folders "TestA" "TestB" baseFolder="/"
}

execute {
    import-files "/TestA" {
        "/home/kbryd/sig1.png" "cmis:document" "image/png" "scanneda1.png" date=2005/11/05
        "/home/kbryd/sig1.png" "cmis:document" "image/png" "scanneda2.png" date=2002/01/05
    }

    add-aspect "/TestA/scanneda1.png" "P:cm:titled212"
    add-aspect "/TestA/scanneda2.png" "P:cm:titled"

    import-file "/home/kbryd/signature.png" "/TestA" "cmis:document" "image/png" "scanneda3.png" date=2005/11/05
    
    delete-file "/TestA/scanneda1.png" all-versions="true"

    update-properties "/TestA/scanneda3.png" cm_title="something else.png"
    update-properties "/TestA/scanneda3.png" cm_sentdate=2006/11/01 14:45:19
    delete-file "/TestA/scanneda2.png"   
}

validate {
}
```

## Data generation.

Data generation is a useful feature that allows bulk importing of test data documents into CMIS compatible repository. Additionally it can populate metadata of document with values coming from predefined dictionaries. 

Sample script for generating thousands of documents can look like this one:

What it does is:

* it will load three dictionaries from files /tmp/disciplines, /tmp/types, /tmp/subtypes. The dictionaries are simple text files where values are separated by new line characters. From the dictionary values cartesian product will be calculated so for example, having three dictionaries:

* level1A, level1B
* level2A, level2B
* level3A, level3B

following combinations will be generated:

```
[level1A, level2A, level3A]
[level1A, level2A, level3B]
[level1A, level2B, level3A]
[level1A, level2B, level3B]
[level1B, level2A, level3A]
[level1B, level2A, level3B]
[level1B, level2B, level3A]
[level1B, level2B, level3B]
```

* it will import each file in content-path location ("/media/kbryd/Media/work/sample_data/department") to a location in repository defined with linking-rule: /Repository/${discipline}/static/${doctype}/sub/${docsubtype} - each ${} variable will be replaced by a value coming from appropriate dictionary. 
* naming-rule defines what the object name should be. It can use variables from the dictionaries plus a few additional: ${file_name},  ${file_size}, ${file_path}, ${file_ext}, ${file_mime}
* mapping defines the mapping for populating metadata of each document, e.g. in this case *discipline* attribute will be populated with value of *discipline*.
*
```
    generate-random-data "set1" {
        doc-type "cara_document"
        linking-rule "/Repository/${discipline}/static/${doctype}/sub/${docsubtype}"
        naming-rule "${file_name} - ${doctype}"
        content-path "/media/kbryd/Media/work/sample_data/department"
        randomize-order true

        mapping {
            discipline {
                "${discipline}"
            }
            doc_type {
                "${doctype}"
            }
            doc_subtype {
                "${docsubtype}"
            }
        }

        dictionaries {
            discipline "/tmp/disciplines"
            doctype "/tmp/types"
            docsubtype "/tmp/subtypes"
        }
    }
```



## CMIS in Documentum 6.7+

cmis-in-batch supports Documentum 6.7+ (obviously). There are some issues however due to the way how Documentum handles object names with the same names. Here is a snippet from Documentum 6.7 CMIS reference guide which describes the problem:

```The Documentum CMIS implementation therefore exposes unique document path segments when
requested using a combination of the object_name and r_object_id. This is done for every document,
not just those that cannot be addressed by path. The path segments take this form:
with name: 0900000b80001234_docname
without name: 0900000b80001234_
So in order to allow cmis-in-batch to work with Documentum repositories I have modified the way how paths to objects are handled as described above. This is not an optimal solution but it will work in all cases when objects in folders have unique names.
```
Another problem is that the Documentum CMIS implementation returns only three types:

cmis:folder
cmis:document
cmis:relationship
so there is no way of checking whether a custom Documentum type is available.

