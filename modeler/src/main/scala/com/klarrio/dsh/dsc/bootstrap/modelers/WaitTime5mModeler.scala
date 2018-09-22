package com.klarrio.dsh.dsc.bootstrap.modelers

import org.apache.spark.ml.{Pipeline, PipelineModel}
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.ml.regression.{LinearRegression, LinearRegressionModel}
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.functions.{col, lead}

/**
  * Creates and trains Linear Regression model to predict wait time in 5 minutes:
  * - Creates basetable
  * - Creates and configures ML pipeline
  * - Fits the pipeline to the training data
  * - Saves the model
  */
object WaitTime5mModeler {
  def createModel(data: DataFrame, printOutput: Boolean): Unit = {
    /**
      * BASETABLE CREATION
      */
    val baseTable = data
      .withColumn("label", lead(col("avgQueueDuration"), 5, null).over(Window.orderBy("pubTime")))
      .na.drop("any")
      .drop("pubTime")

    baseTable.show()

    /**
      * PIPELINE CREATION
      */
    // The names of the feature columns
    val featureArray = Array("amountOfCallers", "amountOfQueuers", "avgQueueDuration", "avgCallDuration")

    // Assemble all the feature columns into one column (necessary for model training)
    val assembler = new VectorAssembler()
      .setInputCols(featureArray)
      .setOutputCol("features")

    // Configure the model we want to train
    val lr = new LinearRegression()

    // Compose the pipeline of the assembler and the linear regression
    val pipeline = new Pipeline().setStages(Array(assembler, lr))

    /**
      * PIPELINE MODEL TRAINING AND SAVING
      */
    val pipelineModel = pipeline.fit(baseTable)

    // Save the model in a file so we can use it for scoring
    pipelineModel.write.overwrite().save("../model-scoring-processor/src/main/resources/pipeline_model_wait_time_5min")

    if (printOutput) printModelEvaluation(pipelineModel)
  }

  private def printModelEvaluation(pipelineModel: PipelineModel): Unit = {
    val lrModel: LinearRegressionModel = pipelineModel.stages(1).asInstanceOf[LinearRegressionModel]
    // Print the coefficients and intercept for linear regression
    println(s"Coefficients: ${lrModel.coefficients} Intercept: ${lrModel.intercept}")

    // Summarize the model over the training set and print out some metrics
    val trainingSummary = lrModel.summary
    println(s"numIterations: ${trainingSummary.totalIterations}")
    println(s"objectiveHistory: [${trainingSummary.objectiveHistory.mkString(",")}]")
    trainingSummary.residuals.show()
    println(s"RMSE: ${trainingSummary.rootMeanSquaredError}")
    println(s"r2: ${trainingSummary.r2}")
  }
}
