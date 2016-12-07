package uk.gov.hmrc.selfassessmentapi.resources

import org.joda.time.LocalDate
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.selfassessmentapi.resources.models.selfemployment._
import uk.gov.hmrc.selfassessmentapi.resources.models.{selfemployment, _}
import uk.gov.hmrc.support.BaseFunctionalSpec

class SelfEmploymentsResourceSpec extends BaseFunctionalSpec {

  val selfEmployment = SelfEmployment(
    accountingPeriod = AccountingPeriod(LocalDate.parse("2017-04-01"), LocalDate.parse("2018-04-01")),
    accountingType = AccountingType.CASH,
    commencementDate = LocalDate.now.minusDays(1))

  implicit def selfEmployment2Json(selfEmployment: SelfEmployment): JsValue = Json.toJson(selfEmployment)

  "create" should {
    "return code 201 when creating a valid a self-employment source of income" in {
      given()
        .userIsAuthorisedForTheResource(nino)
        .when()
        .post(selfEmployment).to(s"/ni/$nino/self-employments")
        .thenAssertThat()
        .statusIs(201)
        .responseContainsHeader("Location", s"/self-assessment/ni/$nino/self-employments/\\w+".r)
    }

    "return code 400 (INVALID_REQUEST) when attempting to create a self-employment with an invalid dates in the accountingPeriod" in {
      val selfEmployment =
        s"""
           |{
           |  "accountingPeriod": {
           |    "start": "01-01-2017",
           |    "end": "02-01-2017"
           |  },
           |  "accountingType": "CASH",
           |  "commencementDate": "${LocalDate.now.minusDays(1)}"
           |}
         """.stripMargin

      val expectedBody =
        s"""
           |{
           |  "code": "INVALID_REQUEST",
           |  "message": "Invalid request",
           |  "errors": [
           |    {
           |      "code": "INVALID_DATE",
           |      "path": "/accountingPeriod/start",
           |      "message": "please provide a date in ISO format (i.e. YYYY-MM-DD)"
           |    },
           |    {
           |      "code": "INVALID_DATE",
           |      "path": "/accountingPeriod/end",
           |      "message": "please provide a date in ISO format (i.e. YYYY-MM-DD)"
           |    }
           |  ]
           |}
         """.stripMargin

      given()
        .userIsAuthorisedForTheResource(nino)
        .when()
        .post(Json.parse(selfEmployment)).to(s"/ni/$nino/self-employments")
        .thenAssertThat()
        .statusIs(400)
        .contentTypeIsJson()
        .bodyIsLike(expectedBody)
    }

    "return code 400 (INVALID_VALUE) when attempting to create a self-employment with an invalid accounting type" in {
      val selfEmployment =
        s"""
           |{
           |  "accountingPeriod": {
           |    "start": "2017-04-06",
           |    "end": "2018-04-06"
           |  },
           |  "accountingType": "NOOOOO",
           |  "commencementDate": "2016-01-01"
           |}
         """.stripMargin

      val expectedBody =
        s"""
           |{
           |  "code": "INVALID_REQUEST",
           |  "message": "Invalid request",
           |  "errors": [
           |    {
           |      "code": "INVALID_VALUE",
           |      "path": "/accountingType",
           |      "message": "AccountingType should be either CASH or ACCRUAL"
           |    }
           |  ]
           |}
         """.stripMargin

      given()
        .userIsAuthorisedForTheResource(nino)
        .when()
        .post(Json.parse(selfEmployment)).to(s"/ni/$nino/self-employments")
        .thenAssertThat()
        .statusIs(400)
        .contentTypeIsJson()
        .bodyIsLike(expectedBody)
    }
  }

