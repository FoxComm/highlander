package utils

import com.github.tminglei.slickpg._
import com.github.tminglei.slickpg.array.PgArrayJdbcTypes
import org.json4s.JValue
import slick.driver.PostgresDriver

trait ExPostgresDriver extends PostgresDriver
  with PgArraySupport
  with PgDateSupport
  with PgRangeSupport
  with PgHStoreSupport
  with PgJson4sSupport
  with PgArrayJdbcTypes
  with PgSearchSupport
  // with PgPostGISSupport
  with PgNetSupport
  with PgLTreeSupport {

  override val pgjson = "jsonb"
  type DOCType = JValue
  override val jsonMethods = org.json4s.jackson.JsonMethods

  override val api = MyAPI

  val plainAPI = new API with Json4sJsonPlainImplicits

  object MyAPI extends API with ArrayImplicits
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

    implicit val json4sJsonArrayTypeMapper: DriverJdbcType[List[JValue]] =
      new AdvancedArrayJdbcType[JValue](pgjson,
        (s) ⇒ utils.SimpleArrayUtils.fromString[JValue](jsonMethods.parse(_))(s).orNull,
        (v) ⇒ utils.SimpleArrayUtils.mkString[JValue](_.toString())(v)
      ).to(_.toList)
  }
}

object ExPostgresDriver extends ExPostgresDriver

