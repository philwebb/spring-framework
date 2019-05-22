/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.mock.web;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import org.springframework.util.FileCopyUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static temp.XAssert.assertEquals;
import static temp.XAssert.assertNull;

/**
 * @author Juergen Hoeller
 */
public class MockMultipartHttpServletRequestTests {

	@Test
	public void mockMultipartHttpServletRequestWithByteArray() throws IOException {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		assertThat(request.getFileNames().hasNext()).isFalse();
		assertNull(request.getFile("file1"));
		assertNull(request.getFile("file2"));
		assertThat(request.getFileMap().isEmpty()).isTrue();

		request.addFile(new MockMultipartFile("file1", "myContent1".getBytes()));
		request.addFile(new MockMultipartFile("file2", "myOrigFilename", "text/plain", "myContent2".getBytes()));
		doTestMultipartHttpServletRequest(request);
	}

	@Test
	public void mockMultipartHttpServletRequestWithInputStream() throws IOException {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		request.addFile(new MockMultipartFile("file1", new ByteArrayInputStream("myContent1".getBytes())));
		request.addFile(new MockMultipartFile("file2", "myOrigFilename", "text/plain", new ByteArrayInputStream(
			"myContent2".getBytes())));
		doTestMultipartHttpServletRequest(request);
	}

	private void doTestMultipartHttpServletRequest(MultipartHttpServletRequest request) throws IOException {
		Set<String> fileNames = new HashSet<>();
		Iterator<String> fileIter = request.getFileNames();
		while (fileIter.hasNext()) {
			fileNames.add(fileIter.next());
		}
		assertEquals(2, fileNames.size());
		assertThat(fileNames.contains("file1")).isTrue();
		assertThat(fileNames.contains("file2")).isTrue();
		MultipartFile file1 = request.getFile("file1");
		MultipartFile file2 = request.getFile("file2");
		Map<String, MultipartFile> fileMap = request.getFileMap();
		List<String> fileMapKeys = new LinkedList<>(fileMap.keySet());
		assertEquals(2, fileMapKeys.size());
		assertEquals(file1, fileMap.get("file1"));
		assertEquals(file2, fileMap.get("file2"));

		assertEquals("file1", file1.getName());
		assertEquals("", file1.getOriginalFilename());
		assertNull(file1.getContentType());
		assertThat(ObjectUtils.nullSafeEquals("myContent1".getBytes(), file1.getBytes())).isTrue();
		assertThat(ObjectUtils.nullSafeEquals("myContent1".getBytes(),
			FileCopyUtils.copyToByteArray(file1.getInputStream()))).isTrue();
		assertEquals("file2", file2.getName());
		assertEquals("myOrigFilename", file2.getOriginalFilename());
		assertEquals("text/plain", file2.getContentType());
		assertThat(ObjectUtils.nullSafeEquals("myContent2".getBytes(), file2.getBytes())).isTrue();
		assertThat(ObjectUtils.nullSafeEquals("myContent2".getBytes(),
			FileCopyUtils.copyToByteArray(file2.getInputStream()))).isTrue();
	}

}
