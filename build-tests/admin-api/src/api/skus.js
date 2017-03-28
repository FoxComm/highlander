
// @class Skus
// Accessible via [skus](#foxapi-skus) property of [FoxApi](#foxapi) instance.

import endpoints from '../endpoints';

export default class Skus {
  constructor(api) {
    this.api = api;
  }

  /**
   * @method create(context: String, sku: SkuPayload): Promise<SkuResponse>
   * Create new SKU.
   */
  create(context, sku) {
    return this.api.post(endpoints.skus(context), sku);
  }

  /**
   * @method one(context: String, skuCode: String): Promise<SkuResponse>
   * Find SKU by code.
   */
  one(context, skuCode) {
    return this.api.get(endpoints.sku(context, skuCode));
  }

  /**
   * @method update(context: String, skuCode: String, sku: SkuPayload): Promise<SkuResponse>
   * Update SKU details.
   */
  update(context, skuCode, sku) {
    return this.api.patch(endpoints.sku(context, skuCode), sku);
  }

  /**
   * @method archive(context: String, skuCode: String): Promise
   * Archive SKU.
   */
  archive(context, skuCode) {
    return this.api.delete(endpoints.sku(context, skuCode));
  }

  /**
   * @method inventory(skuCode: String): Promise<InventoryResponse>
   * Lookup SKU inventory.
   */
  inventory(skuCode) {
    return this.api.get(endpoints.skuInventory(skuCode));
  }
}
