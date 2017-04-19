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

import play.api.libs.json.{Json, OWrites, Reads}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.selfassessmentapi.models.{PeriodId, SourceId}
import uk.gov.hmrc.selfassessmentapi.models.properties.{FHLProperties, OtherProperties, PropertyType}

case class UKPropertyOtherPeriodAuditData(nino: Nino, sourceId: SourceId = PropertyType.OTHER.toString,
                                          requestPayload: Option[OtherProperties], periodId: Option[PeriodId] = None,
                                          transactionReference: Option[String] = None)

object UKPropertyOtherPeriodAuditData {
  implicit val writes: OWrites[UKPropertyOtherPeriodAuditData] = Json.writes[UKPropertyOtherPeriodAuditData]
  implicit val reads: Reads[UKPropertyOtherPeriodAuditData] = Json.reads[UKPropertyOtherPeriodAuditData]
}

case class UKPropertyFHLPeriodAuditData(nino: Nino, sourceId: SourceId = PropertyType.OTHER.toString,
                                          requestPayload: Option[FHLProperties], periodId: Option[PeriodId] = None,
                                          transactionReference: Option[String] = None)

object UKPropertyFHLPeriodAuditData {
  implicit val writes: OWrites[UKPropertyFHLPeriodAuditData] = Json.writes[UKPropertyFHLPeriodAuditData]
  implicit val reads: Reads[UKPropertyFHLPeriodAuditData] = Json.reads[UKPropertyFHLPeriodAuditData]
}
