package com.databricks.mosaic.types

import org.apache.spark.sql.types.{StringType, StructField, StructType}

/**
 * Type definition for JSON encoding. JSON encoding is defined as (json: string).
 * This abstraction over StringType is needed to ensure matching
 * can distinguish between StringType (WKT) and JSONType (GEOJSON).
 */
class JSONType() extends StructType(
  Array(
    StructField("json", StringType)
  )
) {
  override def typeName: String = "json"
}
