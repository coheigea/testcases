package org.apache.coheigea.santuario.benchmarks;
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


import java.io.StringReader;
import java.util.concurrent.TimeUnit;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.xml.security.utils.XMLUtils;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.xml.sax.InputSource;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)

/**
 * Some benchmarking tests for different ways of referencing keys in XML Signatures.
 */
public class XMLUtilsPerformanceBenchmark {

	@Benchmark
    @Fork(1)
    @Warmup(iterations = 5)
	@Threads(10)
	// New DocumentBuilder cache in Santuario 2.1.4+
    public void testXMLUtils() throws Exception {
        InputSource inputSource = new InputSource(new StringReader("<xml>123</xml>"));
        XMLUtils.read(inputSource, false);
    }
	
	/*
	@Benchmark
	@Fork(1)
	@Warmup(iterations = 5)
	@Threads(10)
	// Only available until Santuario 2.1.3
	public void testPooledDocumentBuilder() throws Exception {
		InputSource inputSource = new InputSource(new StringReader("<xml>123</xml>"));
		DocumentBuilder db = XMLUtils.createDocumentBuilder(false);
		db.parse(inputSource);
		XMLUtils.repoolDocumentBuilder(db);
	}
	*/

	@Benchmark
    @Fork(1)
    @Warmup(iterations = 5)
	@Threads(10)
	// Plain Java calls for comparison
    public void testCreateDocumentBuilder() throws Exception {
        DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
        dfactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
        dfactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        dfactory.setNamespaceAware(true);
        DocumentBuilder documentBuilder = dfactory.newDocumentBuilder();

        InputSource inputSource = new InputSource(new StringReader("<xml>123</xml>"));
        documentBuilder.parse(inputSource);
	}
    
}