  "update" should {
    "return code 204 when successfully updating a self-employment resource" in {
      val selfEmployment2 = Json.toJson(SelfEmployment(
        accountingPeriod = AccountingPeriod(
          start = LocalDate.parse("2017-04-01"),
          end = LocalDate.parse("2017-04-02")),
        accountingType = AccountingType.ACCRUAL,
        commencementDate = LocalDate.parse("2016-01-01")
      ))

      given()
        .userIsAuthorisedForTheResource(nino)
        .when()
        .post(selfEmployment).to(s"/ni/$nino/self-employments")
        .thenAssertThat()
        .statusIs(201)
        .when()
        .put(selfEmployment2).at("%sourceLocation%")
        .thenAssertThat()
        .statusIs(204)
        .when()
        .get("%sourceLocation%")
        .thenAssertThat()
        .bodyIsLike(Json.toJson(selfEmployment2).toString)
    }

    "return code 404 when attempting to update a non-existent self-employment resource" in {
      val selfEmployment2 = Json.toJson(selfEmployment.copy(commencementDate = LocalDate.now.minusDays(2)))

      given()
        .userIsAuthorisedForTheResource(nino)
        .when()
        .post(selfEmployment).to(s"/ni/$nino/self-employments")
        .thenAssertThat()
        .statusIs(201)
        .when()
        .put(selfEmployment2).at(s"/ni/$nino/self-employments/invalidSourceId")
        .thenAssertThat()
        .statusIs(404)
    }

    "return code 400 (INVALID_DATE) when attempting to update a self-employment with a non-ISO (i.e. YYYY-MM-DD) date" in {
      val invalidDateJson =
        s"""
           |{
           |  "accountingPeriod": {
           |    "start": "2017-04-06",
           |    "end": "2018-04-06"
           |  },
           |  "accountingType": "CASH",
           |  "commencementDate": "22-10-2016"
           |}
         """.stripMargin

      val expectedBody =
        s"""
           |{
           |  "code": "INVALID_REQUEST",
           |  "message": "Invalid request",
           |  "errors": [
           |    {
           |      "code": "INVALID_DATE",
           |      "path": "/commencementDate",
           |      "message": "please provide a date in ISO format (i.e. YYYY-MM-DD)"
           |    }
           |  ]
           |}
         """.stripMargin

      given()
        .userIsAuthorisedForTheResource(nino)
        .when()
        .post(selfEmployment).to(s"/ni/$nino/self-employments")
        .thenAssertThat()
        .statusIs(201)
        .when()
        .put(Json.parse(invalidDateJson)).at(s"%sourceLocation%")
        .thenAssertThat()
        .statusIs(400)
        .contentTypeIsJson()
        .bodyIsLike(expectedBody)
    }

    "return code 400 (INVALID_DATE) when attempting to create a self-employment with an empty date" in {
      val invalidDateJson =
        s"""
           |{
           |  "accountingPeriod": {
           |    "start": "2017-04-06",
           |    "end": "2018-04-06"
           |  },
           |  "accountingType": "CASH",
           |  "commencementDate": ""
           |}
         """.stripMargin

      val expectedBody =
        s"""
           |{
           |  "code": "INVALID_REQUEST",
           |  "message": "Invalid request",
           |  "errors": [
           |    {
           |      "code": "INVALID_DATE",
           |      "path": "/commencementDate",
           |      "message": "please provide a date in ISO format (i.e. YYYY-MM-DD)"
           |    }
           |  ]
           |}
         """.stripMargin

      given()
        .userIsAuthorisedForTheResource(nino)
        .when()
        .post(selfEmployment).to(s"/ni/$nino/self-employments")
        .thenAssertThat()
        .statusIs(201)
        .when()
        .put(Json.parse(invalidDateJson)).at(s"%sourceLocation%")
        .thenAssertThat()
        .statusIs(400)
        .contentTypeIsJson()
        .bodyIsLike(expectedBody)
    }

    "return code 400 (MANDATORY_FIELD_MISSING) when attempting to update a self-employment with an empty body" in {
      val expectedBody =
        s"""
           |{
           |  "code": "INVALID_REQUEST",
           |  "message": "Invalid request",
           |  "errors": [
           |    {
           |      "code": "MANDATORY_FIELD_MISSING",
           |      "path": "/accountingPeriod",
           |      "message": "a mandatory field is missing"
           |    },
           |    {
           |      "code": "MANDATORY_FIELD_MISSING",
           |      "path": "/accountingType",
           |      "message": "a mandatory field is missing"
           |    },
           |    {
           |      "code": "MANDATORY_FIELD_MISSING",
           |      "path": "/commencementDate",
           |      "message": "a mandatory field is missing"
           |    }
           |  ]
           |}
         """.stripMargin

      given()
        .userIsAuthorisedForTheResource(nino)
        .when()
        .post(selfEmployment).to(s"/ni/$nino/self-employments")
        .thenAssertThat()
        .statusIs(201)
        .when()
        .put(Json.parse("{}")).at(s"%sourceLocation%")
        .thenAssertThat()
        .statusIs(400)
        .contentTypeIsJson()
        .bodyIsLike(expectedBody)
    }

    "return code 400 (INVALID_VALUE) when attempting to update a self-employment with an invalid accounting type" in {
      val updatedSelfEmployment =
        s"""
           |{
           |  "accountingPeriod": {
           |    "start": "2017-04-06",
           |    "end": "2018-04-06"
           |  },
           |  "accountingType": "NOOOOO",
           |  "commencementDate": "2016-01-01"
           |}
         """.stripMargin

      val expectedBody =
        s"""
           |{
           |  "code": "INVALID_REQUEST",
           |  "message": "Invalid request",
           |  "errors": [
           |    {
           |      "code": "INVALID_VALUE",
           |      "path": "/accountingType",
           |      "message": "AccountingType should be either CASH or ACCRUAL"
           |    }
           |  ]
           |}
         """.stripMargin

      given()
        .userIsAuthorisedForTheResource(nino)
        .when()
        .post(selfEmployment).to(s"/ni/$nino/self-employments")
        .thenAssertThat()
        .statusIs(201)
        .when()
        .put(Json.parse(updatedSelfEmployment)).at(s"%sourceLocation%")
        .thenAssertThat()
        .statusIs(400)
        .contentTypeIsJson()
        .bodyIsLike(expectedBody)
    }
  }

