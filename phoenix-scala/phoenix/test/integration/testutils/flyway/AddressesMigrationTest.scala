package testutils.flyway

import slick.jdbc.PostgresProfile.api._

case object AddressesMigrationTest extends FlywayMigrationsTest("5.20170607110159") {

  val addressesColumns = sql"select column_name from INFORMATION_SCHEMA.COLUMNS where table_name='addresses'"

  override def testBeforeMigration(implicit db: Database): Unit = {
    addressesColumns
      .as[String]
      .gimme mustNot contain("cord_ref") // simple sanity check before and after

    runSql("/sql/addresses_live_data.sql")
  }

  override def testAfterMigration(implicit db: Database): Unit = {
    addressesColumns
      .as[String]
      .gimme must contain("cord_ref")

    def addressMustBeMigrated(cord: String, name: String): Unit =
      sql"select name from addresses where cord_ref = $cord".as[String].gimme.head must === (name)

    addressMustBeMigrated("BR10007", "Marilyne Heidenreich")
    addressMustBeMigrated("BR10028", "Velma Quitzon")
    addressMustBeMigrated("BR10066", "Nannie Nicolas")
  }

}
