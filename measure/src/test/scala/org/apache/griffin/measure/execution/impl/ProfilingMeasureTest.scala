/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.griffin.measure.execution.impl

import org.apache.griffin.measure.configuration.dqdefinition.MeasureParam
import org.apache.griffin.measure.execution.Measure._
import org.apache.griffin.measure.execution.impl.ProfilingMeasure._

class ProfilingMeasureTest extends MeasureTest {
  var param: MeasureParam = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    param = MeasureParam("param", "Profiling", "source")
  }

  "ProfilingMeasure" should "validate expression config" in {

    // Default values
    val defaultProfilingMeasure = ProfilingMeasure(param)

    assertResult(3)(defaultProfilingMeasure.roundScale)
    assertResult(true)(defaultProfilingMeasure.approxDistinctCount)

    // Incorrect Type
    val invalidMeasure = ProfilingMeasure(
      param.copy(config = Map(ApproxDistinctCountStr -> "false", RoundScaleStr -> "1")))

    assertResult(3)(invalidMeasure.roundScale)
    assertResult(true)(invalidMeasure.approxDistinctCount)

    // Correct Type
    val validMeasure = ProfilingMeasure(
      param.copy(config = Map(ApproxDistinctCountStr -> false, RoundScaleStr -> 5)))

    assertResult(5)(validMeasure.roundScale)
    assertResult(false)(validMeasure.approxDistinctCount)
  }

  it should "support metric writing" in {
    val measure = ProfilingMeasure(param)
    assertResult(true)(measure.supportsMetricWrite)
  }

  it should "not support record writing" in {
    val measure = ProfilingMeasure(param)
    assertResult(false)(measure.supportsRecordWrite)
  }

  it should "profile all columns when no expression is provided" in {
    val measure = ProfilingMeasure(param)

    assertResult(3)(measure.roundScale)
    assertResult(true)(measure.approxDistinctCount)

    val (_, metricsDf) = measure.execute(context, None)

    assertResult(metricsDf.count())(1L)

    val row = metricsDf.head()
    assertResult(param.getDataSource)(row.getAs[String](DataSource))
    assertResult(param.getName)(row.getAs[String](MeasureName))
    assertResult(param.getType.toString)(row.getAs[String](MeasureType))

    val metricMap = row.getAs[Map[String, Any]](Metrics)
    assert(metricMap.contains(ColumnDetails))
    assert(metricMap(ColumnDetails).isInstanceOf[Map[String, Any]])

    val columnDetailsMap = metricMap(ColumnDetails).asInstanceOf[Map[String, Any]]
    assert(columnDetailsMap.size == sourceSchema.size)
  }

  it should "profile only selected columns if expression is provided" in {
    val measure = ProfilingMeasure(
      param.copy(config = Map(Expression -> "name, gender", ApproxDistinctCountStr -> false)))

    assertResult(3)(measure.roundScale)
    assertResult(false)(measure.approxDistinctCount)

    val (_, metricsDf) = measure.execute(context, None)

    assertResult(metricsDf.count())(1L)

    val row = metricsDf.head()
    assertResult(param.getDataSource)(row.getAs[String](DataSource))
    assertResult(param.getName)(row.getAs[String](MeasureName))
    assertResult(param.getType.toString)(row.getAs[String](MeasureType))

    val metricMap = row.getAs[Map[String, Any]](Metrics)
    assert(metricMap.contains(ColumnDetails))
    assert(metricMap(ColumnDetails).isInstanceOf[Map[String, Any]])

    val columnDetailsMap = metricMap(ColumnDetails).asInstanceOf[Map[String, Any]]
    assert(columnDetailsMap.size == 2)
  }

  it should "throw runtime error for invalid expr" in {
    assertThrows[AssertionError] {
      ProfilingMeasure(param.copy(config = Map(Expression -> "xyz")))
        .execute(context)
    }
  }

}
