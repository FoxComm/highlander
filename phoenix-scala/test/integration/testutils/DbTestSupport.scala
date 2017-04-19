package testutils

import com.typesafe.config.{Config, ConfigFactory}
import java.sql.{Connection, PreparedStatement}
import javax.sql.DataSource
import models.objects.ObjectContexts
import models.product.SimpleContext
import org.scalatest._
import scala.annotation.tailrec
import slick.driver.PostgresDriver
import slick.driver.PostgresDriver.api._
import slick.jdbc.hikaricp.HikariCPJdbcDataSource
import utils.aliases._
import utils.db._
import utils.db.flyway.newFlyway
import utils.seeds.Factories

trait DbTestSupport extends SuiteMixin with BeforeAndAfterAll with GimmeSupport { self: TestSuite ⇒
  def dbConfig: Config =
    ConfigFactory.parseString(s"""db.name = "$dbName"
         |db.url = "jdbc:postgresql://localhost/$dbName?user=phoenix&prepareThreshold=0"
       """.stripMargin).withFallback(TestBase.bareConfig)

  val dbName: String = suiteName.flatMap {
    case c if c.isUpper ⇒ s"_${c.toLower}"
    case c              ⇒ s"$c"
  }.stripPrefix("_")

  implicit lazy val db: DB = {
    DbTestSupport.createDB(dbName)
    Database.forConfig("db", dbConfig)
  }

  implicit val ec: EC

  val api: PostgresDriver.API = DbTestSupport.api

  private[this] lazy val truncateTables: PreparedStatement = {
    val conn = db.source.createConnection()

    val allTables = conn.getMetaData.getTables(conn.getCatalog, "public", "%", Array("TABLE"))

    @tailrec
    def iterate(in: Seq[String]): Seq[String] =
      if (allTables.next()) iterate(in :+ allTables.getString(3)) else in

    val sqlTables = iterate(Seq()).filterNot { t ⇒
      t.startsWith("pg_") || t.startsWith("sql_") || DbTestSupport.DoNotTruncate.contains(t)
    }.mkString("{", ",", "}")

    conn.prepareStatement(s"select truncate_nonempty_tables('$sqlTables'::text[])")
  }

  override protected def beforeAll(): Unit = {
    DbTestSupport.migrateDB(DbTestSupport.jdbcDataSourceFromSlickDB(api))
    Factories.createSingleMerchantSystem.gimme
    truncateTables // init
  }

  override protected def afterAll(): Unit = {
    db.close()
    DbTestSupport.dropDB(dbName)
  }

  override abstract protected def withFixture(test: NoArgTest): Outcome = {
    truncateTables.executeQuery()

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

object DbTestSupport extends GimmeSupport {
  val db: DB = Database.forConfig("db", TestBase.bareConfig)

  def api: PostgresDriver.API = slick.driver.PostgresDriver.api

  /* tables which should *not* be truncated b/c they're static and seeded by migration */
  val DoNotTruncate = Set("states",
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

  private def createDB(name: String, conn: Connection): Unit = {
    val stmt = conn.createStatement()

    try {
      stmt.execute(s"create database $name")
    } finally stmt.close()
  }

  private def dropDB(name: String, conn: Connection): Unit = {
    val stmt = conn.createStatement()

    try {
      stmt.execute(s"update pg_database set datallowconn = 'false' where datname = '$name'")
      stmt.execute(s"""select pg_terminate_backend(pid)
             |from pg_stat_activity
             |where datname = '$name'""".stripMargin)
      stmt.execute(s"drop database if exists $name")
    } finally stmt.close()
  }

  def createDB(name: String)(implicit ec: EC): Unit = {
    val conn = db.source.createConnection()

    try {
      dropDB(name, conn)
      createDB(name, conn)
      // must be set before creation of hikari pool for IT db
      setSearchPath(name, conn, List("\"$user\"", "public", "exts"))
    } finally conn.close()
  }

  def dropDB(name: String)(implicit ec: EC): Unit = {
    val conn = db.source.createConnection()

    try dropDB(name, conn)
    finally conn.close()
  }

  def migrateDB(dataSource: DataSource): Unit = {
    val flyway = newFlyway(dataSource)

    flyway.clean()
    flyway.migrate()
  }

  def setSearchPath(name: String, conn: Connection, path: List[String]) = {
    val stmt = conn.createStatement()

    try stmt.execute(s"alter database $name set search_path to ${path.mkString(",")}")
    finally stmt.close()
  }

  def jdbcDataSourceFromSlickDB(api: PostgresDriver.API)(implicit db: api.Database): DataSource =
    db.source match {
      case source: HikariCPJdbcDataSource ⇒ source.ds
    }
}
