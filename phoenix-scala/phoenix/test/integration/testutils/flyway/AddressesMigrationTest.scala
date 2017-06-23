package testutils.flyway

import slick.jdbc.PostgresProfile.api._

case object AddressesMigrationTest extends FlywayMigrationsTest("5.20170607110159") {

  val addressesColumns = sql"select column_name from information_schema.columns where table_name='addresses'"

  def checkAddress(implicit db: Database): Vector[(String, String, String, String)] =
    sql"select address1, address2, city, zip from addresses where name = 'Dr. Kennith Flatley'"
      .as[(String, String, String, String)]
      .gimme

  override def testBeforeMigration(implicit db: Database): Unit = {

    runSql("/sql/addresses_live_data.sql")
    println("before " + checkAddress)
  }

  override def testAfterMigration(implicit db: Database): Unit =
    // fixme !!
//    def addressMustBeMigrated(cord: String, name: String): Unit =
//      sql"select name from addresses where cord_ref = $cord".as[String].gimme.head must === (name)

//    addressMustBeMigrated("BR10007", "Marilyne Heidenreich")
//    addressMustBeMigrated("BR10028", "Velma Quitzon")
//    addressMustBeMigrated("BR10066", "Nannie Nicolas")

    // FIXME merge address duplicates @aafa
    println("after " + checkAddress)

}
