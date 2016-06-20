package uk.gov.hmrc.selfassessmentapi.live

import org.joda.time.LocalDate
import play.api.libs.json.JsValue
import play.api.libs.json.Json.{parse, toJson}
import uk.gov.hmrc.selfassessmentapi.MongoEmbeddedDatabase
import uk.gov.hmrc.selfassessmentapi.domain.ErrorCode.COMMENCEMENT_DATE_NOT_IN_THE_PAST
import uk.gov.hmrc.selfassessmentapi.domain.TaxYear
import uk.gov.hmrc.selfassessmentapi.domain.selfemployment.SelfEmployment
import uk.gov.hmrc.selfassessmentapi.domain.selfemployment.SelfEmployment._
import uk.gov.hmrc.selfassessmentapi.domain.selfemployment.SourceType.SelfEmployments
import uk.gov.hmrc.selfassessmentapi.repositories.SourceRepository
import uk.gov.hmrc.selfassessmentapi.repositories.live.SelfEmploymentMongoRepository
import uk.gov.hmrc.support.BaseFunctionalSpec

case class Output(body: JsValue, code: Int)

case class Scenario(input: JsValue, output: Output)

class SourceControllerSpec extends BaseFunctionalSpec with MongoEmbeddedDatabase {

  private val seRepository: SourceRepository[SelfEmployment] = new SelfEmploymentMongoRepository

  val supportedSourceTypes = Set(SelfEmployments)

  val errorScenarios = Map(
    SelfEmployments -> Scenario( input = toJson(SelfEmployment.example().copy(commencementDate = LocalDate.now().plusDays(1))),
                                 output = Output(
                                     body = parse(
                                        s"""
                                           |[
                                           | {
                                           |   "path":"/commencementDate",
                                           |   "code": "${COMMENCEMENT_DATE_NOT_IN_THE_PAST}",
                                           |   "message":"commencement date should be in the past"
                                           | }
                                           |]
                                          """.stripMargin),
                                        code = 400
                                  )
    )
  )

  lazy val createSource = Map(
    SelfEmployments -> await(seRepository.create(saUtr, TaxYear(taxYear), SelfEmployment.example()))
  )



  override def beforeEach() {
    super.baseBeforeEach()
  }

  override def beforeAll = {
    super.mongoBeforeAll()
    super.baseBeforeAll()
  }

  override def afterAll: Unit = {
    super.mongoAfterAll()
  }

  "Live source controller" should {

    "return a 404 error when source type is invalid" in {
      given().userIsAuthorisedForTheResource(saUtr)
        .when()
        .get(s"/$saUtr/$taxYear/blah")
        .thenAssertThat()
        .statusIs(404)
    }

    "return a 404 error when source with given id does not exist" in {
      supportedSourceTypes.foreach { sourceType =>
        given().userIsAuthorisedForTheResource(saUtr)
          .when()
          .get(s"/$saUtr/$taxYear/${sourceType.name}/asdfasdf")
          .thenAssertThat()
          .statusIs(404)

        given().userIsAuthorisedForTheResource(saUtr)
          .when()
          .put(s"/$saUtr/$taxYear/${sourceType.name}/asdfasdf", Some(sourceType.example()))
          .thenAssertThat()
          .statusIs(404)

        given().userIsAuthorisedForTheResource(saUtr)
          .when()
          .delete(s"/$saUtr/$taxYear/${sourceType.name}/asdfasdf")
          .thenAssertThat()
          .statusIs(404)

      }
    }

    "return a 201 response with links if POST is successful" in {
      supportedSourceTypes.foreach { sourceType =>
        given().userIsAuthorisedForTheResource(saUtr)
        when()
          .post(s"/$saUtr/$taxYear/${sourceType.name}", Some(sourceType.example()))
          .thenAssertThat()
          .statusIs(201)
          .contentTypeIsHalJson()
          .bodyHasLink("self", s"/self-assessment/$saUtr/$taxYear/${sourceType.name}/.+".r)
          .bodyHasSummaryLinks(sourceType, saUtr, taxYear)
      }
    }

    "return 400 and an error response if the data for POST is invalid" in {
      supportedSourceTypes.foreach { sourceType =>
        given().userIsAuthorisedForTheResource(saUtr)
        when()
          .post(s"/$saUtr/$taxYear/${sourceType.name}", Some(errorScenarios(sourceType).input))
          .thenAssertThat()
          .statusIs(errorScenarios(sourceType).output.code)
          .bodyIs(errorScenarios(sourceType).output.body)
      }
    }

    "return 200 code with links if PUT is successful" in {
      supportedSourceTypes.foreach { sourceType =>
        val seId = createSource(sourceType)
        given().userIsAuthorisedForTheResource(saUtr)
        when()
          .put(s"/$saUtr/$taxYear/${sourceType.name}/$seId", Some(toJson(SelfEmployment.example().copy(commencementDate = LocalDate.now().minusDays(1)))))
          .thenAssertThat()
          .statusIs(200)
          .bodyHasLink("self", s"/self-assessment/$saUtr/$taxYear/${sourceType.name}/.+".r)
          .bodyHasSummaryLinks(sourceType, saUtr, taxYear)
      }
    }

    "return 204 code when DELETE is successful" in {
      supportedSourceTypes.foreach { sourceType =>
        val seId = createSource(sourceType)
        given().userIsAuthorisedForTheResource(saUtr)
        when()
          .delete(s"/$saUtr/$taxYear/${sourceType.name}/$seId")
          .thenAssertThat()
          .statusIs(204)
      }
    }

    "return 400 and an error response if the data for PUT is invalid" in {
      supportedSourceTypes.foreach { sourceType =>
        val seId = createSource(sourceType)
        given().userIsAuthorisedForTheResource(saUtr)
        when()
          .put(s"/$saUtr/$taxYear/${sourceType.name}/$seId", Some(errorScenarios(sourceType).input))
          .thenAssertThat()
          .statusIs(errorScenarios(sourceType).output.code)
          .bodyIs(errorScenarios(sourceType).output.body)
      }
    }
  }
}