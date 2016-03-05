/**
 * @flow
 */

import _ from 'lodash';

import type { ProductAttribute, ProductForm, ProductShadow } from '../modules/products/details';

type Attribute = {
  label: string,
  type: string,
  value: string,
};

function getProductAttributes(form: ProductForm, shadow: ProductShadow): { [key:string]: Attribute } {
  const attributes: { [key:string]: Attribute } = _.reduce(shadow.attributes, (res, attribute, key) => {
    const attr: ?ProductAttribute = _.get(form, ['attributes', key]);

    if (!attr) {
      throw new Error(`Attribute ${key} not found in product from id=${form.id}`);
    }

    res[key] = {
      label: key,
      type: attr.type,
      value: attr[attribute],
    };

    return res;
  }, {});

  return attributes;
}

export default class Product {
  productAttributes: { [key:string]: Attribute };

  constructor(form: ProductForm, shadow: ProductShadow) {
    this.productAttributes = getProductAttributes(form, shadow);
  }
}
