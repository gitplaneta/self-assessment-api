/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.selfassessmentapi.config


import play.api.libs.json.JsValue
import uk.gov.hmrc.play.audit.AuditExtensions
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.config.LoadAuditingConfig
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.{DataEvent, ExtendedDataEvent}
import uk.gov.hmrc.play.auth.microservice.connectors.AuthConnector
import uk.gov.hmrc.play.config.{AppName, RunMode, ServicesConfig}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.http.hooks.HttpHook
import uk.gov.hmrc.play.http.ws._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object WSHttp extends WSGet with WSPut with WSPost with WSDelete with WSPatch with AppName with RunMode with HttpAuditing {
  override val hooks: Seq[HttpHook] = Seq(AuditingHook)
  override def auditConnector: AuditConnector = MicroserviceAuditConnector
}

object MicroserviceAuditConnector extends AuditConnector with RunMode {
  override lazy val auditingConfig = LoadAuditingConfig(s"$env.auditing")

  def audit(auditType: String, source: Option[String], path: String, auditData: Map[String, String])(implicit hc: HeaderCarrier): Future[AuditResult] = {
    val auditEvent = DataEvent(auditSource = "self-assessment-api",
      auditType = auditType,
      tags = AuditExtensions.auditHeaderCarrier(hc).toAuditTags(transactionName = if(source.isDefined) s"${source.get}-$auditType" else auditType, path = path),
      detail = auditData
    )
    sendEvent(auditEvent)
  }

  def audit2(auditType: String, transactionName: String, path: String, auditData: JsValue)(implicit hc: HeaderCarrier): Future[AuditResult] = {
    val auditEvent = ExtendedDataEvent(auditSource = "self-assessment-api",
      auditType = auditType,
      tags = AuditExtensions.auditHeaderCarrier(hc).toAuditTags(transactionName = transactionName, path = path),
      detail = auditData
    )
    sendEvent(auditEvent)
  }
}

object MTDSAEvent extends Enumeration {
  val periodicUpdateSubmitted,
  taxCalculationTriggered,
  taxCalculationResult
  = Value

  type MTDSAEvent = MTDSAEvent.Value
}

object MicroserviceAuthConnector extends AuthConnector with ServicesConfig {
  override val authBaseUrl = baseUrl("auth")
}
