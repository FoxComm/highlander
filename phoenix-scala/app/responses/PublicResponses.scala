package responses

import io.circe.Decoder.Result
import io.circe._
import models.location.{Country, Region}
import scala.collection.immutable.Seq

object PublicResponses {
  case class CountryWithRegions(country: Country, regions: Seq[Region])

  object CountryWithRegions {
    implicit val decodeCountryWithRegions: Decoder[CountryWithRegions] =
      new Decoder[CountryWithRegions] {
        def apply(c: HCursor): Result[CountryWithRegions] =
          for {
            country ← Decoder[Country].tryDecode(c)
            regions ← Decoder[Seq[Region]].tryDecode(c.downField("regions"))
          } yield CountryWithRegions(country, regions)
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
