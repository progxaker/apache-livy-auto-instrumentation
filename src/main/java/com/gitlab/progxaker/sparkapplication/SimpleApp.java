package com.gitlab.progxaker.sparkapplication;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.SparkSession;

public class SimpleApp {
  public static void main(String[] args) {
    String logFile = "/tmp/spark/README.md";
    SparkSession spark = SparkSession.builder().appName("Simple Application").getOrCreate();
    Dataset<String> logData = spark.read().textFile(logFile).cache();

    long length = logData.count();

    System.out.println("Lines: " + length);

    spark.stop();
  }
}
