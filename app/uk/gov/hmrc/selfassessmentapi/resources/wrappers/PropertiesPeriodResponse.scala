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

package uk.gov.hmrc.selfassessmentapi.resources.wrappers

import org.joda.time.LocalDate
import play.api.Logger
import play.api.libs.json.JsValue
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.http.HttpResponse
import uk.gov.hmrc.selfassessmentapi.models.des.{DesError, DesErrorCode}
import uk.gov.hmrc.selfassessmentapi.models.properties.PropertyType.PropertyType
import uk.gov.hmrc.selfassessmentapi.models.properties.{FHL, Other}
import uk.gov.hmrc.selfassessmentapi.models.{PeriodSummary, des}

case class PropertiesPeriodResponse(underlying: HttpResponse,
                                    from: Option[LocalDate] = None,
                                    to: Option[LocalDate] = None) {

  private val logger: Logger = Logger(classOf[PropertiesPeriodResponse])

  val status: Int = underlying.status

  def json: JsValue = underlying.json

  def getPeriodId: String =
    (for {
      fromDate <- from
      toDate <- to
    } yield s"${fromDate}_$toDate")
      .getOrElse(throw new IllegalStateException("response should contain period from and to dates"))

  def createLocationHeader(nino: Nino, id: PropertyType): String =
    s"/self-assessment/ni/$nino/uk-properties/$id/periods/$getPeriodId"

  def containsOverlappingPeriod: Boolean = {
    json.asOpt[DesError] match {
      case Some(err) => err.code == DesErrorCode.INVALID_PERIOD
      case None =>
        logger.error("The response from DES does not match the expected error format.")
        false
    }
  }

  def mkPeriodIdFHL(prop: FHL.Properties): FHL.Properties =
    prop.copy(id = Some(s"${prop.from}_${prop.to}"))

  def periodFHL: Option[FHL.Properties] =
    json.asOpt[des.properties.FHL.Properties] match {
      case Some(prop) =>
        Some((mkPeriodIdFHL _ compose FHL.Properties.from)(prop))
      case None =>
        logger.error("The response from DES does not match the expected properties period format.")
        None
    }

  def mkPeriodIdOther(prop: Other.Properties): Other.Properties =
    prop.copy(id = Some(s"${prop.from}_${prop.to}"))

  def periodOther: Option[Other.Properties] =
    json.asOpt[des.properties.Other.Properties] match {
      case Some(prop) =>
        Some((mkPeriodIdOther _ compose Other.Properties.from)(prop))
      case None =>
        logger.error("The response from DES does not match the expected properties period format.")
        None
    }

  def allPeriodsFHL: Seq[PeriodSummary] =
    json.asOpt[Seq[des.properties.FHL.Properties]] match {
      case Some(desPeriods) =>
        desPeriods.map((mkPeriodIdFHL _ compose FHL.Properties.from)(_).asSummary)
      case None =>
        logger.error("The response from DES does not match the expected self-employment period format.")
        Seq.empty
    }

  def allPeriodsOther: Seq[PeriodSummary] =
    json.asOpt[Seq[des.properties.Other.Properties]] match {
      case Some(desPeriods) =>
        desPeriods.map((mkPeriodIdOther _ compose Other.Properties.from)(_).asSummary)
      case None =>
        logger.error("The response from DES does not match the expected self-employment period format.")
        Seq.empty
    }

}
