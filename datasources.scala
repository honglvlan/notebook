package org.scalatraining.datasource

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.catalyst.types.{IntegerType, StringType}
import org.apache.spark.sql.catalyst.expressions.{Row, GenericMutableRow}
import org.apache.spark.sql.catalyst.types.{StructField, StructType}
import org.apache.spark.sql.sources.{PrunedScan, BaseRelation, RelationProvider}

object BuildHelper {
  def idBuilder(idx: Int) = idx

  def userNameBuilder(idx: Int) = s"name_$idx"

  def passBuilder(idx: Int) = s"pass_$idx"
}

case class MyPrunedScan(count: Int, slices: Int)(@transient val sqlContext: SQLContext) extends PrunedScan {
  override def sizeInBytes = 20 * count

  override def schema =
    StructType(
      StructField("uid", IntegerType, nullable = false) ::
        StructField("name", StringType, nullable = false) ::
        StructField("password", StringType, nullable = false) ::
        Nil)

  override def buildScan(requiredColumns: Array[String]): RDD[Row] = {
    val builders = requiredColumns.map { column =>
      column match {
        case "uid" => BuildHelper.idBuilder _
        case "name" => BuildHelper.userNameBuilder _
        case "password" => BuildHelper.passBuilder _
        case _ => sys.error(s"Cannot find the column $column")
      }
    }

    // TO DO do something to query the databases
    val row = new GenericMutableRow(requiredColumns.length)
    sqlContext.sparkContext.parallelize(1 to 1000, slices).map { i =>
      var idx = 0
      while (idx < builders.length) {
        row(idx) = builders(idx)(i)
        idx += 1
      }

      row
    }
  }
}

class MyPrunedScanProvider extends RelationProvider {
  override def createRelation(
                               sqlContext: SQLContext,
                               parameters: Map[String, String]): BaseRelation = {
    MyPrunedScan(parameters("count").toInt, parameters("slices").toInt)(sqlContext)
  }
}


