package org.rapidoid.http;

/*
 * #%L
 * rapidoid-http
 * %%
 * Copyright (C) 2014 - 2015 Nikolche Mihajlovski and contributors
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.commons.io.FileUtils;
import org.rapidoid.annotation.Authors;
import org.rapidoid.annotation.Since;
import org.rapidoid.io.IO;
import org.rapidoid.util.Rnd;
import org.testng.annotations.Test;

@Authors("Nikolche Mihajlovski")
@Since("2.0.0")
public class HttpServerHeadersTest extends HttpTestCommons {

	@Test
	public void shouldHandleHttpRequests() throws IOException, URISyntaxException {

		server();

		server.get("/file", new Handler() {
			@Override
			public Object handle(HttpExchange x) {
				return x.download(x.subpath().substring(1) + ".txt").write("ab").write("cde");
			}
		});

		server.get("/bin", new Handler() {
			@Override
			public Object handle(HttpExchange x) {
				return x.binary().write("bin");
			}
		});

		server.get("/session", new Handler() {
			@Override
			public Object handle(HttpExchange x) {
				if (x.cookie("ses", null) == null) {
					x.setCookie("ses", "023B");
				}
				x.setCookie("key" + Rnd.rnd(100), "val" + Rnd.rnd(100));

				return x.writeJSON(x.cookies());
			}
		});

		server.get("/async", new Handler() {
			@Override
			public Object handle(HttpExchange x) {
				return x.async().write("now").done();
			}
		});

		server.get("/testfile1", new Handler() {
			@Override
			public Object handle(HttpExchange x) {
				return IO.file("test1.txt");
			}
		});

		server.get("/rabbit.jpg", new Handler() {
			@Override
			public Object handle(HttpExchange x) {
				return x.sendFile(IO.file("rabbit.jpg"));
			}
		});

		server.get("/ab", new Handler() {
			@Override
			public Object handle(HttpExchange x) {
				return x.sendFile(IO.file("ab.html"));
			}
		});

		server.serve(new Handler() {
			@Override
			public Object handle(HttpExchange x) {
				return x.setCookie("asd", "f").html().write("a<b>b</b>c");
			}
		});

		start();

		byte[] ab = FileUtils.readFileToByteArray(IO.file("ab.html"));

		for (int i = 0; i < 100; i++) {
			eq(get("/"), "a<b>b</b>c");
			eq(get("/xy"), "a<b>b</b>c");
			eq(get("/async"), "now");
			eq(get("/session"), "{}");
			eq(get("/bin"), "bin");
			eq(get("/file/foo"), "abcde");
			eq(get("/testfile1"), "TEST1");
			eq(getBytes("/ab"), ab);
		}

		shutdown();
	}

	@Test
	public void shouldRenderRabbit() throws Exception { // :)

		server();

		server.get("/rabbit.jpg", new Handler() {
			@Override
			public Object handle(HttpExchange x) {
				return x.sendFile(IO.file("rabbit.jpg"));
			}
		});

		start();

		byte[] rabbit = FileUtils.readFileToByteArray(IO.file("rabbit.jpg"));

		for (int i = 0; i < 1000; i++) {
			eq(getBytes("/rabbit.jpg"), rabbit);
		}

		shutdown();
	}

}
