/**
 * @flow
 */

import _ from 'lodash';

import type { FullProduct, ProductAttribute, ProductForm, ProductShadow } from '../modules/products/details';

type Attribute = {
  label: string,
  type: string,
  value: string,
};

type Attributes = { [key:string]: Attribute };

export function getProductAttributes(product: FullProduct): Attributes {
  const form: ?ProductForm = _.get(product, 'form.product');
  const shadow: ?ProductShadow = _.get(product, 'form.shadow');

  if (!form) throw new Error('Product form not found in FullProduct response.');
  if (!shadow) throw new Error('Product shadow not found in FullProduct response.');

  const attributes: Attributes = _.reduce(shadow.attributes, (res, attrValue, key) => {
    const productAttribute = form.attributes[key];

    res[key] = {
      label: key,
      type: productAttribute.type,
      value: productAttribute[attrValue],
    };

    return res;
  }, {});

  return attributes;
}
