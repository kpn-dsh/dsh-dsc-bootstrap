package com.klarrio.dsh.dsc.bootstrap.features

import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.LongType

object AverageHandlingTime {
  def compute(dataFrame: DataFrame): DataFrame = {
    dataFrame.filter(col("pubTime") >= date_trunc("minute", col("dt_handled")))
      .withColumn("totalCallDuration", (col("dt_handled").cast(LongType) - col("dt_offered").cast(LongType)) / 60.0)
      .groupBy("pubTime")
      .agg(avg("totalCallDuration").alias("avgCallDuration"))
  }
}
