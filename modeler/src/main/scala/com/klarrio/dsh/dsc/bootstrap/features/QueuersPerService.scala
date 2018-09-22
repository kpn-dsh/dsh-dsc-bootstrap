package com.klarrio.dsh.dsc.bootstrap.features

import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions._


object QueuersPerService {
  def compute(dataFrame: DataFrame): DataFrame = {
    dataFrame.filter(col("pubTime") < col("dt_start") && col("pubTime") > date_trunc("minute", col("dt_offered_queue")))
      .groupBy("pubTime", "services_name")
      .count()
      .withColumnRenamed("count", "queuersForService")
  }
}
