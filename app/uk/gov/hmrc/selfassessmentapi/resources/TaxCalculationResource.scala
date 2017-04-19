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

import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.selfassessmentapi.config.{MTDSAEvent, MicroserviceAuditConnector}
import uk.gov.hmrc.selfassessmentapi.connectors.TaxCalculationConnector
import uk.gov.hmrc.selfassessmentapi.models.Errors.Error
import uk.gov.hmrc.selfassessmentapi.models.audit.TaxCalculationAuditData
import uk.gov.hmrc.selfassessmentapi.models.calculation.CalculationRequest
import uk.gov.hmrc.selfassessmentapi.models.{SourceId, SourceType}
import uk.gov.hmrc.selfassessmentapi.resources.wrappers.TaxCalculationResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object TaxCalculationResource extends BaseController {

  private lazy val featureSwitch = FeatureSwitchAction(SourceType.Calculation)
  private val logger = Logger(TaxCalculationResource.getClass)
  private val connector = TaxCalculationConnector
  private val auditConnector = MicroserviceAuditConnector

  private val cannedEtaResponse =
    s"""
       |{
       |  "etaSeconds": 5
       |}
     """.stripMargin

  def requestCalculation(nino: Nino): Action[JsValue] = featureSwitch.asyncJsonFeatureSwitch { implicit request =>
    var taxCalculationAuditData = TaxCalculationAuditData(nino)
    var taxYear = ""
    validate[CalculationRequest, TaxCalculationResponse](request.body) { req =>
      taxYear = req.taxYear.toString
      taxCalculationAuditData = taxCalculationAuditData.copy(taxYear = Some(req.taxYear))
      connector.requestCalculation(nino, req.taxYear)
    } match {
      case Left(errorResult) => Future.successful(handleValidationErrors(errorResult))
      case Right(result) => result.map { response =>
        response.status match {
          case 202 =>
            auditConnector.audit2(auditType = MTDSAEvent.taxCalculationTriggered.toString,
              transactionName = "trigger-tax-calculation",
              path = s"/ni/$nino/calculations",
              auditData = Json.toJson(taxCalculationAuditData.copy(calculationId = response.calcId))
            )
            Accepted(Json.parse(cannedEtaResponse))
              .withHeaders(LOCATION -> response.calcId.map(id => s"/self-assessment/ni/$nino/calculations/$id").getOrElse(""))
          case 400 => BadRequest(Error.from(response.json))
          case _ => unhandledResponse(response.status, logger)
        }
      }
    }
  }

  def retrieveCalculation(nino: Nino, calcId: SourceId): Action[AnyContent] = featureSwitch.asyncFeatureSwitch { implicit request =>
    var taxCalculationAuditData = TaxCalculationAuditData(nino, taxYear = None, calculationId = Some(calcId))
    connector.retrieveCalculation(nino, calcId).map { response =>
      response.status match {
        case 200 =>
          auditConnector.audit2(auditType = MTDSAEvent.taxCalculationResult.toString,
            transactionName = "get-tax-calculation",
            path = s"/ni/$nino/calculations/$calcId",
            auditData = Json.toJson(taxCalculationAuditData.copy(responsePayload = response.calculation))
          )
          Ok(Json.toJson(response.calculation))
        case 204 => NoContent
        case 400 if response.isInvalidCalcId => NotFound
        case 400 => BadRequest(Error.from(response.json))
        case 404 => NotFound
        case _ => unhandledResponse(response.status, logger)
      }
    }
  }

}
