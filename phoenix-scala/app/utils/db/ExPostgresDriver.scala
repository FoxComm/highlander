package utils.db

import cats.implicits._
import com.github.tminglei.slickpg._
import com.github.tminglei.slickpg.array.PgArrayJdbcTypes
import com.github.tminglei.slickpg.utils.SimpleArrayUtils
import io.circe.Json
import io.circe.jackson.syntax._
import io.circe.parser.parse
import slick.driver.PostgresDriver

trait ExPostgresDriver
    extends PostgresDriver
    with PgArraySupport
    with PgDateSupport
    with PgRangeSupport
    with PgHStoreSupport
    with PgCirceJsonSupport
    with PgArrayJdbcTypes
    with PgSearchSupport
    // with PgPostGISSupport
    with PgNetSupport
    with PgLTreeSupport {

  override val pgjson = "jsonb"
  type DOCType = Json

  override val api = MyAPI

  val plainAPI = new API with CirceJsonPlainImplicits

  object MyAPI
      extends API
      with ArrayImplicits
      with DateTimeImplicits
      with JsonImplicits
      with NetImplicits
      with LTreeImplicits
      with RangeImplicits
      with HStoreImplicits
      with SearchImplicits
      with SearchAssistants {
    implicit val strListTypeMapper: DriverJdbcType[List[String]] =
      new SimpleArrayJdbcType[String]("text").to(_.toList)

    implicit val jsonArrayTypeMapper: DriverJdbcType[List[Json]] = new AdvancedArrayJdbcType[Json](
        pgjson,
        (s) ⇒ SimpleArrayUtils.fromString[Json](parse(_).getOrElse(Json.Null))(s).orNull,
        (v) ⇒ SimpleArrayUtils.mkString[Json](_.jacksonPrint)(v)).to(_.toList)
  }
}

object ExPostgresDriver extends ExPostgresDriver
