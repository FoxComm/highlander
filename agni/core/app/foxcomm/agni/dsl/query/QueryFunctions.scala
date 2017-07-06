package foxcomm.agni.dsl.query

import cats.data.NonEmptyList
import cats.implicits._
import foxcomm.agni.dsl._
import io.circe._
import io.circe.generic.extras.semiauto._
import scala.util.Try
import shapeless._

@SuppressWarnings(Array("org.wartremover.warts.Equals", "org.wartremover.warts.Throw"))
private[query] trait QueryFunctions { this: QueryData ⇒
  object Boostable {
    private[this] val boostableRegex = "^(\\w+)\\^([0-9]*\\.?[0-9]+)$".r

    def unapply(s: String): Option[(String, Float)] = s match {
      case boostableRegex(f, b) ⇒ Try(f → b.toFloat).toOption
      case _                    ⇒ None
    }
  }

  sealed trait QueryFunction
  object QueryFunction {
    val BoostField: String = "boost"
    val DepthField         = "_depth"

    // TODO: make it configurable (?)
    val MaxDepth = 25

    sealed trait WithField { this: QueryFunction ⇒
      def field: QueryField
      def boost: Option[Float]
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

    sealed case class matches private (in: Option[QueryField],
                                       value: QueryValue[String],
                                       context: Option[QueryContext],
                                       boost: Option[Float])
        extends QueryFunction
        with FullText
    object matches {
      implicit val decodeMatches: Decoder[matches] = deriveDecoder[matches]
    }

    sealed case class equals private (in: QueryField,
                                      value: CompoundValue,
                                      context: Option[QueryContext],
                                      boost: Option[Float])
        extends QueryFunction
        with TermLevel
        with WithField {
      def field: QueryField = in
    }
    object equals {
      implicit val decodeEquals: Decoder[equals] = deriveDecoder[equals]
    }

    sealed case class exists private (value: QueryField, context: Option[QueryContext])
        extends QueryFunction
        with TermLevel
    object exists {
      implicit val decodeExists: Decoder[exists] = deriveDecoder[exists]
    }

    sealed case class range private (in: QueryField.Single,
                                     value: RangeValue,
                                     context: Option[QueryContext],
                                     boost: Option[Float])
        extends QueryFunction
        with TermLevel
        with WithField {
      def field: QueryField.Single = in
    }
    object range {
      implicit val decodeRange: Decoder[range] = deriveDecoder[range]
    }

    sealed case class raw private (value: JsonObject, context: QueryContext)
        extends QueryFunction
        with WithContext {
      def ctx: QueryContext = context
    }
    object raw {
      implicit val decodeRaw: Decoder[raw] = deriveDecoder[raw]
    }

    sealed case class bool private (value: QueryValue[QueryFunction], context: QueryContext)
        extends QueryFunction
        with WithContext {
      def ctx: QueryContext = context
    }
    object bool {
      implicit lazy val decodeNestedQueryFunctions: Decoder[QueryValue[QueryFunction]] = {
        val decodeQfs: Decoder[QueryValue[QueryFunction]] =
          Decoder.decodeNonEmptyList[QueryFunction].map(qfs ⇒ Inr(Inl(qfs)))
        val decodeQf: Decoder[QueryValue[QueryFunction]] =
          Decoder[QueryFunction].map(qf ⇒ Inl(qf))

        Decoder.instance[QueryValue[QueryFunction]] { c ⇒
          // explicit codec definition as we don't want circe to try other codec
          // for the sake of better client error messages
          if (c.value.isArray) decodeQfs(c)
          else decodeQf(c)
        }
      }

      implicit val decodeBool: Decoder[bool] = {
        val decoder = deriveDecoder[bool]

        Decoder.instance { c ⇒
          val depth = (for {
            parent ← c.up.focus
            parent ← parent.asObject
            depth  ← parent(DepthField)
            depth  ← depth.as[Int].toOption
          } yield depth).getOrElse(1)

          // we start counting from 0,
          // which denotes implicit top-level bool query
          if (depth > MaxDepth)
            Either.left(DecodingFailure(s"Max depth of $MaxDepth exceeded for a bool query", c.history))
          else
            decoder.tryDecode(c.withFocus(_.mapObject(_.add(DepthField, Json.fromInt(depth + 1)))))
        }
      }
    }

    implicit val decodeQueryFunction: Decoder[QueryFunction] = {
      val boostable: Map[String, Decoder[_ <: QueryFunction with QueryFunction.WithField]] = Map(
        "matches" → Decoder[matches],
        "equals"  → Decoder[equals],
        "range"   → Decoder[range]
      )
      val all: Map[String, Decoder[_ <: QueryFunction]] = boostable ++ Map(
        "exists" → Decoder[exists],
        "raw"    → Decoder[raw],
        "bool"   → Decoder[bool]
      )

      Decoder.instance { hc ⇒
        val c   = hc.downField(Discriminator)
        val tpe = c.focus.flatMap(_.asString)
        tpe match {
          case Some(Boostable(t, b)) ⇒
            if (boostable.contains(t))
              boostable(t).tryDecode(hc.withFocus(_.mapObject(_.add(BoostField, Json.fromFloatOrNull(b)))))
            else
              Either.left(DecodingFailure(s"Query function is not boostable", c.history))
          case Some(t) if all.contains(t) ⇒
            all(t).tryDecode(hc)
          case _ ⇒
            Either.left(DecodingFailure("Unknown query function type", c.history))
        }
      }
    }
  }

  sealed case class FCQuery(query: Option[NonEmptyList[QueryFunction]])
  object FCQuery {
    implicit val decodeFCQuery: Decoder[FCQuery] = {
      val qfsDecode = Decoder.decodeNonEmptyList[QueryFunction]
      val qfDecode  = Decoder[QueryFunction].map(NonEmptyList.of(_))

      Decoder.instance { c ⇒
        if (c.value.isNull) Either.right(FCQuery(None))
        else if (c.value.isArray) qfsDecode.map(qfs ⇒ FCQuery(Some(qfs)))(c)
        else if (c.value.isObject) qfDecode.map(qfs ⇒ FCQuery(Some(qfs)))(c)
        else
          Either.left(
            DecodingFailure(
              "Query DSL must be either empty or " +
                "a non-empty array of query functions or " +
                "a single query function object",
              c.history
            ))
      }
    }
  }
}
