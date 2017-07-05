package foxcomm.agni

import io.circe.Decoder
import io.circe.generic.extras.Configuration
import shapeless._

package object dsl {
  val Discriminator: String = foxcomm.agni.Discriminator

  implicit def configuration: Configuration = foxcomm.agni.configuration

  // decodes coproduct in a naive way assuming that json representations of each component are disjoint
  implicit def decodeCoproduct[H: Decoder, T <: Coproduct: Decoder]: Decoder[H :+: T] =
    Decoder[H].map(Inl(_)) or Decoder[T].map(Inr(_))

  implicit val decodeCNil: Decoder[CNil] = Decoder.failedWithMessage("Cannot decode value")
}
