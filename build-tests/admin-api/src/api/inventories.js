
// @class Inventories
// Accessible via [inventories](#foxapi-inventories) property of [FoxApi](#foxapi) instance.

import endpoints from '../endpoints';

export default class Inventories {
  constructor(api) {
    this.api = api;
  }

  /**
   * @method get(skuCode: String): Promise<InventoryResponse>
   * Get SKU inventory by code.
   */
  get(skuCode) {
    return this.api.get(endpoints.inventory(skuCode));
  }

  /**
   * @method getShipments(referenceNumber: String): Promise<ShipmentResponse[]>
   * Get shipments by reference number.
   */
  getShipments(referenceNumber) {
    return this.api.get(endpoints.inventoryShipments(referenceNumber));
  }

  /**
   * @method increment(stockItemId: Number, payload: ModifyInventoryItemQuantityPayload): Promise
   * Increment inventory item quantity.
   */
  increment(stockItemId, payload) {
    return this.api.patch(endpoints.inventoryIncrement(stockItemId), payload);
  }

  /**
   * @method decrement(stockItemId: Number, payload: ModifyInventoryItemQuantityPayload): Promise
   * Decrement inventory item quantity.
   */
  decrement(stockItemId, payload) {
    return this.api.patch(endpoints.inventoryDecrement(stockItemId), payload);
  }
}
