package foxcomm.agni.dsl

import cats.data.{NonEmptyList, NonEmptyVector}
import cats.implicits._
import io.circe._
import io.circe.generic.extras.semiauto._
import shapeless._

@SuppressWarnings(Array("org.wartremover.warts.Equals"))
object query {
  type QueryValue[T] = T :+: NonEmptyList[T] :+: CNil
  type CompoundValue = QueryValue[JsonNumber] :+: QueryValue[String] :+: CNil
  type Field         = String :+: NonEmptyVector[String] :+: CNil
  type RangeValue    = RangeBound[JsonNumber] :+: RangeBound[String] :+: CNil

  implicit class RichQueryValue[T](val qv: QueryValue[T]) extends AnyVal {
    def toNEL: NonEmptyList[T] = qv.eliminate(NonEmptyList.of(_), _.eliminate(identity, _.impossible))

    def toList: List[T] = toNEL.toList
  }

  implicit class RichCompoundValue(val cv: CompoundValue) extends AnyVal {
    def toNEL: NonEmptyList[AnyRef] = cv.eliminate(_.toNEL, _.eliminate(_.toNEL, _.impossible))

    def toList: List[AnyRef] = toNEL.toList
  }

  implicit def decodeQueryValue[T: Decoder]: Decoder[QueryValue[T]] =
    Decoder[T].map(Coproduct[QueryValue[T]](_)) or Decoder
      .decodeNonEmptyList[T]
      .map(Coproduct[QueryValue[T]](_))

  implicit val decodeCompoundValue: Decoder[CompoundValue] =
    Decoder[QueryValue[JsonNumber]].map(Coproduct[CompoundValue](_)) or
      Decoder[QueryValue[String]].map(Coproduct[CompoundValue](_))

  implicit val decodeField: Decoder[Field] = Decoder.decodeString.map { s ⇒
    val xs = s.split("\\.")
    if (xs.length > 1) Coproduct[Field](NonEmptyVector.of(xs.head, xs.tail: _*))
    else Coproduct[Field](s)
  }

  implicit val decodeRange: Decoder[RangeValue] =
    Decoder[RangeBound[JsonNumber]].map(Coproduct[RangeValue](_)) or
      Decoder[RangeBound[String]].map(Coproduct[RangeValue](_))

  sealed trait QueryField {
    def toNEL: NonEmptyList[Field]

    def toList: List[Field] = toNEL.toList
  }
  object QueryField {
    final case class Single(field: Field) extends QueryField {
      def toNEL: NonEmptyList[Field] = NonEmptyList.of(field)
    }
    object Single {
      implicit val decodeSingle: Decoder[Single] = Decoder[Field].map(Single(_))
    }

    final case class Multiple(fields: NonEmptyList[Field]) extends QueryField {
      def toNEL: NonEmptyList[Field] = fields
    }
    object Multiple {
      implicit val decodeMultiple: Decoder[Multiple] =
        Decoder.decodeNonEmptyList[Field].map(Multiple(_))
    }

    implicit val decodeQueryField: Decoder[QueryField] =
      Decoder[Single].map(identity[QueryField]) or
        Decoder[Multiple].map(identity[QueryField])
  }

  sealed trait QueryContext
  object QueryContext {
    case object filter extends QueryContext
    case object must   extends QueryContext
    case object should extends QueryContext
    case object not    extends QueryContext

    implicit val decodeQueryContext: Decoder[QueryContext] = deriveEnumerationDecoder[QueryContext]
  }

  sealed trait RangeFunction
  object RangeFunction {
    sealed trait LowerBound extends RangeFunction {
      def withBound: Boolean
    }
    case object Gt extends RangeFunction with LowerBound {
      def withBound: Boolean = false
    }
    case object Gte extends RangeFunction with LowerBound {
      def withBound: Boolean = true
    }

    sealed trait UpperBound extends RangeFunction {
      def withBound: Boolean
    }
    case object Lt extends RangeFunction with UpperBound {
      def withBound: Boolean = false
    }
    case object Lte extends RangeFunction with UpperBound {
      def withBound: Boolean = true
    }

    implicit val decodeRangeFunction: KeyDecoder[RangeFunction] = KeyDecoder.instance {
      case "lt" | "<"   ⇒ Some(Lt)
      case "lte" | "<=" ⇒ Some(Lte)
      case "gt" | ">"   ⇒ Some(Gt)
      case "gte" | ">=" ⇒ Some(Gte)
    }
  }

  final case class RangeBound[T](lower: Option[(RangeFunction.LowerBound, T)],
                                 upper: Option[(RangeFunction.UpperBound, T)])
  object RangeBound {
    import RangeFunction._

    implicit def decodeRangeBound[T: Decoder]: Decoder[RangeBound[T]] =
      Decoder.decodeMapLike[Map, RangeFunction, T].emap { map ⇒
        val lbs = map.view.collect {
          case (lb: LowerBound, v) ⇒ lb → v
        }.toList
        val ubs = map.view.collect {
          case (ub: UpperBound, v) ⇒ ub → v
        }.toList

        if (lbs.size > 1) Either.left("Only single lower bound can be specified")
        else if (ubs.size > 1) Either.left("Only single upper bound can be specified")
        else Either.right(RangeBound(lbs.headOption, ubs.headOption))
      }
  }

  sealed trait QueryFunction
  object QueryFunction {
    sealed trait WithField { this: QueryFunction ⇒
      def field: QueryField
    }
    sealed trait WithContext { this: QueryFunction ⇒
      def ctx: QueryContext
    }
    sealed trait TermLevel extends WithContext { this: QueryFunction ⇒
      def context: Option[QueryContext]

      final def ctx: QueryContext = context.getOrElse(QueryContext.filter)
    }
    sealed trait FullText extends WithContext with WithField { this: QueryFunction ⇒
      def context: Option[QueryContext]
      def in: Option[QueryField]

      final def ctx: QueryContext = context.getOrElse(QueryContext.must)
      final def field: QueryField = in.getOrElse(QueryField.Single(Coproduct("_all")))
    }

    final case class matches private (in: Option[QueryField],
                                      value: QueryValue[String],
                                      context: Option[QueryContext])
        extends QueryFunction
        with FullText
    final case class equals private (in: QueryField, value: CompoundValue, context: Option[QueryContext])
        extends QueryFunction
        with TermLevel
        with WithField {
      def field: QueryField = in
    }
    final case class exists private (value: QueryField, context: Option[QueryContext])
        extends QueryFunction
        with TermLevel
    final case class range private (in: QueryField.Single, value: RangeValue, context: Option[QueryContext])
        extends QueryFunction
        with TermLevel
        with WithField {
      def field: QueryField.Single = in
    }
    final case class raw private (value: JsonObject, context: QueryContext)
        extends QueryFunction
        with WithContext {
      def ctx: QueryContext = context
    }
    final case class bool private (in: QueryField.Single, value: QueryValue[QueryFunction])
        extends QueryFunction

    implicit val decodeQueryFunction: Decoder[QueryFunction] = deriveDecoder[QueryFunction]
  }

  final case class FCQuery(query: Option[NonEmptyList[QueryFunction]])
  object FCQuery {
    implicit val decodeFCQuery: Decoder[FCQuery] = {
      Decoder
        .decodeOption(
          Decoder
            .decodeNonEmptyList[QueryFunction]
            .or(Decoder[QueryFunction].map(NonEmptyList.of(_))))
        .map(FCQuery(_))
    }
  }
}
