/*
 * Copyright 2014 Databricks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.databricks.spark.xml.util

import java.math.BigDecimal
import java.sql.{Date, Timestamp}
import java.time.{ZoneId, ZonedDateTime}
import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}
import java.util.Locale

import org.scalatest.funsuite.AnyFunSuite

import org.apache.spark.sql.types._
import com.databricks.spark.xml.XmlOptions
import com.databricks.spark.xml.util.TypeCast.isParseableAsZonedDateTime

final class TypeCastSuite extends AnyFunSuite {

  test("Can parse decimal type values") {
    val options = new XmlOptions()
    val stringValues = Seq("10.05", "1,000.01", "158,058,049.001")
    val decimalValues = Seq(10.05, 1000.01, 158058049.001)
    val decimalType = DecimalType.SYSTEM_DEFAULT

    stringValues.zip(decimalValues).foreach { case (strVal, decimalVal) =>
      val dt = new BigDecimal(decimalVal.toString)
      assert(TypeCast.castTo(strVal, decimalType, options) ===
        Decimal(dt, dt.precision(), dt.scale()))
    }
  }

  test("Nullable types are handled") {
    val options = new XmlOptions(Map("nullValue" -> "-"))
    for (t <- Seq(ByteType, ShortType, IntegerType, LongType, FloatType, DoubleType,
                  BooleanType, TimestampType, DateType, StringType)) {
      assert(TypeCast.castTo("-", t, options) === null)
    }
  }

  test("String type should always return the same as the input") {
    val options = new XmlOptions()
    assert(TypeCast.castTo("", StringType, options) === "")
  }

  test("Types are cast correctly") {
    val options = new XmlOptions()
    assert(TypeCast.castTo("10", ByteType, options) === 10)
    assert(TypeCast.castTo("10", ShortType, options) === 10)
    assert(TypeCast.castTo("10", IntegerType, options) === 10)
    assert(TypeCast.castTo("10", LongType, options) === 10)
    assert(TypeCast.castTo("1.00", FloatType, options) === 1.0)
    assert(TypeCast.castTo("1.00", DoubleType, options) === 1.0)
    assert(TypeCast.castTo("true", BooleanType, options) === true)
    assert(TypeCast.castTo("1", BooleanType, options) === true)
    assert(TypeCast.castTo("false", BooleanType, options) === false)
    assert(TypeCast.castTo("0", BooleanType, options) === false)
    assert(
      TypeCast.castTo("2002-05-30 21:46:54", TimestampType, options) ===
      Timestamp.from(
        ZonedDateTime.of(2002, 5, 30, 21, 46, 54, 0, ZoneId.of("UTC"))
        .toInstant()
      )
    )
    assert(
      TypeCast.castTo("2002-05-30T21:46:54", TimestampType, options) ===
      Timestamp.from(
        ZonedDateTime.of(2002, 5, 30, 21, 46, 54, 0, ZoneId.of("UTC"))
        .toInstant()
      )
    )
    assert(
      TypeCast.castTo("2002-05-30T21:46:54.1234", TimestampType, options) ===
      Timestamp.from(
        ZonedDateTime.of(2002, 5, 30, 21, 46, 54, 123400000, ZoneId.of("UTC"))
        .toInstant()
      )
    )
    assert(
      TypeCast.castTo("2002-05-30T21:46:54Z", TimestampType, options) ===
      Timestamp.from(
        ZonedDateTime.of(2002, 5, 30, 21, 46, 54, 0, ZoneId.of("UTC"))
        .toInstant()
      )
    )
    assert(
      TypeCast.castTo("2002-05-30T21:46:54-06:00", TimestampType, options) ===
      Timestamp.from(
        ZonedDateTime.of(2002, 5, 30, 21, 46, 54, 0, ZoneId.of("-06:00"))
        .toInstant()
      )
    )
    assert(
      TypeCast.castTo("2002-05-30T21:46:54+06:00", TimestampType, options) ===
      Timestamp.from(
        ZonedDateTime.of(2002, 5, 30, 21, 46, 54, 0, ZoneId.of("+06:00"))
        .toInstant()
      )
    )
    assert(
      TypeCast.castTo("2002-05-30T21:46:54.1234Z", TimestampType, options) ===
      Timestamp.from(
        ZonedDateTime.of(2002, 5, 30, 21, 46, 54, 123400000, ZoneId.of("UTC"))
        .toInstant()
      )
    )
    assert(
      TypeCast.castTo("2002-05-30T21:46:54.1234-06:00", TimestampType, options) ===
      Timestamp.from(
        ZonedDateTime.of(2002, 5, 30, 21, 46, 54, 123400000, ZoneId.of("-06:00"))
        .toInstant()
      )
    )
    assert(
      TypeCast.castTo("2002-05-30T21:46:54.1234+06:00", TimestampType, options) ===
      Timestamp.from(
        ZonedDateTime.of(2002, 5, 30, 21, 46, 54, 123400000, ZoneId.of("+06:00"))
        .toInstant()
      )
    )
    assert(TypeCast.castTo("2002-09-24", DateType, options) === Date.valueOf("2002-09-24"))
    assert(TypeCast.castTo("2002-09-24Z", DateType, options) === Date.valueOf("2002-09-24"))
    assert(TypeCast.castTo("2002-09-24-06:00", DateType, options) === Date.valueOf("2002-09-24"))
    assert(TypeCast.castTo("2002-09-24+06:00", DateType, options) === Date.valueOf("2002-09-24"))
  }

  test("Types with sign are cast correctly") {
    val options = new XmlOptions()
    assert(TypeCast.signSafeToInt("+10", options) === 10)
    assert(TypeCast.signSafeToLong("-10", options) === -10)
    assert(TypeCast.signSafeToFloat("1.00", options) === 1.0)
    assert(TypeCast.signSafeToDouble("-1.00", options) === -1.0)
  }

  test("Types with sign are checked correctly") {
    assert(TypeCast.isBoolean("true"))
    assert(TypeCast.isInteger("10"))
    assert(TypeCast.isLong("10"))
    assert(TypeCast.isDouble("+10.1"))
    val timestamp = "2015-01-01 00:00:00"
    assert(TypeCast.isTimestamp(timestamp, new XmlOptions()))
  }

  test("Float and Double Types are cast correctly with Locale") {
    val options = new XmlOptions()
    val defaultLocale = Locale.getDefault
    try {
      Locale.setDefault(Locale.FRANCE)
      assert(TypeCast.castTo("1,00", FloatType, options) === 1.0)
      assert(TypeCast.castTo("1,00", DoubleType, options) === 1.0)
    } finally {
      Locale.setDefault(defaultLocale)
    }
  }

  test("Test if string is parseable as a timestamp") {
    val supportedXmlTimestampFormatters = Seq(
      // 2002-05-30 21:46:54
      new DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .append(DateTimeFormatter.ISO_LOCAL_DATE)
        .appendLiteral(' ')
        .append(DateTimeFormatter.ISO_LOCAL_TIME)
        .toFormatter()
        .withZone(ZoneId.of("UTC")),
      // 2002-05-30T21:46:54
      DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.of("UTC")),
      // 2002-05-30T21:46:54+06:00
      DateTimeFormatter.ISO_OFFSET_DATE_TIME,
    )

    val supportedXmlTimestamps = Seq(
      "2002-05-30 21:46:54",
      "2002-05-30T21:46:54",
      "2002-05-30T21:46:54+06:00"
    )

    val checkBuiltInTimestamps = supportedXmlTimestampFormatters.zip(supportedXmlTimestamps)

    checkBuiltInTimestamps.foreach { case(format, value) =>
      assert(isParseableAsZonedDateTime(value, format))
    }
    sys.exit()

    assert(isParseableAsZonedDateTime(
      "12-03-2011 10:15:30 PST",
      DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss z")
    ))
    assert(isParseableAsZonedDateTime(
      "2011/12/03 16:15:30 +1000",
      DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss Z")
    ))
    assert(!isParseableAsZonedDateTime(
      "2011/12/03 16:15:30",
      DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
    ))
    assert(!isParseableAsZonedDateTime(
      "12-03-2011 10:15:30 PS",
      DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss z")
    ))
    assert(!isParseableAsZonedDateTime(
      "12-03-2011 10:15:30 PST",
      DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss")
    ))
  }
}