  "retrieve" should {
    "return code 200 when retrieving a self-employment resource that exists" in {
      val expectedBody =
        s"""
           |{
           |  "accountingPeriod": {
           |    "start": "2017-04-01",
           |    "end": "2018-04-01"
           |  },
           |  "accountingType": "CASH",
           |  "commencementDate": "${LocalDate.now.minusDays(1)}"
           |}
       """.stripMargin

      given()
        .userIsAuthorisedForTheResource(nino)
        .when()
        .post(selfEmployment).to(s"/ni/$nino/self-employments")
        .thenAssertThat()
        .statusIs(201)
        .when()
        .get(s"%sourceLocation%")
        .thenAssertThat()
        .statusIs(200)
        .contentTypeIsJson()
        .bodyIsLike(expectedBody)
    }

    "return code 404 when retrieving a self-employment resource that does not exist" in {
      given()
        .userIsAuthorisedForTheResource(nino)
        .when()
        .get(s"/ni/$nino/self-employments/invalidSourceId")
        .thenAssertThat()
        .statusIs(404)
    }
  }

  "retrieveAll" should {
    "return code 200 when retrieving self-employments that exist" in {

      val expectedBody =
        s"""
           |[
           |  {
           |    "accountingPeriod": {
           |      "start": "2017-04-01",
           |      "end": "2018-04-01"
           |    },
           |    "accountingType": "CASH",
           |    "commencementDate": "${LocalDate.now.minusDays(1)}"
           |  }
           |]
         """.stripMargin

      given()
        .userIsAuthorisedForTheResource(nino)
        .when()
        .post(selfEmployment).to(s"/ni/$nino/self-employments")
        .thenAssertThat()
        .statusIs(201)
        .when()
        .get(s"/ni/$nino/self-employments")
        .thenAssertThat()
        .statusIs(200)
        .contentTypeIsJson()
        .bodyIsLike(expectedBody)
    }

    "return code 200 with an empty body when the user has no self-employment sources" in {
      given()
        .userIsAuthorisedForTheResource(nino)
        .when()
        .get(s"/ni/$nino/self-employments")
        .thenAssertThat()
        .statusIs(200)
        .jsonBodyIsEmptyArray
    }
  }

