
// @class Products
// Accessible via [products](#foxapi-products) property of [FoxApi](#foxapi) instance.

import endpoints from '../endpoints';

export default class Products {
  constructor(api) {
    this.api = api;
  }

  /**
   * @method create(context: String, product: ProductPayload): Promise<ProductResponse>
   * Create new product.
   */
  create(context, product) {
    return this.api.post(endpoints.products(context), product);
  }

  /**
   * @method one(context: String, productId: Number): Promise<ProductResponse>
   * Find product by id.
   */
  one(context, productId) {
    return this.api.get(endpoints.product(context, productId));
  }

  /**
   * @method update(context: String, productId: Number, product: ProductPayload): Promise<ProductResponse>
   * Update product details.
   */
  update(context, productId, product) {
    return this.api.patch(endpoints.product(context, productId), product);
  }

  /**
   * @method archive(context: String, productId: Number): Promise
   * Archive product.
   */
  archive(context, productId) {
    return this.api.delete(endpoints.product(context, productId));
  }
}
