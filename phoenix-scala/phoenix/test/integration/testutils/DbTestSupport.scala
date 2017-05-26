package testutils

import java.sql.PreparedStatement
import java.util.Locale
import javax.sql.DataSource
import scala.util.Random

import objectframework.models.ObjectContexts
import org.scalatest._
import phoenix.models.product.SimpleContext
import phoenix.utils.aliases.EC
import phoenix.utils.db.flyway.{newFlyway, subprojectSqlLocation}
import phoenix.utils.seeds.Factories
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.hikaricp.HikariCPJdbcDataSource
import core.db._

import scala.annotation.tailrec

trait DbTestSupport extends SuiteMixin with BeforeAndAfterAll with GimmeSupport {
  this: TestSuite ⇒

  import DbTestSupport._

  implicit lazy val db = database

  implicit val ec: EC

  val api = slick.jdbc.PostgresProfile.api

  /* tables which should *not* be truncated b/c they're static and seeded by migration */
  val doNotTruncate = Set("states",
                          "countries",
                          "regions",
                          "schema_version",
                          "systems",
                          "resources",
                          "scopes",
                          "organizations",
                          "scope_domains",
                          "roles",
                          "permissions",
                          "role_permissions")

  private def randomizeSequences(schema: String): Unit = {
    // When changing this, please, if anything, make them less predictable, not more. @michalrus
    val allSequences =
      sql"SELECT sequence_name FROM information_schema.sequences WHERE sequence_schema = $schema"
        .as[String]
        .gimme

    // TODO: Make it possible to not filter these out… @michalrus
    val randomizedSequences = allSequences.filterNot(
        Set(
            "scopes_id_seq" // FIXME: What the hell. https://foxcommerce.slack.com/archives/C06696D1R/p1495796779988723
        ) contains _)

    val gap = 1000000
    val withValues = Random
      .shuffle(randomizedSequences)
      .zip(Stream.from(1).map(_ * gap + Random.nextInt(gap / 10)))
    DBIO
      .sequence(withValues.map {
        case (name, value) ⇒
          val increment     = (if (Random.nextBoolean()) 1 else -1) * Random.nextInt(100)
          val incrementNon0 = if (increment == 0) -1 else increment
          sql"ALTER SEQUENCE #$name START WITH #$value INCREMENT BY #$incrementNon0 RESTART".asUpdate
      })
      .gimme
  }

  override protected def beforeAll(): Unit = {
    if (!migrated) {
      Locale.setDefault(Locale.US)
      val flyway = newFlyway(dataSource, subprojectSqlLocation)

      flyway.clean()
      flyway.migrate()

      val Schema = "public"

      // TODO: it would be best if data created in *.sql migrations above had randomized sequences as well… @michalrus
      randomizeSequences(Schema)

      truncateTablesStmt = {
        // FIXME: just use Slick, it can be done IIRC @michalrus
        val allTables: Seq[String] = {
          val src =
            persistConn.getMetaData.getTables(persistConn.getCatalog, Schema, "%", Array("TABLE"))

          @tailrec
          def iterate(in: Seq[String]): Seq[String] =
            if (src.next()) iterate(in :+ src.getString(3)) else in

          iterate(Seq())
        }

        tables = allTables.filterNot { t ⇒
          t.startsWith("pg_") || t.startsWith("sql_") || doNotTruncate.contains(t)
        }
        val sqlTables = tables.mkString("{", ",", "}")
        persistConn.prepareStatement(s"select truncate_nonempty_tables('$sqlTables'::text[])")
      }

      Factories.createSingleMerchantSystem.gimme

      migrated = true
    }
  }

  override abstract protected def withFixture(test: NoArgTest): Outcome = {
    truncateTablesStmt.executeQuery()

    // TODO: Use Seeds.createBase after promo tests are fixed?
    createBaseTestSeeds()

    test()
  }

  private def createBaseTestSeeds() = {
    // Base test data
    (for {
      _ ← * <~ ObjectContexts.create(SimpleContext.create())
      // Can't create all schemas right now because promo tests are fucky
      // FIXME @anna @michalrus
      _ ← * <~ Factories.FIXME_createAllButPromoSchemas
    } yield {}).gimme
  }
}

object DbTestSupport {

  @volatile var migrated                              = false
  @volatile var tables: Seq[String]                   = Seq()
  @volatile var truncateTablesStmt: PreparedStatement = _

  lazy val database    = Database.forConfig("db", TestBase.bareConfig)
  lazy val dataSource  = jdbcDataSourceFromSlickDB(database)
  lazy val persistConn = dataSource.getConnection
  val api              = slick.jdbc.PostgresProfile.api

  def jdbcDataSourceFromSlickDB(db: api.Database): DataSource =
    db.source match {
      case source: HikariCPJdbcDataSource ⇒ source.ds
    }
}