  "updateAnnualSummary" should {
    "return code 204 when updating an annual summary for a valid self-employment source" in {
      val annualSummaries = Json.toJson(AnnualSummary(Some(Allowances.example), Some(Adjustments.example)))

      given()
        .userIsAuthorisedForTheResource(nino)
        .when()
        .post(selfEmployment).to(s"/ni/$nino/self-employments")
        .thenAssertThat()
        .statusIs(201)
        .when()
        .put(annualSummaries).at(s"%sourceLocation%/$taxYear")
        .thenAssertThat()
        .statusIs(204)
    }

    "return code 404 when updating an annual summary for an invalid self-employment source" in {
      val annualSummaries = Json.toJson(selfemployment.AnnualSummary(Some(Allowances.example), Some(Adjustments.example)))

      given()
        .userIsAuthorisedForTheResource(nino)
        .when()
        .put(annualSummaries).at(s"/ni/$nino/self-employments/sillysource/$taxYear")
        .thenAssertThat()
        .statusIs(404)
    }

    "return code 400 when updating an annual summary providing an invalid adjustment & allowance" in {
      val invalidAdjustment = Adjustments.example.copy(includedNonTaxableProfits = Some(-100), overlapReliefUsed = Some(-100), goodsAndServicesOwnUse = Some(-50))
      val invalidAllowances = Allowances.example.copy(capitalAllowanceMainPool = Some(-100))
      val annualSummaries = Json.toJson(selfemployment.AnnualSummary(Some(invalidAllowances), Some(invalidAdjustment)))

      val expectedBody =
        s"""
           |{
           |  "code": "INVALID_REQUEST",
           |  "message": "Invalid request",
           |  "errors": [
           |    {
           |      "code": "INVALID_MONETARY_AMOUNT",
           |      "path": "/adjustments/includedNonTaxableProfits",
           |      "message": "amounts should be positive numbers with up to 2 decimal places"
           |    },
           |    {
           |      "code": "INVALID_MONETARY_AMOUNT",
           |      "path": "/adjustments/overlapReliefUsed",
           |      "message": "amounts should be positive numbers with up to 2 decimal places"
           |    },
           |    {
           |      "code": "INVALID_MONETARY_AMOUNT",
           |      "path": "/adjustments/goodsAndServicesOwnUse",
           |      "message": "amounts should be positive numbers with up to 2 decimal places"
           |    },
           |    {
           |      "code": "INVALID_MONETARY_AMOUNT",
           |      "path": "/allowances/capitalAllowanceMainPool",
           |      "message": "amounts should be positive numbers with up to 2 decimal places"
           |    }
           |  ]
           |}
         """.stripMargin

      given()
        .userIsAuthorisedForTheResource(nino)
        .when()
        .post(selfEmployment).to(s"/ni/$nino/self-employments")
        .thenAssertThat()
        .statusIs(201)
        .when()
        .put(annualSummaries).at(s"%sourceLocation%/$taxYear")
        .thenAssertThat()
        .statusIs(400)
        .contentTypeIsJson()
        .bodyIsLike(expectedBody)
    }
  }

  "retrieveAnnualSummary" should {
    "return code 200 when retrieving an annual summary that exists" in {
      val annualSummaries = Json.toJson(selfemployment.AnnualSummary(Some(Allowances.example), Some(Adjustments.example)))
      val expectedJson = Json.toJson(annualSummaries).toString()

      given()
        .userIsAuthorisedForTheResource(nino)
        .when()
        .post(selfEmployment).to(s"/ni/$nino/self-employments")
        .thenAssertThat()
        .statusIs(201)
        .when()
        .put(annualSummaries).at(s"%sourceLocation%/$taxYear")
        .thenAssertThat()
        .statusIs(204)
        .when()
        .get(s"%sourceLocation%/$taxYear")
        .thenAssertThat()
        .statusIs(200)
        .contentTypeIsJson()
        .bodyIsLike(expectedJson)
    }

    "return code 200 containing and empty object when retrieving a non-existent annual summary" in {

      given()
        .userIsAuthorisedForTheResource(nino)
        .when()
        .post(selfEmployment).to(s"/ni/$nino/self-employments")
        .thenAssertThat()
        .statusIs(201)
        .when()
        .get(s"%sourceLocation%/$taxYear")
        .thenAssertThat()
        .statusIs(200)
        .contentTypeIsJson()
        .jsonBodyIsEmptyObject
    }
  }

