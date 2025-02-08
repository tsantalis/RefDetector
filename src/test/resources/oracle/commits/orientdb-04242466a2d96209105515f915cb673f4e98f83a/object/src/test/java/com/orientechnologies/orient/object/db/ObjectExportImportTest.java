package com.orientechnologies.orient.object.db;

import com.orientechnologies.orient.core.command.OCommandOutputListener;
import com.orientechnologies.orient.core.db.tool.ODatabaseExport;
import com.orientechnologies.orient.core.db.tool.ODatabaseImport;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;

/**
 * Created by tglman on 23/12/15.
 */
public class ObjectExportImportTest {

  @Test
  public void testExportImport() throws IOException {

    OObjectDatabaseTx db = new OObjectDatabaseTx("memory:test");
    db.create();
    db.setAutomaticSchemaGeneration(true);
    db.getMetadata().getSchema().synchronizeSchema();

    assertNotNull(db.getMetadata().getSchema().getClass("ODocumentWrapper"));
    byte[] bytes;
    ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
    new ODatabaseExport(db.getUnderlying(), byteOutputStream, new OCommandOutputListener() {
      @Override
      public void onMessage(String iText) {

      }
    }).exportDatabase().close();
    bytes = byteOutputStream.toByteArray();
    OObjectDatabaseTx db1 = new OObjectDatabaseTx("memory:test1");
    db1.create();
    db1.setAutomaticSchemaGeneration(true);
    db1.getMetadata().getSchema().synchronizeSchema();
    InputStream input = new ByteArrayInputStream(bytes);
    new ODatabaseImport(db1.getUnderlying(), input, new OCommandOutputListener() {
      @Override
      public void onMessage(String iText) {

      }
    }).importDatabase().close();
    assertNotNull(db1.getMetadata().getSchema().getClass("ODocumentWrapper"));
  }

}
