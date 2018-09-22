package com.klarrio.dsh.kpn.datascience.bootstrap.exercise

/**
 * Case classes for the parsed observations
 */

case class AverageHandlingTime(pubTime: Long, avgCallDuration: Double)
case class QueuersAndCallers(pubTime: Long, queueSize: Int, callsAmt: Int, ratioServed: Double)
case class WaitTime(pubTime: Long, queueDuration: Double)


case class Features(pubTime: Long, amountOfCallers: Int, amountOfQueuers: Int, avgQueueDuration: Double, avgCallDuration: Double)