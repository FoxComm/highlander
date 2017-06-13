package foxcomm.search.dsl

import cats.data.NonEmptyList
import cats.syntax.either._
import io.circe.generic.extras.semiauto._
import io.circe.{Decoder, JsonNumber, KeyDecoder}
import shapeless._

object query {
  sealed trait QueryField {
    def toList: List[String]
  }
  object QueryField {
    final case class Single(field: String) extends QueryField {
      def toList: List[String] = List(field)
    }
    object Single {
      implicit val decodeSingle: Decoder[Single] = Decoder.decodeString
        .emap {
          case s if s.startsWith("$") ⇒ Either.left(s"Defined unknown special query field $s")
          case s                      ⇒ Either.right(s)
        }
        .map(Single(_))
    }

    final case class Multiple(fields: NonEmptyList[String]) extends QueryField {
      def toList: List[String] = fields.toList
    }
    object Multiple {
      implicit val decodeMultiple: Decoder[Multiple] =
        Decoder.decodeNonEmptyList[String].map(Multiple(_))
    }

    implicit val decodeQueryField: Decoder[QueryField] =
      Decoder[Single].map(s ⇒ s: QueryField) or
        Decoder[Multiple].map(m ⇒ m: QueryField)
  }

  sealed trait RangeFunction
  object RangeFunction {
    case object Lt  extends RangeFunction
    case object Lte extends RangeFunction
    case object Gt  extends RangeFunction
    case object Gte extends RangeFunction

    implicit val decodeRangeFunction: KeyDecoder[RangeFunction] = KeyDecoder.instance {
      case "lt" | "<"   ⇒ Some(Lt)
      case "lte" | "<=" ⇒ Some(Lte)
      case "gt" | ">"   ⇒ Some(Gt)
      case "gte" | ">=" ⇒ Some(Gte)
    }
  }

  sealed trait EntityState
  object EntityState {
    case object all      extends EntityState
    case object active   extends EntityState
    case object inactive extends EntityState

    implicit val decodeEntityState: Decoder[EntityState] = deriveEnumerationDecoder[EntityState]
  }

  sealed trait QueryFunction
  @SuppressWarnings(
    Array("org.wartremover.warts.AsInstanceOf", "org.wartremover.warts.ExplicitImplicitTypes"))
  object QueryFunction {
    type SingleValue    = JsonNumber :+: String :+: CNil
    type MultipleValues = NonEmptyList[JsonNumber] :+: NonEmptyList[String] :+: CNil
    type CompoundValue  = SingleValue :+: MultipleValues :+: CNil
    type RangeValue     = Map[RangeFunction, JsonNumber] :+: Map[RangeFunction, String] :+: CNil

    object listOfAnyValueF extends Poly1 {
      implicit def caseValue = at[SingleValue](v ⇒ List(v.unify.asInstanceOf[AnyRef]))

      implicit def caseValues = at[MultipleValues](_.unify.toList.asInstanceOf[List[AnyRef]])
    }

    implicit val decodeSingleValue: Decoder[SingleValue] =
      Decoder.decodeJsonNumber.map(Inl(_)) or Decoder.decodeString.map(s ⇒ Inr(Inl(s)))

    implicit val decodeMultipleValue: Decoder[MultipleValues] =
      Decoder.decodeNonEmptyList[JsonNumber].map(Inl(_)) or Decoder
        .decodeNonEmptyList[String]
        .map(s ⇒ Inr(Inl(s)))

    implicit val decodeCompoundValue: Decoder[CompoundValue] =
      Decoder[SingleValue].map(Coproduct[CompoundValue](_)) or
        Decoder[MultipleValues].map(Coproduct[CompoundValue](_))

    implicit val decodeRange: Decoder[RangeValue] =
      Decoder.decodeMapLike[Map, RangeFunction, JsonNumber].map(Inl(_)) or
        Decoder.decodeMapLike[Map, RangeFunction, String].map(sm ⇒ Inr(Inl(sm)))

    final case class contains(in: QueryField, value: CompoundValue)  extends QueryFunction
    final case class matches(in: QueryField, value: CompoundValue)   extends QueryFunction
    final case class range(in: QueryField.Single, value: RangeValue) extends QueryFunction
    final case class eq(in: QueryField, value: CompoundValue)        extends QueryFunction
    final case class neq(in: QueryField, value: CompoundValue)       extends QueryFunction
    final case class state(value: EntityState)                       extends QueryFunction

    implicit val decodeQueryFunction: Decoder[QueryFunction] = deriveDecoder[QueryFunction]
  }

  final case class FCQuery(query: NonEmptyList[QueryFunction])
  object FCQuery {
    implicit val decodeFCQuery: Decoder[FCQuery] =
      Decoder
        .decodeNonEmptyList[QueryFunction]
        .or(Decoder[QueryFunction].map(NonEmptyList.of(_)))
        .map(FCQuery(_))
  }
}
