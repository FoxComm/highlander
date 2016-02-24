/**
 * @flow
 */

import _ from 'lodash';

import type { ProductResponse } from '../modules/products/sample-products';

export type ProductAttribute = { type: string, value: any };
export type ProductAttributes = { [key:string]: ProductAttribute };

export function getProductAttributes(contextId: number, resp: ProductResponse): ProductAttributes {
  const { product, shadows } = resp;
  const attributes = _.reduce(product.attributes, (result, attribute, key) => {
    const shadow = _.find(shadows, { 'productContextId': contextId });
    const shadowVal = _.get(shadow, ['attributes', key]);

    if (shadowVal) {
      result[key] = {
        type: attribute.type,
        value: attribute[shadowVal],
      };
    }

    return result;
  }, {});

  return attributes;
}
