package custom.rapidoidx.db;

/*
 * #%L
 * rapidoid-x-db-tests
 * %%
 * Copyright (C) 2014 - 2015 Nikolche Mihajlovski and contributors
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import org.rapidoid.annotation.Authors;
import org.rapidoid.annotation.Since;
import org.rapidoid.util.OptimisticConcurrencyControlException;
import org.rapidoidx.db.XDB;
import org.testng.annotations.Test;

import custom.rapidoidx.db.model.IPerson;

@Authors("Nikolche Mihajlovski")
@Since("3.0.0")
public class DbInterfaceOCCTest extends DbTestCommons {

	@Test(expectedExceptions = OptimisticConcurrencyControlException.class)
	public void testOCCFailure() {
		IPerson p1 = XDB.entity(IPerson.class);
		XDB.persist(p1);

		eq(p1.version(), 1);

		IPerson p2 = XDB.entity(IPerson.class);
		p2.id(p1.id());

		XDB.persist(p2);
	}

	@Test
	public void testOCC() {
		IPerson p1 = XDB.entity(IPerson.class);
		XDB.persist(p1);

		eq(p1.version(), 1);

		IPerson p2 = XDB.entity(IPerson.class);
		p2.id(p1.id());

		XDB.refresh(p2);
		eq(p2.version(), 1);

		XDB.persist(p2);
	}

}
