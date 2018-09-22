package com.klarrio.dsh.dsc.bootstrap.modelers

import WaitTime5mModeler.printModelEvaluation
import org.apache.spark.ml.evaluation.RegressionEvaluator
import org.apache.spark.ml.{Pipeline, PipelineModel}
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.ml.regression.{LinearRegression, RandomForestRegressionModel, RandomForestRegressor}
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.functions.{col, lead}

/**
  * Creates and trains Random Forest model to predict the amount of queuers in 15 minutes:
  * - Creates basetable
  * - Creates and configures ML pipeline
  * - Fits the pipeline to the training data
  * - Saves the model
  */
object AmtQueuers15mModeler {
  def createModel(data: DataFrame, printOutput: Boolean): Unit = {
    /**
      * BASETABLE CREATION
      */
    val baseTable = data
      .withColumn("label", lead(col("amountOfQueuers"), 15, null).over(Window.orderBy("pubTime")))
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
    val rf = new RandomForestRegressor()

    // Compose the pipeline of the assembler and the random forest
    val pipeline = new Pipeline().setStages(Array(assembler, rf))

    /**
      * PIPELINE MODEL TRAINING AND SAVING
      */
    val pipelineModel = pipeline.fit(baseTable)

    // Save the model in a file so we can use it for scoring
    pipelineModel.write.overwrite().save("../model-scoring-processor/src/main/resources/pipeline_model_amt_queuers_15min")

    if(printOutput) printModelEvaluation(pipelineModel, baseTable)
  }

  def printModelEvaluation(pipelineModel: PipelineModel, data: DataFrame): Unit ={
    // Make predictions.
    val predictions = pipelineModel.transform(data)

    // Select example rows to display.
    predictions.select("prediction", "label", "features").show(5)

    // Select (prediction, true label) and compute test error.
    val evaluator = new RegressionEvaluator()
      .setMetricName("r2")
    val rmse = evaluator.evaluate(predictions)
    println(s"R^2 on test data = $rmse")

    val rfModel = pipelineModel.stages(1).asInstanceOf[RandomForestRegressionModel]
    println(s"Learned regression forest model:\n ${rfModel.toDebugString}")
  }
}
