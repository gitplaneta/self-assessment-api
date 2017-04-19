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

package uk.gov.hmrc.selfassessmentapi.models.audit

import play.api.libs.json.{Json, Reads, Writes}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.selfassessmentapi.models.{SourceId, TaxYear}
import uk.gov.hmrc.selfassessmentapi.models.des.TaxCalculation

case class TaxCalculationAuditData(nino: Nino, taxYear: Option[TaxYear] = None, calculationId: Option[SourceId] = None, responsePayload: Option[TaxCalculation] = None)

object TaxCalculationAuditData {
  implicit val writes: Writes[TaxCalculationAuditData] = Json.writes[TaxCalculationAuditData]
  implicit val reads: Reads[TaxCalculationAuditData] = Json.reads[TaxCalculationAuditData]
}
