package com.klarrio.dsh.dsc.bootstrap.modelers

import AmtQueuers15mModeler.printModelEvaluation
import org.apache.spark.ml.{Pipeline, PipelineModel}
import org.apache.spark.ml.classification.{RandomForestClassificationModel, RandomForestClassifier}
import org.apache.spark.ml.evaluation.{BinaryClassificationEvaluator, MulticlassClassificationEvaluator}
import org.apache.spark.ml.feature.{IndexToString, StringIndexer, VectorAssembler}
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.functions._

/**
  * Creates and trains Random Forest model to predict the amount of callers in 15 minutes:
  * - Creates basetable
  * - Creates and configures ML pipeline
  * - Fits the pipeline to the training data
  * - Saves the model
  */
object AmtCallers15mModeler {
  def createModel(data: DataFrame, printOutput: Boolean): Unit = {
    /**
      * BASETABLE CREATION
      */
    val baseTable = data
      .withColumn("callers15m", lead(col("amountOfCallers"), 15, 0).over(Window.orderBy("pubTime")))
      .withColumn("label", when(col("callers15m") > col("amountOfCallers"), "rising").otherwise("falling"))
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

    // Index the label as a categorical column
    val labelIndexer = new StringIndexer()
      .setInputCol("label")
      .setOutputCol("indexedLabel")
      .fit(baseTable)

    // Configure the model we want to train
    val rf = new RandomForestClassifier()
      .setLabelCol("indexedLabel")

    // Convert indexed labels back to original labels.
    val labelConverter = new IndexToString()
      .setInputCol("prediction")
      .setOutputCol("predictedLabel")
      .setLabels(labelIndexer.labels)

    // Compose the pipeline of the assembler and the random forest
    val pipeline = new Pipeline().setStages(Array(assembler, labelIndexer, rf, labelConverter))


    /**
      * PIPELINE MODEL TRAINING AND SAVING
      */
    val pipelineModel = pipeline.fit(baseTable)

    // Save the model in a file so we can use it for scoring
    pipelineModel.write.overwrite().save("../model-scoring-processor/src/main/resources/pipeline_model_amt_callers_15min")

    if (printOutput) printModelEvaluation(pipelineModel, baseTable)
  }

  def printModelEvaluation(pipelineModel: PipelineModel, data: DataFrame): Unit = {
    // Make predictions.
    val predictions = pipelineModel.transform(data)

    // Select example rows to display.
    predictions.select("predictedLabel", "label", "features").show(5)

    // Select (prediction, true label) and compute test error.
    val evaluator = new MulticlassClassificationEvaluator()
      .setLabelCol("indexedLabel")
      .setPredictionCol("prediction")
      .setMetricName("accuracy")
    val accuracy = evaluator.evaluate(predictions)
    println(s"Test Error = ${(1.0 - accuracy)}")

    val rfModel = pipelineModel.stages(2).asInstanceOf[RandomForestClassificationModel]
    println(s"Learned classification forest model:\n ${rfModel.toDebugString}")
  }
}
