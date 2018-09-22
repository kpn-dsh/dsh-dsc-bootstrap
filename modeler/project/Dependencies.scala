import sbt._

object Version {
  val spark = "2.3.1"
}


object Dependencies {
  val spark = Seq(
    "org.apache.spark" % s"spark-core_2.11" % Version.spark,
    "org.apache.spark" % s"spark-sql_2.11" % Version.spark,
    "org.apache.spark" % s"spark-mllib_2.11" % Version.spark
  )
}