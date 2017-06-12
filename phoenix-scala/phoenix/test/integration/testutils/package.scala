import java.util.regex.Pattern

import akka.http.scaladsl.model.{HttpResponse, StatusCode, StatusCodes}
import core.failures.Failure
import org.json4s.Formats
import org.json4s.jackson.JsonMethods._
import org.scalatest._
import org.scalatest.concurrent.PatienceConfiguration
import phoenix.responses.TheResponse
import phoenix.utils.JsonFormatters
import phoenix.utils.aliases._
import scala.concurrent.Await._
import scala.concurrent.duration._

package object testutils extends MustMatchers with OptionValues with AppendedClues {

  implicit val formats: Formats = JsonFormatters.phoenixFormats

  def originalSourceClue(implicit line: SL, file: SF) =
    s"""\n(Original source: ${file.value.split("/").last}:${line.value})"""

  type FoxSuite = TestSuite with PatienceConfiguration with DbTestSupport

  implicit class RichAttributes(val attributes: Json) extends AnyVal {
    def get[A](field: String)(implicit mf: Manifest[A]): A =
      (attributes \ field \ "v").extract[A]

    def getOpt[A](field: String)(implicit mf: Manifest[A]): Option[A] =
      (attributes \ field \ "v").extractOpt[A]

    def getValue[A](field: String)(implicit mf: Manifest[A]): A =
      (attributes \ field \ "v" \ "value").extract[A]

    def getString(field: String): String = get[String](field)

    // Concrete attribute extractors
    def code: String      = getString("code")
    def salePrice: Long   = getValue[Long]("salePrice")
    def retailPrice: Long = getValue[Long]("retailPrice")
  }

  implicit class RichTraversable[A](val sequence: Traversable[A]) extends AnyVal {
    def onlyElement(implicit sl: SL, sf: SF): A = {
      sequence must have size 1
      sequence.head
    } withClue originalSourceClue
  }

  implicit class RichHttpResponse(response: HttpResponse)(implicit ec: EC, mat: Mat)
      extends MustMatchers
      with OptionValues
      with AppendedClues {

    lazy val bodyText: String =
      result(response.entity.toStrict(1.second).map(_.data.utf8String), 1.second)

    def as[A <: AnyRef](implicit mf: Manifest[A], line: SL, file: SF): A = {
      response.mustBeOk()
      parse(bodyText).extractOpt[A].value.withClue(s"Failed to parse body!")
    } withClue originalSourceClue

    def asTheResult[A <: AnyRef](implicit mf: Manifest[A], line: SL, file: SF): A =
      asThe[A].result

    def asThe[A <: AnyRef](implicit mf: Manifest[A], line: SL, file: SF): TheResponse[A] =
      as[TheResponse[A]]

    def errors(implicit line: SL, file: SF): List[String] = {
      withClue("Unexpected response status!") { response.status must !==(StatusCodes.OK) }
      extractErrors
    } withClue originalSourceClue

    def error(implicit line: SL, file: SF): String =
      errors.headOption.value.withClue("Expected at least one error, got none!")

    def mustHaveStatus(expected: StatusCode*)(implicit line: SL, file: SF): Unit = {
      withClue("Unexpected response status!") {
        expected.toList match {
          case only :: Nil ⇒ response.status must === (only)
          case _           ⇒ expected must contain(response.status)
        }
      }
    } withClue originalSourceClue

    def mustBeOk()(implicit line: SL, file: SF): Unit =
      mustHaveStatus(StatusCodes.OK)
        .withClue(s"\n Response ${response.headers} \nErrors: $extractErrors!")

    def mustBeEmpty()(implicit line: SL, file: SF): Unit = {
      mustHaveStatus(StatusCodes.NoContent)
      (bodyText mustBe empty).withClue(s"Expected empty body, got $bodyText!")
    }

    def mustFailWith404(expected: Failure*)(implicit line: SL, file: SF): Unit =
      mustFailWith(StatusCodes.NotFound, expected.map(_.description): _*)

    def mustFailWith400(expected: Failure*)(implicit line: SL, file: SF): Unit =
      mustFailWith(StatusCodes.BadRequest, expected.map(_.description): _*)

    def mustFailWith401(implicit line: SL, file: SF): Unit =
      mustFailWith(StatusCodes.Unauthorized,
                   "The resource requires authentication, which was not supplied with the request")

    def mustFailWithMessage(expected: String*)(implicit line: SL, file: SF): Unit =
      mustFailWith(StatusCodes.BadRequest, expected: _*)

    private def mustFailWith(statusCode: StatusCode, expected: String*)(implicit line: SL, file: SF): Unit = {
      mustHaveStatus(statusCode)

      val expectedRegex = expected.map(
        _.split(Pattern.quote("%ANY%"),
                -1 /* Keep trailing empty strings for "%ANY" at the end of the pattern. */ ).toList
          .map(Pattern.quote)
          .mkString(".*?"))

      expectedRegex.toList match {
        case only :: Nil ⇒ response.error must (fullyMatch regex only)
        case _ ⇒
          response.errors.foreach { error ⇒
            withClue(s"“$error” did not match any of $expectedRegex") {
              expectedRegex.exists(error matches _) mustBe true
            }
          }
      }
    } withClue originalSourceClue

    private def extractErrors: List[String] = {
      val errors = (parse(bodyText) \ "errors")
        .extractOpt[List[String]]
        .value
        .withClue(s"Expected errors, found $bodyText!")

      // Apparently I was too ambitious with this one... -- Anna
      // Fucking FIXME, what kind of API is this?!
      // (errors must not be empty).withClue("Expected errors, found empty list!")

      errors
    }
  }
}
