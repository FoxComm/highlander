/**
 * @flow
 */
import type { ProductResponse } from '../../modules/products/sample-products';

export type DetailsParams = {
  productId: number,
  product: ?ProductResponse,
};
