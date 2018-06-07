package foxcomm.agni

import io.circe.Decoder
import io.circe.generic.extras.Configuration
import shapeless._

package object dsl {
  val Discriminator: String = foxcomm.agni.Discriminator

  implicit def configuration: Configuration = foxcomm.agni.configuration

  /** Decodes coproduct assuming that json representations of each coproduct element are disjoint. */
  implicit def decodeCoproduct[H: Decoder, T <: Coproduct: Decoder]: Decoder[H :+: T] =
    Decoder[H].map(Inl(_)) or Decoder[T].map(Inr(_))

  implicit def decodeCoproductLeaf[L: Decoder]: Decoder[L :+: CNil] =
    Decoder[L].map(Inl(_))
}
