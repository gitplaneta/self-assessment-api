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

package uk.gov.hmrc.selfassessmentapi.resources

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, Request}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.selfassessmentapi.config.{MTDSAEvent, MicroserviceAuditConnector}
import uk.gov.hmrc.selfassessmentapi.models.Errors.Error
import uk.gov.hmrc.selfassessmentapi.models._
import uk.gov.hmrc.selfassessmentapi.models.audit.{UKPropertyFHLPeriodAuditData, UKPropertyOtherPeriodAuditData}
import uk.gov.hmrc.selfassessmentapi.models.properties.PropertyType.PropertyType
import uk.gov.hmrc.selfassessmentapi.models.properties._
import uk.gov.hmrc.selfassessmentapi.services.{FHLPropertiesPeriodService, OtherPropertiesPeriodService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object PropertiesPeriodResource extends BaseController {

  lazy val featureSwitch = FeatureSwitchAction(SourceType.Properties, "periods")
  lazy val auditConnector = MicroserviceAuditConnector

  def createPeriod(nino: Nino, id: PropertyType): Action[JsValue] = featureSwitch.asyncJsonFeatureSwitch { implicit request =>
    validateCreateRequest(id, nino, request) match {
      case Left(errorResult) => Future.successful(handleValidationErrors(errorResult))
      case Right(result) => result.map {
        case Right(periodId) =>
          auditEventForCreatePeriod(nino, id, request, periodId, "transactionReference")
          Created.withHeaders(LOCATION -> s"/self-assessment/ni/$nino/${SourceType.Properties.toString}/${id.toString}/periods/$periodId")
        case Left(error) =>
          if (error.code == ErrorCode.NOT_FOUND.toString) NotFound
          else if (error.path.nonEmpty) // i.e. period already exists
            Conflict.withHeaders(LOCATION -> s"/self-assessment/ni/$nino/${SourceType.Properties.toString}/${id.toString}/periods/${error.path}")
          else Forbidden(Json.toJson(Errors.businessError(error)))
      }
    }
  }

  def updatePeriod(nino: Nino, id: PropertyType, periodId: PeriodId): Action[JsValue] = featureSwitch.asyncJsonFeatureSwitch { request =>
    validateUpdateRequest(id, nino, periodId, request) match {
      case Left(errorResult) => Future.successful(handleValidationErrors(errorResult))
      case Right(result) => result.map {
        case true => NoContent
        case false => NotFound
      }
    }
  }

  def retrievePeriod(nino: Nino, id: PropertyType, periodId: PeriodId): Action[AnyContent] = featureSwitch.asyncFeatureSwitch {
    id match {
      case PropertyType.OTHER => OtherPropertiesPeriodService.retrievePeriod(nino, periodId).map {
        case Some(period) => Ok(Json.toJson(period))
        case None => NotFound
      }
      case PropertyType.FHL => FHLPropertiesPeriodService.retrievePeriod(nino, periodId).map {
        case Some(period) => Ok(Json.toJson(period))
        case None => NotFound
      }
    }
  }

  def retrievePeriods(nino: Nino, id: PropertyType): Action[AnyContent] = featureSwitch.asyncFeatureSwitch {
    id match {
      case PropertyType.OTHER => OtherPropertiesPeriodService.retrieveAllPeriods(nino).map {
        case Some(period) => Ok(Json.toJson(period))
        case None => NotFound
      }
      case PropertyType.FHL => FHLPropertiesPeriodService.retrieveAllPeriods(nino).map {
        case Some(period) => Ok(Json.toJson(period))
        case None => NotFound
      }
    }
  }

  private def validateCreateRequest(id: PropertyType, nino: Nino, request: Request[JsValue]): Either[ErrorResult, Future[Either[Error, PeriodId]]] = id match {
    case PropertyType.OTHER => {
      validate[OtherProperties, Either[Error, PeriodId]](request.body) { period =>
        OtherPropertiesPeriodService.createPeriod(nino, period)
      }
    }
    case PropertyType.FHL => {
      validate[FHLProperties, Either[Error, PeriodId]](request.body) { period =>
        FHLPropertiesPeriodService.createPeriod(nino, period)
      }
    }
  }

  private def validateUpdateRequest(id: PropertyType, nino: Nino, periodId: PeriodId, request: Request[JsValue]): Either[ErrorResult, Future[Boolean]] = id match {
    case PropertyType.OTHER => {
      validate[OtherPeriodicData, Boolean](request.body) { period =>
        OtherPropertiesPeriodService.updatePeriod(nino, periodId, period)
      }
    }
    case PropertyType.FHL => {
      validate[FHLPeriodicData, Boolean](request.body) { period =>
        FHLPropertiesPeriodService.updatePeriod(nino, periodId, period)
      }
    }
  }

  private def auditEventForCreatePeriod(nino: Nino, id: PropertyType, request: Request[JsValue], periodId: PeriodId, transactionReference: String)(implicit hc: HeaderCarrier)= {
    id match {
      case PropertyType.OTHER => {
        val requestPayload = Json.fromJson[OtherProperties](request.body).get
        val auditData = UKPropertyOtherPeriodAuditData(nino = nino, requestPayload = Some(requestPayload),
          periodId = Some(periodId), transactionReference = Some(transactionReference))
        auditConnector.audit2(auditType = MTDSAEvent.periodicUpdateSubmitted.toString,
          transactionName = "create-period-uk-property-other",
          path = s"/ni/$nino/${SourceType.Properties.toString}/${id.toString}/periods",
          auditData = Json.toJson(auditData)
        )
      }
      case PropertyType.FHL => {
        val requestPayload = Json.fromJson[FHLProperties](request.body).get
        val auditData = UKPropertyFHLPeriodAuditData(nino = nino, requestPayload = Some(requestPayload),
          periodId = Some(periodId), transactionReference = Some(transactionReference))
        auditConnector.audit2(auditType = MTDSAEvent.periodicUpdateSubmitted.toString,
          transactionName = "create-period-uk-property-fhl",
          path = s"/ni/$nino/${SourceType.Properties.toString}/${id.toString}/periods",
          auditData = Json.toJson(auditData)
        )
      }
    }
  }

}
