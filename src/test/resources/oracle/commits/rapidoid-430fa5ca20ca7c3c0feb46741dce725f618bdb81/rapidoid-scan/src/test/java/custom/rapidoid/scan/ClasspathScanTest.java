package custom.rapidoid.scan;

import java.util.List;

import org.rapidoid.annotation.Authors;
import org.rapidoid.annotation.Since;
import org.rapidoid.cls.Cls;
import org.rapidoid.scan.Scan;
import org.rapidoid.test.TestCommons;
import org.rapidoid.util.U;
import org.testng.annotations.Test;

import cccccc.Ccccc;

import com.moja.Aaa;

/*
 * #%L
 * rapidoid-scan
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

@MyAnnot
class Foo {}

@Authors("Nikolche Mihajlovski")
@Since("2.0.0")
public class ClasspathScanTest extends TestCommons {

	@SuppressWarnings("unchecked")
	@Test
	public void testClasspathScanByName() {
		List<Class<?>> classes = Scan.classes(null, ".*ScanTest", null, null, null);

		eq(U.set(classes), U.set(ClasspathScanTest.class));

		classes = Scan.byName("Bar", null, null);

		eq(U.set(classes), U.set(Bar.class));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testClasspathScanByAnnotation() {
		List<Class<?>> classes = Scan.annotated(MyAnnot.class);

		eq(U.set(classes), U.set(Foo.class, Bar.class));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testScanAll() {
		List<Class<?>> classes = Scan.classes();

		eq(U.set(classes),
				U.set(Foo.class, Bar.class, MyAnnot.class, ClasspathScanTest.class, Aaa.class,
						Cls.getClassIfExists("Bbb"), Ccccc.class));
	}

}