  "createPeriod" should {
    "return code 201 containing a location header when creating a period" in {

      val period = s"""{
        |  "from": "2017-04-01",
        |  "to": "2017-07-04",
        |  "incomes": {
        |    "turnover": { "amount": 100.25 },
        |    "other": { "amount": 100.25 }
        |  },
        |  "expenses": {
        |    "costOfGoodsBought": {
        |      "amount": 100.25,
        |      "disallowableAmount": 50.25
        |    },
        |    "cisPaymentsToSubcontractors": {
        |      "amount": 100.25,
        |      "disallowableAmount": 50.25
        |    },
        |    "staffCosts": {
        |      "amount": 100.25,
        |      "disallowableAmount": 50.25
        |    },
        |    "travelCosts": {
        |      "amount": 100.25,
        |      "disallowableAmount": 50.25
        |    },
        |    "premisesRunningCosts": {
        |      "amount": 100.25,
        |      "disallowableAmount": 50.25
        |    },
        |    "maintenanceCosts": {
        |      "amount": 100.25,
        |      "disallowableAmount": 50.25
        |    },
        |    "adminCosts": {
        |      "amount": 100.25,
        |      "disallowableAmount": 50.25
        |    },
        |    "advertisingCosts": {
        |      "amount": 100.25,
        |      "disallowableAmount": 50.25
        |    },
        |    "interest": {
        |      "amount": 100.25,
        |      "disallowableAmount": 50.25
        |    },
        |    "financialCharges": {
        |      "amount": 100.25,
        |      "disallowableAmount": 50.25
        |    },
        |    "badDebt": {
        |      "amount": 100.25,
        |      "disallowableAmount": 50.25
        |    },
        |    "professionalFees": {
        |      "amount": 100.25,
        |      "disallowableAmount": 50.25
        |    },
        |    "depreciation": {
        |      "amount": 100.25,
        |      "disallowableAmount": 100.25
        |    },
        |    "other": {
        |      "amount": 100.25,
        |      "disallowableAmount": 50.25
        |    }
        |  }
        |}""".stripMargin

      given()
        .userIsAuthorisedForTheResource(nino)
        .when()
        .post(selfEmployment).to(s"/ni/$nino/self-employments")
        .thenAssertThat()
        .statusIs(201)
        .when()
        .post(Json.parse(period)).to(s"%sourceLocation%/periods")
        .thenAssertThat()
        .statusIs(201)
        .responseContainsHeader("Location", s"/self-assessment/ni/$nino/self-employments/\\w+/periods/\\w+".r)
    }

    "return code 400 when attempting to create a period with the 'from' and 'to' dates are in the incorrect order" in {
      val periodOne = s"""{
                      |  "from": "2017-04-01",
                      |  "to": "2017-03-31"
                      |}""".stripMargin

      val expectedBody =
        s"""
           |{
           |  "code": "INVALID_REQUEST",
           |  "message": "Invalid request",
           |  "errors": [
           |    {
           |      "code": "INVALID_PERIOD",
           |      "message": "the period 'from' date should come before the 'to' date"
           |    }
           |  ]
           |}
         """.stripMargin

      given()
        .userIsAuthorisedForTheResource(nino)
        .when()
        .post(selfEmployment).to(s"/ni/$nino/self-employments")
        .thenAssertThat()
        .statusIs(201)
        .when()
        .post(Json.parse(periodOne)).to(s"%sourceLocation%/periods")
        .thenAssertThat()
        .statusIs(400)
        .contentTypeIsJson()
        .bodyIsLike(expectedBody)
    }

    "return code 403 when attempting to create a period whose date range overlaps" in {
      val periodOne = Json.parse(s"""{
                      |  "from": "2017-04-01",
                      |  "to": "2017-07-04"
                      |}""".stripMargin)

      val periodTwo = Json.parse(s"""{
                         |  "from": "2017-07-05",
                         |  "to": "2017-08-04"
                         |}""".stripMargin)

      val overlappingPeriod = Json.parse(s"""{
                         |  "from": "2017-08-04",
                         |  "to": "2017-09-04"
                         |}""".stripMargin)

      val expectedBody =
        s"""
           |{
           |  "code": "BUSINESS_ERROR",
           |  "message": "Business validation error",
           |  "errors": [
           |    {
           |      "code": "OVERLAPPING_PERIOD",
           |      "message": "Periods should not overlap"
           |    }
           |  ]
           |}
         """.stripMargin

      given()
        .userIsAuthorisedForTheResource(nino)
        .when()
        .post(selfEmployment).to(s"/ni/$nino/self-employments")
        .thenAssertThat()
        .statusIs(201)
        .when()
        .post(periodOne).to(s"%sourceLocation%/periods")
        .thenAssertThat()
        .statusIs(201)
        .when()
        .post(periodTwo).to(s"%sourceLocation%/periods")
        .thenAssertThat()
        .statusIs(201)
        .when()
        .post(overlappingPeriod).to(s"%sourceLocation%/periods")
        .thenAssertThat()
        .statusIs(403)
        .bodyIsLike(expectedBody)

    }

    "return code 403 when attempting to create a period that would leave a gap between the latest period and the one provided" in {
      val periodOne = Json.parse(s"""{
                                    |  "from": "2017-04-01",
                                    |  "to": "2017-07-04"
                                    |}""".stripMargin)

      val periodTwoWithGap = Json.parse(s"""{
                                    |  "from": "2017-07-06",
                                    |  "to": "2017-08-04"
                                    |}""".stripMargin)

      val expectedBody =
        s"""
           |{
           |  "code": "BUSINESS_ERROR",
           |  "message": "Business validation error",
           |  "errors": [
           |    {
           |      "code": "GAP_PERIOD",
           |      "message": "Periods should not contain gaps between each other"
           |    }
           |  ]
           |}
         """.stripMargin

      given()
        .userIsAuthorisedForTheResource(nino)
        .when()
        .post(selfEmployment).to(s"/ni/$nino/self-employments")
        .thenAssertThat()
        .statusIs(201)
        .when()
        .post(periodOne).to(s"%sourceLocation%/periods")
        .thenAssertThat()
        .statusIs(201)
        .when()
        .post(periodTwoWithGap).to(s"%sourceLocation%/periods")
        .thenAssertThat()
        .statusIs(403)
        .bodyIsLike(expectedBody)
    }
  }

