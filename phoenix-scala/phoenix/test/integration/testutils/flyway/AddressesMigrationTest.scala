package testutils.flyway

import slick.jdbc.PostgresProfile.api._

case object AddressesMigrationTest extends FlywayMigrationsTest("5.20170607110159") {

  val addressCordExist = sql"select count(1) from information_schema.tables where table_name='address_cord'"

  override def testBeforeMigration(implicit db: Database): Unit = {
    // sanity check: ensure that new table was added
    addressCordExist.as[Int].gimme.head must === (0)
    runSql("/sql/addresses_live_data.sql")
  }

  override def testAfterMigration(implicit db: Database): Unit = {
    addressCordExist.as[Int].gimme.head must === (1)

    def addressMustBeMigrated(cord: String, name: String, address: String): Unit =
      sql"select a.name, a.address1 from addresses as a inner join address_cord as ac on ac.address_id = a.id where ac.cord_ref = $cord"
        .as[(String, String)]
        .gimme
        .head must === (name, address)

    addressMustBeMigrated("BR10067", "Verner Nitzsche", "Zboncak")
    addressMustBeMigrated("BR10068", "Verner Nitzsche", "Zboncak")
    addressMustBeMigrated("BR10046", "Dr. Kennith Flatley", "Bernier")
    addressMustBeMigrated("BR10026", "Ms. Layla Donnelly", "Boyer") // have address2 == null, still must be migrated

    // for each of the following cords we should have strictly one address id
    val p = sql"select distinct(address_id) from address_cord where cord_ref in ('BR10045','BR10049','BR10048','BR10047','BR10046')"
      .as[Int]
      .gimme
      .length === (1)

  }

}
