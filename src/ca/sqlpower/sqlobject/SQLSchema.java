/*
 * Copyright (c) 2008, SQL Power Group Inc.
 *
 * This file is part of SQL Power Library.
 *
 * SQL Power Library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * SQL Power Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */
package ca.sqlpower.sqlobject;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * A SQLSchema is a container for SQLTables.  If it is in the
 * containment hierarchy for a given RDBMS, it will be directly above
 * SQLTables.  Its parent could be either a SQLDatabase or a SQLCatalog.
 */
public class SQLSchema extends SQLObject {
	private static final Logger logger = Logger.getLogger(SQLSchema.class);
	protected String nativeTerm;

	public SQLSchema(boolean populated) {
		this(null, null, populated);
	}

	public SQLSchema(SQLObject parent, String name, boolean populated) {
		if (parent != null && !(parent instanceof SQLCatalog || parent instanceof SQLDatabase)) {
			throw new IllegalArgumentException("Parent to SQLSchema must be SQLCatalog or SQLDatabase");
		}
		setParent(parent);
		setName(name);
		this.children = new LinkedList();
		this.nativeTerm = "schema";
		this.populated = populated;
	}

	public SQLTable getTableByName(String tableName) throws SQLObjectException {
		populate();
		Iterator childit = children.iterator();
		while (childit.hasNext()) {
			SQLTable child = (SQLTable) childit.next();
			logger.debug("getTableByName: is child '"+child.getName()+"' equal to '"+tableName+"'?");		
			if (child.getName().equalsIgnoreCase(tableName)) {
				return child;
			}
		}
		return null;
	}

	public String toString() {
		return getShortDisplayName();
	}

	public boolean isParentTypeDatabase() {
		return (getParent() instanceof SQLDatabase);
	}

	// ---------------------- SQLObject support ------------------------
	
	public String getShortDisplayName() {
		return  getName();
	}
	
	public boolean allowsChildren() {
		return true;
	}

	/**
	 * Populates this schema from the source database, if there
	 * is one.  Schemas that have no parent should not need to be
	 * autopopulated, because this makes no sense.
	 * 
	 * @throws NullPointerException if this schema has no parent database.
	 */
	public void populateImpl() throws SQLObjectException {
		if (populated) return;
		
		logger.debug("SQLSchema: populate starting");

		int oldSize = children.size();
		
		SQLObject tmp = getParent();
		while (tmp != null && (! (tmp instanceof SQLDatabase))) {
			tmp = tmp.getParent();
		}
		if (tmp == null) throw new IllegalStateException("Schema does not have a SQLDatabase ancestor. Can't populate.");
		SQLDatabase parentDatabase = (SQLDatabase) tmp;
		
		Connection con = null;
		ResultSet rs = null;
		try {
			synchronized (parentDatabase) {
				con = parentDatabase.getConnection();
				DatabaseMetaData dbmd = con.getMetaData();
				
				if ( getParent() instanceof SQLDatabase ) {
                    List<SQLTable> tables = SQLTable.fetchTablesForTableContainer(dbmd, null, getName());
                    for (SQLTable table : tables) {
                        table.setParent(this);
                        children.add(table);
                    }

				} else if ( getParent() instanceof SQLCatalog ) {
                    List<SQLTable> tables = SQLTable.fetchTablesForTableContainer(dbmd, getParent().getName(), getName());
                    for (SQLTable table : tables) {
                        table.setParent(this);
                        children.add(table);
                    }
				}
			}
		} catch (SQLException e) {
			throw new SQLObjectException("schema.populate.fail", e);
		} finally {
			populated = true;
			int newSize = children.size();
			if (newSize > oldSize) {
				int[] changedIndices = new int[newSize - oldSize];
				for (int i = 0, n = newSize - oldSize; i < n; i++) {
					changedIndices[i] = oldSize + i;
				}
				fireDbChildrenInserted(changedIndices, children.subList(oldSize, newSize));
			}
			try {
				if ( rs != null ) rs.close();
			} catch (SQLException e2) {
				logger.error("Could not close result set", e2);
			}
			try {
				if ( con != null ) con.close();
			} catch (SQLException e2) {
				logger.error("Could not close connection", e2);
			}
		}
		logger.debug("SQLSchema: populate finished");
	}


	// ----------------- accessors and mutators -------------------
	
	/**
	 * Gets the value of nativeTerm
	 *
	 * @return the value of nativeTerm
	 */
	public String getNativeTerm()  {
		return this.nativeTerm;
	}

	/**
	 * Sets the value of nativeTerm to a lowercase version of argNativeTerm.
	 *
	 * @param argNativeTerm Value to assign to this.nativeTerm
	 */
	public void setNativeTerm(String argNativeTerm) {
	    String oldValue = nativeTerm;
		if (argNativeTerm != null) argNativeTerm = argNativeTerm.toLowerCase();
		this.nativeTerm = argNativeTerm;
		fireDbObjectChanged("nativeTerm", oldValue, argNativeTerm);
	}

	@Override
	public Class<? extends SQLObject> getChildType() {
		return SQLTable.class;
	}

}