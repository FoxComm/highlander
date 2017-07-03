
import type { Sku } from 'modules/product-details';

export type TProductView = {
  title: string,
  description: string,
  images: Array<string>,
  currency: string,
  price: number|string,
  skus: Array<Sku>,
};
