package models.Merchant

case class MerchantConfiguration(id: Int = 0,
                                 inventorySourceType: Merchant.InventorySourceType = InventorySourceType.FoxCommerce,
                                 inventorySync: Boolean = false, // is polling enabled?
                                 inventoryPollingInterval: Int = 3600, //in seconds
                                 inventorySourceName: Option[String], // Magento, NetSuite, etc.
                                 inventorySourceVersion: Option[String], // 1.39, 2.0, etc.
                                 inventorySourceCredentials: Option[String], // API Key or Token)
                                 catalogSync: Boolean = false,
                                 catalogPollingInterval: Int = 3600,
                                 catalogSourceName: Option[String],
                                 catalogSourceVersion: Option[String],
                                 catalogSourceCredentials: Option[String])
                                 


/* Long-Term To-Do: Have a fancy database of all inventorySources and versions.  
 * Kind of like with regions. */

object MerchantConfiguration {

  sealed trait InventorySourceType

  
  // They manage inventory locally in FoxCommerce
  case object FoxCommerce extends InventorySource
  // Fox retrieves the inventory from their ERP or WMS
  case object ERP extends InventorySource
  case object WMS extends InventorySource
  // Fox retrieves the inventory from their legacy/existing eCommerce platform
  case object LegacyEcommerce extends InventorySource
}
