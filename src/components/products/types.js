/**
 * @flow
 */
import type { Product } from '../../modules/products/details';

export type DetailsParams = {
  productId: number,
  product: ?Product,
};
