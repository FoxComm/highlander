package foxcomm.search

import cats.data.NonEmptyList
import cats.syntax.either._
import io.circe.Decoder.Result
import io.circe.generic.extras.auto._
import io.circe.{Decoder, DecodingFailure, HCursor}
import scala.collection.immutable
import shapeless._

sealed trait QueryField extends Product with Serializable {
  def toList: List[String]
}
object QueryField {
  final case class Single(field: String) extends QueryField {
    def toList: List[String] = List(field)
  }
  final case class Multiple(fields: NonEmptyList[String]) extends QueryField {
    def toList: List[String] = fields.toList
  }
  final case class Compound(field: String) extends QueryField {
    def toList: List[String] = ??? // TODO: implement compound fields like $text
  }
}

sealed trait RangeFunction
object RangeFunction {
  case object Lt  extends RangeFunction
  case object Lte extends RangeFunction
  case object Gt  extends RangeFunction
  case object Gte extends RangeFunction
  case object Eq  extends RangeFunction
  case object Neq extends RangeFunction
}

sealed trait QueryFunction extends Product with Serializable {
  type Value <: Coproduct

  def value: Value
}
@SuppressWarnings(
  Array("org.wartremover.warts.AsInstanceOf",
        "org.wartremover.warts.Equals",
        "org.wartremover.warts.ExplicitImplicitTypes"))
object QueryFunction {
  type QueryValue[T] = T :+: NonEmptyList[T] :+: CNil
  // TODO: what with precision loss? current elastic4s version operates on Double's in range query anyway
  type SingleValue   = Double :+: String :+: CNil
  type CompoundValue = QueryValue[SingleValue]
  type RangeValue    = immutable.Map[RangeFunction, SingleValue] :+: CNil
  type StateValue    = allW.T :+: activeW.T :+: inactiveW.T :+: CNil

  val allW      = Witness("all")
  val activeW   = Witness("active")
  val inactiveW = Witness("inactive")

  object queryFieldF extends Poly1 {
    implicit def caseField  = at[String](QueryField.Single)
    implicit def caseFields = at[NonEmptyList[String]](QueryField.Multiple)
  }

  // we autobox `Double` here via type cast
  object listOfAnyValueF extends Poly1 {
    implicit def caseValue  = at[SingleValue](v ⇒ List(v.unify.asInstanceOf[AnyRef]))
    implicit def caseValues = at[NonEmptyList[SingleValue]](_.map(_.unify).toList.asInstanceOf[List[AnyRef]])
  }

  implicit def decodeQueryValue[T: Decoder]: Decoder[QueryValue[T]] =
    Decoder[T]
      .map(Coproduct[QueryValue[T]](_))
      .or(Decoder.decodeNonEmptyList[T].map(Coproduct[QueryValue[T]](_)))

  implicit val decodeSingleValue: Decoder[SingleValue] =
    Decoder.decodeDouble.map(Inl(_)).or(Decoder.decodeString.map(s ⇒ Inr(Inl(s))))

  implicit val decodeState: Decoder[StateValue] =
    Decoder.decodeString.emap {
      case v if v == allW.value      ⇒ Either.right(Coproduct[StateValue](allW.value))
      case v if v == activeW.value   ⇒ Either.right(Coproduct[StateValue](activeW.value))
      case v if v == inactiveW.value ⇒ Either.right(Coproduct[StateValue](inactiveW.value))
      case _                         ⇒ Either.left("unknown state defined")
    }

  implicit val decodeQueryField: Decoder[QueryField] = new Decoder[QueryField] {
    def apply(c: HCursor): Result[QueryField] =
      c.downField("in").as[QueryValue[String]].right.map(_.fold(queryFieldF))
  }

  implicit val decodeCompoundValue: Decoder[CompoundValue] = decodeQueryValue[SingleValue]

  implicit val decodeRange: Decoder[RangeValue] = new Decoder[RangeValue] {
    def apply(c: HCursor): Result[RangeValue] = ??? // TODO: implement decoder
  }

  private[this] val decoderMap = Map(
    "contains" → Decoder[Contains],
    "matches"  → Decoder[Matches],
    "is"       → Decoder[Is],
    "range"    → Decoder[Range],
    "state"    → Decoder[State]
  )

  implicit val decodeQueryFunction: Decoder[QueryFunction] = new Decoder[QueryFunction] {
    def apply(c: HCursor): Result[QueryFunction] =
      for {
        tpe ← c.downField("type").as[String]
        decoder ← decoderMap
                    .get(tpe)
                    .map(Either.right(_))
                    .getOrElse(Either.left(DecodingFailure("", c.history)))
        value ← decoder(c)
      } yield value
  }

  final case class Contains(in: QueryField, value: CompoundValue) extends QueryFunction {
    type Value = CompoundValue
  }
  final case class Matches(in: QueryField, value: CompoundValue) extends QueryFunction {
    type Value = CompoundValue
  }
  final case class Range(in: QueryField.Single, value: RangeValue) extends QueryFunction {
    type Value = RangeValue
  }
  final case class Is(in: QueryField, value: CompoundValue) extends QueryFunction {
    type Value = CompoundValue
  }
  final case class State(value: StateValue) extends QueryFunction {
    type Value = StateValue
  }
}

final case class FCQuery(query: NonEmptyList[QueryFunction])
object FCQuery {
  // TODO: check performance (e.g. no doubled attempt for query function decoding) if query is malformed
  // should be fast, as it'd either fail fast on lack of array
  // or on first (or single) malformed query function
  implicit val decodeFCQuery: Decoder[FCQuery] =
    Decoder
      .decodeNonEmptyList[QueryFunction]
      .or(Decoder[QueryFunction].map(NonEmptyList.of(_)))
      .map(FCQuery(_))
}
