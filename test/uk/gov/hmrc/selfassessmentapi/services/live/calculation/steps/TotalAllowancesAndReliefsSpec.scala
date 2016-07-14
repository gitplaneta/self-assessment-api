/*
 * Copyright 2016 HM Revenue & Customs
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

package uk.gov.hmrc.selfassessmentapi.services.live.calculation.steps

import uk.gov.hmrc.selfassessmentapi.domain.Deductions
import uk.gov.hmrc.selfassessmentapi.{SelfEmploymentSugar, UnitSpec}

class TotalAllowancesAndReliefsSpec extends UnitSpec with SelfEmploymentSugar {

  "run" should {

    "calculate total allowances and reliefs by summing income tax relief and the personal allowance" in {

      val liability = aLiability().copy(personalAllowance = Some(5000), incomeTaxRelief = Some(1400))

      TotalAllowancesAndReliefs.run(SelfAssessment(), liability).deductions shouldBe Some(
        Deductions(incomeTaxRelief = 1400, totalDeductions = 6400)
      )
    }
  }
}