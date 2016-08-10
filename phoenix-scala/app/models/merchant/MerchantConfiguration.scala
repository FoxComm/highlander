package models.Merchant

/* The primary weakness with this approach is that we don't have a clear sense of types for credentials
 * We are not yet sure which types are going to be required for third-parties like Magento, WMSs, etc.
 */

case class MerchantConfiguration(id: Int = 0,
                                 merchantId: Int,
                                 environment: Merchant.EnvironmentType = EnvironmentType.Staging,
                                 inventorySourceType: Merchant.SourceOrDestinationType = SourceOrDestinationType.FoxCommerce,
                                 inventorySync: Boolean = false, // is polling enabled?
                                 inventoryPollingInterval: Int = 3600, //in seconds
                                 inventorySourceName: Option[String], // Magento, NetSuite, etc.
                                 inventorySourceURL: Option[String],
                                 inventorySourceVersion: Option[String], // 1.39, 2.0, etc.
                                 inventorySourceCredentials: Option[String], // API Key or Token)
                                 catalogSync: Boolean = false,
                                 catalogPollingInterval: Int = 3600,
                                 catalogSourceName: Option[String],
                                 catalogSourceURL: Option[String],
                                 catalogSourceVersion: Option[String],
                                 catalogSourceCredentials: Option[String],
                                 fulfillmentType: Merchant.FulfillmentType = SourceOrDestinationType.FoxCommerce,
                                 fulfillmentDestinationName: Option[String], 
                                 fulfillmentDestinationURL: Option[String],
                                 fulfillmentDestinationVersion: Option[String],
                                 fulfillmentDestinationCredentials: Option[String]
                                 )

/* One important question is around global permissions for certain kinds of resources.  
 * We will have fine-grained access on a per-account basis, but only for resources where global access is allowed.
 * We'll have to explore where we want that to live.  Global could live in the same place as local access.
 * Ex. Orders.  Merchant 1 may not have access to Orders or Customers.  Whereas Merchant2 maybe allowed both.
 * This might be a global configuration, though.  And apply to all vendors?
 */



/* Long-Term To-Do: Have a fancy database of all inventorySources and versions.  
 * Kind of like with regions. */

object MerchantConfiguration {

  sealed trait SourceOrDestinationType
  // They manage inventory locally in FoxCommerce
  case object FoxCommerce extends InventorySource
  // Fox retrieves the inventory from their ERP or WMS
  case object ERP extends InventorySource
    case object Netsuite extends ERP 
  case object WMS extends InventorySource
    case object ShipStation extends WMS
  // Fox retrieves the inventory from their legacy/existing eCommerce platform
  case object LegacyEcommerce extends InventorySource

  sealed trait EnvironmentType
  case object Staging extends EnvironmentType
  case object Production extends EnvironmentType
  case object Test extends EnvironmentType
 
}
