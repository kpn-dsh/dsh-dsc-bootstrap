package com.klarrio.dsh.dsc.bootstrap

import com.klarrio.dsh.datascience.bootstrap.features.{AverageHandlingTime, QueuersAndCallers, WaitTime}
import com.klarrio.dsh.dsc.bootstrap.features.{AverageHandlingTime, QueuersAndCallers, WaitTime}
import com.klarrio.dsh.dsc.bootstrap.modelers.AmtCallers15mModeler
import com.klarrio.dsh.kpn.datascience.bootstrap.exercise.features.{AverageHandlingTime, QueuersAndCallers, WaitTime}
import com.klarrio.dsh.kpn.datascience.bootstrap.exercise.modelers._
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

object ModelerMain {
  def main(args: Array[String]): Unit = {
    /**
      * Change this to the modeler you will be working on:
      * WaitTime5mModeler / WaitTime15mModeler / AmtCallers15mModeler / AmtQueuers15mModeler
      */
    val modeler = AmtCallers15mModeler

    // Initialize Spark
    val sparkSession = initSpark()

    // Read the callcenter log files with the header and infer the schema
    val callcenterLogs = sparkSession.read
      .option("inferSchema", "true")
      .option("header", "true")
      .csv("src/main/resources/callcenter_logs.csv")

    // Compute the feature columns
    val queuersAndCallers = QueuersAndCallers.compute(callcenterLogs)
    val waitTime = WaitTime.compute(callcenterLogs)
      .withColumnRenamed("pubTime", "pubTimeWaitTime")
    val averageHandlingTime = AverageHandlingTime.compute(callcenterLogs)
      .withColumnRenamed("pubTime", "pubTimeHandlingTime")

    // Join all the feature Dataframes together
    // compute the predictor
    // drop null values (about 30 rows in our dataset)
    val data = queuersAndCallers //.join(queuersPerService, "pubTime")
      .join(waitTime, col("pubTime") === col("pubTimeWaitTime"), "left_outer")
      .join(averageHandlingTime, col("pubTime") === col("pubTimeHandlingTime"), "left_outer")
      .drop("pubTimeWaitTime", "pubTimeHandlingTime")

    modeler.createModel(data, printOutput = true)
  }

  def initSpark(): SparkSession = {
    val sparkSession: SparkSession = SparkSession.builder
      .appName("dsh-kpn-datascience-bootstrap-modeler")
      .master("local[*]")
      .getOrCreate
    sparkSession
  }
}




//    val firstTimestampLogs = callcenterLogs.drop("pubTime").distinct()
//      .withColumn("pubTime", to_timestamp(date_trunc("minute", col("dt_offered")).cast("long") + 60))
//      .select("pubTime", "caseId", "dt_offered", "dt_handled", "contact_sk", "dt_offered_service", "dt_offered_queue", "dt_start", "dt_end", "services_sk", "services_name", "ind_forward_service_previous", "area", "subarea")
//
//
//    val lastTimestampLogs = callcenterLogs.drop("pubTime").distinct()
//      .withColumn("pubTime", to_timestamp(date_trunc("minute", col("dt_handled")).cast("long") + 60))
//      .select("pubTime", "caseId", "dt_offered", "dt_handled", "contact_sk", "dt_offered_service", "dt_offered_queue", "dt_start", "dt_end", "services_sk", "services_name", "ind_forward_service_previous", "area", "subarea")
//
//
//    val preLastTimestampLogs = callcenterLogs.drop("pubTime").distinct()
//      .withColumn("pubTime", date_trunc("minute", col("dt_handled")))
//      .select("pubTime", "caseId", "dt_offered", "dt_handled", "contact_sk", "dt_offered_service", "dt_offered_queue", "dt_start", "dt_end", "services_sk", "services_name", "ind_forward_service_previous", "area", "subarea")
//
//
//    lastTimestampLogs.union(callcenterLogs).union(preLastTimestampLogs).union(firstTimestampLogs)
//      .distinct()
//      .na.fill("NaN", Seq("subArea"))
//      .filter(col("pubTime") > unix_timestamp(lit("2017-04-03 07:30:00.000"), "yyyy-MM-dd HH:mm:ss.SSS").cast(TimestampType))
//      .orderBy("pubTime")
//      .coalesce(1)
//      .write
//      .option("timestampFormat", "yyyy-MM-dd HH:mm:ss.SSS")
//      .csv("src/main/resources/callcenterLogs")
