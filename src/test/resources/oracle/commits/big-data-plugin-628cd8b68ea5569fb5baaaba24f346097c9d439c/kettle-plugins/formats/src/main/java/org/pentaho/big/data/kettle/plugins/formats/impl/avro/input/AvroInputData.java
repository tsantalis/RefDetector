/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2017 by Hitachi Vantara : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.pentaho.big.data.kettle.plugins.formats.impl.avro.input;

import java.util.Iterator;
import java.util.List;

import org.pentaho.di.core.RowMetaAndData;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.steps.file.BaseFileInputStepData;
import org.pentaho.hadoop.shim.api.format.IPentahoInputFormat.IPentahoRecordReader;
import org.pentaho.hadoop.shim.api.format.IPentahoInputFormat.IPentahoInputSplit;
import org.pentaho.hadoop.shim.api.format.IPentahoAvroInputFormat;

public class AvroInputData extends BaseFileInputStepData {
  IPentahoAvroInputFormat input;
  List<IPentahoInputSplit> splits;
  int currentSplit;
  IPentahoRecordReader reader;
  Iterator<RowMetaAndData> rowIterator;
  RowMetaInterface outputRowMeta;
}
