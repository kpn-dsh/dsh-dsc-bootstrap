package com.klarrio.dsh.dsc.bootstrap.features

import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions._

object QueuersAndCallers {
  def compute(dataFrame: DataFrame) = {
    val callersStream = dataFrame
      .groupBy("pubTime")
      .count()
      .withColumnRenamed("count", "amountOfCallers")

    val queuersStream = dataFrame
      .filter((col("pubTime") < col("dt_start")) && (col("pubTime") > date_trunc("minute", col("dt_offered_queue"))))
      .groupBy("pubTime")
      .count()
      .withColumnRenamed("count", "amountOfQueuers")
      .withColumnRenamed("pubTime", "pubT")

    callersStream
      .join(queuersStream, callersStream("pubTime") === queuersStream("pubT"), "left_outer")
      .drop("pubT")
      .na.fill(0, Seq("amountOfCallers", "amountOfQueuers"))
  }
}
