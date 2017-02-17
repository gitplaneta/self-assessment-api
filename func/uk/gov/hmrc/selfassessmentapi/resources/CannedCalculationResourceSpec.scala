package uk.gov.hmrc.selfassessmentapi.resources

import uk.gov.hmrc.support.BaseFunctionalSpec

class CannedCalculationResourceSpec extends BaseFunctionalSpec {
  "requestCalculation" should {
    "return 202 containing a Location header, along with an ETA for the calculation to be ready" in {
      given()
        .userIsAuthorisedForTheResource(nino)
        .when()
        .post(s"/ni/$nino/calculations")
        .thenAssertThat()
        .statusIs(202)
        .responseContainsHeader("Location", s"/self-assessment/ni/$nino/calculations/\\w+".r)
        .bodyIsLike(Jsons.CannedCalculation.eta(5).toString())
    }
  }

  "retrieveCalculation" should {
    "return 200 containing a calculation" in {
      given()
        .userIsAuthorisedForTheResource(nino)
        .when()
        .post(s"/ni/$nino/calculations")
        .thenAssertThat()
        .statusIs(202)
        .when()
        .get(s"%sourceLocation%")
        .thenAssertThat()
        .statusIs(200)
        .bodyIsLike(Jsons.CannedCalculation().toString)
    }

    "return 404 when attempting to retrieve a calculation using an invalid id" in {
      given()
        .userIsAuthorisedForTheResource(nino)
        .when()
        .get(s"/ni/$nino/calculations/ohno")
        .thenAssertThat()
        .statusIs(404)
    }
  }
}