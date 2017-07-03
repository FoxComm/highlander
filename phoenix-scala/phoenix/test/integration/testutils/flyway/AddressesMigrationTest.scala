package testutils.flyway

import slick.jdbc.PostgresProfile.api._

case object AddressesMigrationTest extends FlywayMigrationsTest("5.20170607110159") {

  type ShippingAddressTuple = (String, String, String, String)

  val addressCordExist = sql"select count(1) from information_schema.tables where table_name='address_cord'"
  var existingTuple    = Vector.empty[ShippingAddressTuple]

  override def testBeforeMigration(implicit db: Database): Unit = {
    // sanity check: ensure that new table was added
    addressCordExist.as[Int].gimme.head must === (0)

    runSql("/sql/addresses_live_data.sql")

    existingTuple =
      sql"select cord_ref, name, address1, city from order_shipping_addresses".as[ShippingAddressTuple].gimme
  }

  override def testAfterMigration(implicit db: Database): Unit = {
    addressCordExist.as[Int].gimme.head must === (1)

    // for each of the following cords we should have strictly one address id
    val p = sql"select distinct(address_id) from address_cord where cord_ref in ('BR10045','BR10049','BR10048','BR10047','BR10046')"
      .as[Int]
      .gimme
      .length === (1)

    val migratedTuple =
      sql"select ac.cord_ref, a.name, a.address1, a.city from addresses as a inner join address_cord as ac on ac.address_id = a.id"
        .as[ShippingAddressTuple]
        .gimme

    existingTuple.foreach(migratedTuple must contain(_))
  }

}
