package foxcomm.search.dsl

import cats.data.NonEmptyList
import cats.syntax.either._
import io.circe.generic.extras.semiauto._
import io.circe.{Decoder, JsonNumber, KeyDecoder}
import shapeless._
import shapeless.ops.coproduct.Folder

@SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf", "org.wartremover.warts.ExplicitImplicitTypes"))
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
    sealed trait LowerBound extends RangeFunction {
      override def hashCode(): Int = super.hashCode()

      override def equals(obj: scala.Any): Boolean = super.equals(obj)
    }
    case object Lt  extends RangeFunction with LowerBound
    case object Lte extends RangeFunction with LowerBound

    sealed trait UpperBound extends RangeFunction {
      override def hashCode(): Int = super.hashCode()

      override def equals(obj: scala.Any): Boolean = super.equals(obj)
    }
    case object Gt  extends RangeFunction with UpperBound
    case object Gte extends RangeFunction with UpperBound

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

  type QueryValue[T] = T :+: NonEmptyList[T] :+: CNil
  type CompoundValue = QueryValue[JsonNumber] :+: QueryValue[String] :+: CNil
  type RangeValue    = Map[RangeFunction, JsonNumber] :+: Map[RangeFunction, String] :+: CNil

  object queryValueF extends Poly1 {
    implicit def singleValue[T] = at[T](List(_))

    implicit def multipleValues[T] = at[NonEmptyList[T]](_.toList)
  }

  object listOfAnyValueF extends Poly1 {
    implicit def caseJsonNumber = at[QueryValue[JsonNumber]](_.fold(queryValueF).asInstanceOf[List[AnyRef]])

    implicit def caseString = at[QueryValue[String]](_.fold(queryValueF).asInstanceOf[List[AnyRef]])
  }

  // Ugly optimisation to avoid recreation of folder instance on each implicit call
  // It is safe to cast it the implicit to proper type,
  // since we don't assume anything about the type, but only construct list.
  private[this] val queryValueFolderInstance = new Folder[queryValueF.type, QueryValue[_]] {
    type Out = List[_]

    def apply(qv: QueryValue[_]): Out = qv match {
      case Inl(v)       ⇒ List(v)
      case Inr(Inl(vs)) ⇒ vs.toList
      case _            ⇒ Nil
    }
  }

  // TODO: for some reason shapeless cannot find implicit `Folder` instance
  // if Poly function cases contain generic param as in `queryValueF`
  implicit def queryValueFolder[T]: Folder[queryValueF.type, QueryValue[T]] =
    queryValueFolderInstance.asInstanceOf[Folder[queryValueF.type, QueryValue[T]]]

  implicit class RichQueryValue[T](val qv: QueryValue[T]) extends AnyVal {
    def toList: List[T] = qv.fold(queryValueF).asInstanceOf[List[T]]
  }

  implicit class RichCompoundValue(val cv: CompoundValue) extends AnyVal {
    def toList(implicit folder: Folder[listOfAnyValueF.type, CompoundValue]): List[AnyRef] =
      cv.fold(listOfAnyValueF).asInstanceOf[List[AnyRef]]
  }

  implicit def decodeQueryValue[T: Decoder]: Decoder[QueryValue[T]] =
    Decoder[T].map(Inl(_)) or Decoder.decodeNonEmptyList[T].map(n ⇒ Inr(Inl(n)))

  implicit val decodeCompoundValue: Decoder[CompoundValue] =
    Decoder[QueryValue[JsonNumber]].map(Coproduct[CompoundValue](_)) or
      Decoder[QueryValue[String]].map(Coproduct[CompoundValue](_))

  implicit val decodeRange: Decoder[RangeValue] =
    Decoder.decodeMapLike[Map, RangeFunction, JsonNumber].map(Inl(_)) or
      Decoder.decodeMapLike[Map, RangeFunction, String].map(sm ⇒ Inr(Inl(sm)))

  sealed trait QueryFunction
  object QueryFunction {
    final case class matches(in: QueryField, value: QueryValue[String]) extends QueryFunction
    final case class range(in: QueryField.Single, value: RangeValue)    extends QueryFunction
    final case class eq(in: QueryField, value: CompoundValue)           extends QueryFunction
    final case class neq(in: QueryField, value: CompoundValue)          extends QueryFunction
    final case class state(value: EntityState)                          extends QueryFunction

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
