package com.metasys.cmis;

import com.metasys.CMISInBatch;
import org.junit.Test;

/**
 * Created by kbryd on 5/15/16.
 */
public class CMISInBatchTest {

    @Test
    public void testSomething() throws Exception {
        System.out.println("test something");

        CMISInBatch cmisInBatch = new CMISInBatch();

        cmisInBatch.init("target/classes/com/metasys/alfresco/import_data_example.sdl");


    }


}