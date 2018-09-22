package com.klarrio.dsh.kpn.datascience.bootstrap.exercise

import java.sql.Timestamp

/**
  * Case class for the parsed observations
  * The structure of the incoming observations will be the same as this case class
  * The timestamps of the incoming observations will need to be parsed into these java.sql.Timestamp
  */
case class CallObservation(
  pubTime: Timestamp,
  caseId: String,
  dt_offered: Timestamp,
  dt_handled: Timestamp,
  dt_offered_service: Timestamp,
  dt_offered_queue: Timestamp,
  dt_start: Timestamp,
  dt_end: Timestamp,
  services_name: String,
  ind_forward_service_previous: String,
  area: String,
  subarea: String
)