  "updatePeriod" should {
    "return code 204 when updating a period that exists" in {
      val period = s"""{
                      |  "from": "2017-04-01",
                      |  "to": "2017-07-04",
                      |  "incomes": {
                      |    "turnover": { "amount": 100.25 },
                      |    "other": { "amount": 100.25 }
                      |  },
                      |  "expenses": {
                      |    "costOfGoodsBought": {
                      |      "amount": 100.25,
                      |      "disallowableAmount": 50.25
                      |    },
                      |    "cisPaymentsToSubcontractors": {
                      |      "amount": 100.25,
                      |      "disallowableAmount": 50.25
                      |    }
                      |  }
                      |}""".stripMargin

      val updatePeriod = s"""{
                      |  "incomes": {
                      |    "turnover": { "amount": 200.25 },
                      |    "other": { "amount": 100.25 }
                      |  },
                      |  "expenses": {
                      |    "costOfGoodsBought": {
                      |      "amount": 200.25,
                      |      "disallowableAmount": 50.25
                      |    },
                      |    "cisPaymentsToSubcontractors": {
                      |      "amount": 100.25,
                      |      "disallowableAmount": 50.25
                      |    }
                      |  }
                      |}""".stripMargin

      val updatedPeriod = s"""{
                            |  "from": "2017-04-01",
                            |  "to": "2017-07-04",
                            |  "incomes": {
                            |    "turnover": { "amount": 200.25 },
                            |    "other": { "amount": 100.25 }
                            |  },
                            |  "expenses": {
                            |    "costOfGoodsBought": {
                            |      "amount": 200.25,
                            |      "disallowableAmount": 50.25
                            |    },
                            |    "cisPaymentsToSubcontractors": {
                            |      "amount": 100.25,
                            |      "disallowableAmount": 50.25
                            |    }
                            |  }
                            |}""".stripMargin

      given()
        .userIsAuthorisedForTheResource(nino)
        .when()
        .post(selfEmployment).to(s"/ni/$nino/self-employments")
        .thenAssertThat()
        .statusIs(201)
        .when()
        .post(Json.parse(period)).to(s"%sourceLocation%/periods")
        .thenAssertThat()
        .statusIs(201)
        .when()
        .put(Json.parse(updatePeriod)).at(s"%periodLocation%")
        .thenAssertThat()
        .statusIs(204)
        .when()
        .get(s"%periodLocation%")
        .thenAssertThat()
        .bodyIsLike(updatedPeriod)
    }

    "return code 404 when attempting to update a non-existent period" in {
      val period = s"""{
                      |  "incomes": {
                      |    "turnover": { "amount": 100.25 },
                      |    "other": { "amount": 100.25 }
                      |  },
                      |  "expenses": {
                      |    "costOfGoodsBought": {
                      |      "amount": 100.25,
                      |      "disallowableAmount": 50.25
                      |    },
                      |    "cisPaymentsToSubcontractors": {
                      |      "amount": 100.25,
                      |      "disallowableAmount": 50.25
                      |    }
                      |  }
                      |}""".stripMargin

      given()
        .userIsAuthorisedForTheResource(nino)
        .when()
        .post(selfEmployment).to(s"/ni/$nino/self-employments")
        .thenAssertThat()
        .statusIs(201)
        .when()
        .put(Json.toJson(period)).at(s"%sourceLocation%/periods/thereisnoperiodhere")
        .thenAssertThat()
        .statusIs(404)
    }
  }

