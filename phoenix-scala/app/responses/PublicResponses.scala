package responses

import cats.implicits._
import io.circe.Decoder.Result
import io.circe._
import models.location.{Country, Region}
import scala.collection.immutable.Seq
import utils.json.codecs._

object PublicResponses {
  case class CountryWithRegions(country: Country, regions: Seq[Region])

  object CountryWithRegions {
    implicit val decodeCountryWithRegions: Decoder[CountryWithRegions] =
      new Decoder[CountryWithRegions] {
        def apply(c: HCursor): Result[CountryWithRegions] =
          Decoder[Country]
            .tryDecode(c)
            .map2(Decoder[Seq[Region]].tryDecode(c.downField("regions")))(CountryWithRegions(_, _))
      }

    implicit val encodeCountryWithRegions: Encoder[CountryWithRegions] =
      new Encoder[CountryWithRegions] {
        def apply(cwr: CountryWithRegions): Json =
          Encoder[Country]
            .apply(cwr.country)
            .deepMerge(Json.obj("regions" → Encoder[Seq[Region]].apply(cwr.regions)))
      }
  }
}
