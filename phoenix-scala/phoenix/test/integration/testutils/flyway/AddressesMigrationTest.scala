package testutils.flyway

import slick.jdbc.PostgresProfile.api._

case object AddressesMigrationTest extends FlywayMigrationsTest("5.20170607110159") {

  val addressesColumns = sql"select column_name from INFORMATION_SCHEMA.COLUMNS where table_name='addresses'"

  override def testBeforeMigration(implicit db: Database): Unit = {
    addressesColumns
      .as[String]
      .gimme mustNot contain("cord_ref")

    sql"""
          insert into carts (account_id, reference_number, created_at, updated_at, currency, scope) values (1, 'TEST-ABC-1', now(), null, 'USD', '1');
          insert into order_shipping_addresses(cord_ref, region_id, name, address1, address2, city, zip, phone_number, created_at, updated_at)
            values ('TEST-ABC-1', 1, 'Test address', 'Test Case rd.', null, 'Testburg', '125438', '12345678', now(), null);
    """.asUpdate.gimme
  }

  override def testAfterMigration(implicit db: Database): Unit = {
    addressesColumns
      .as[String]
      .gimme must contain("cord_ref")

    sql"""select a.cord_ref from addresses as a""".as[String].gimme must === ("TEST-ABC-1")
  }

}