  "retrievePeriod" should {
    "return code 200 when retrieving a period that exists" in {
      val period = s"""{
                      |  "from": "2017-04-01",
                      |  "to": "2017-07-04",
                      |  "incomes": {
                      |    "turnover": { "amount": 100.25 },
                      |    "other": { "amount": 100.25 }
                      |  },
                      |  "expenses": {
                      |    "costOfGoodsBought": {
                      |      "amount": 100.25,
                      |      "disallowableAmount": 50.25
                      |    },
                      |    "cisPaymentsToSubcontractors": {
                      |      "amount": 100.25,
                      |      "disallowableAmount": 50.25
                      |    }
                      |  }
                      |}""".stripMargin

      given()
        .userIsAuthorisedForTheResource(nino)
        .when()
        .post(selfEmployment).to(s"/ni/$nino/self-employments")
        .thenAssertThat()
        .statusIs(201)
        .when()
        .post(Json.parse(period)).to(s"%sourceLocation%/periods")
        .thenAssertThat()
        .statusIs(201)
        .when()
        .get(s"%periodLocation%")
        .thenAssertThat()
        .statusIs(200)
        .contentTypeIsJson()
        .bodyIsLike(period)
        .bodyDoesNotHavePath[PeriodId]("periodId")
    }

    "return code 404 when retrieving a period that does not exist" in {

      given()
        .userIsAuthorisedForTheResource(nino)
        .when()
        .post(selfEmployment).to(s"/ni/$nino/self-employments")
        .thenAssertThat()
        .statusIs(201)
        .when()
        .get(s"%sourceLocation%/periods/oops")
        .thenAssertThat()
        .statusIs(404)
    }
  }

  "retrieveAllPeriods" should {
    "return code 200 when retrieving all periods where periods.size > 0, sorted by from date" in {
      val periodOne = s"""{
                      |  "from": "2017-04-01",
                      |  "to": "2017-07-04"
                      |}""".stripMargin

      val periodTwo = s"""{
                         |  "from": "2017-07-05",
                         |  "to": "2017-08-04"
                         |}""".stripMargin

      val expectedBody =
        s"""
           |[
           |  $periodOne,
           |  $periodTwo
           |]
         """.stripMargin

      given()
        .userIsAuthorisedForTheResource(nino)
        .when()
        .post(selfEmployment).to(s"/ni/$nino/self-employments")
        .thenAssertThat()
        .statusIs(201)
        .when()
        .post(Json.parse(periodOne)).to(s"%sourceLocation%/periods")
        .thenAssertThat()
        .statusIs(201)
        .when()
        .post(Json.parse(periodTwo)).to(s"%sourceLocation%/periods")
        .thenAssertThat()
        .statusIs(201)
        .when()
        .get(s"%sourceLocation%/periods")
        .thenAssertThat()
        .statusIs(200)
        .contentTypeIsJson()
        .bodyIsLike(expectedBody)
        .selectFields(_ \\ "periodId").matches("\\w+".r)
    }

    "return code 200 containing an empty json body when retrieving all periods where periods.size == 0" in {
      given()
        .userIsAuthorisedForTheResource(nino)
        .when()
        .post(selfEmployment).to(s"/ni/$nino/self-employments")
        .thenAssertThat()
        .statusIs(201)
        .when()
        .get(s"%sourceLocation%/periods")
        .thenAssertThat()
        .statusIs(200)
        .jsonBodyIsEmptyArray
    }
  }
}