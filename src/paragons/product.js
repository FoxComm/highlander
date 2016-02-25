/**
 * @flow
 */

import _ from 'lodash';

import type { Attribute, ProductShadow, ProductResponse } from '../modules/products/sample-products';

export type ProductAttribute = { type: string, value: any };
export type ProductAttributes = { [key:string]: ProductAttribute };

export function getProductTitle(contextId: number, resp: ProductResponse): string {
  const { product, shadows } = resp;
  const shadow = _.find(shadows, { 'productContextId': contextId });
  const attribute = _.get(product, ['attributes', 'title']);
  const baked = getBakedAttribute('title', attribute, shadow);
  return _.get(baked, 'value', '');
}

export function getProductAttributes(contextId: number, resp: ProductResponse): ProductAttributes {
  const { product, shadows } = resp;
  const shadow = _.find(shadows, { 'productContextId': contextId });
  const attributes = _.reduce(product.attributes, (result, attribute, key) => {
    const prodAttr = getBakedAttribute(key, attribute, shadow);

    if (prodAttr) {
      result[key] = prodAttr;
    }

    return result;
  }, {});

  return attributes;
}

function getBakedAttribute(key: string, 
                           attribute: Attribute,
                           shadow: ProductShadow): ?ProductAttribute {
  const shadowVal = _.get(shadow, ['attributes', key]);

  if (attribute && shadowVal) {
    return { type: attribute.type, value: attribute[shadowVal] };
  } else {
    return null;
  }
}
