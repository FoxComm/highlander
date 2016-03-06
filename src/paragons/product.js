/**
 * @flow
 */

import _ from 'lodash';
import { assoc } from 'sprout-data';

import type { FullProduct, ProductAttribute, ProductForm, ProductShadow } from '../modules/products/details';

type Attribute = {
  label: string,
  type: string,
  value: string,
};

type Attributes = { [key:string]: Attribute };

function getProductForm(product: FullProduct): ProductForm {
  const form: ?ProductForm = _.get(product, 'form.product');
  if (!form) throw new Error('Product form not found in FullProduct response.');
  return form;
}

function getProductShadow(product: FullProduct): ProductShadow {
  const shadow: ?ProductShadow = _.get(product, 'shadow.product');
  if (!shadow) throw new Error('Product shadow not found in FullProduct response.');
  return shadow;
}

/**
 * Extracts all of the attributes from a FullProduct by combining the ProductForm
 * and ProductShadow. This is essentially the client-side view of the illuminated
 * product.
 * @param {FullProduct} product The full product response from Phoenix.
 * @return {Attributes} Illuminated list of attributes.
 */
export function getProductAttributes(product: FullProduct): Attributes {
  const form = getProductForm(product);
  const shadow = getProductShadow(product);

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

/**
 * Takes the FullProduct response from the API and ensures that default attributes
 * have been included.
 * @param {FullProduct} product The full product response from Phoenix.
 * @return {FullProduct} Copy of the input that includes all default attributes.
 */
export function configureProduct(product: FullProduct): FullProduct {
  const defaultAttrs = {
    title: 'string',
    description: 'string',
    retailPrice: 'price',
    salePrice: 'price',
    url: 'string',
    metaTitle: 'string',
    metaDescription: 'string',
  };

  const newProduct: FullProduct = _.reduce(defaultAttrs, (res, val, key) => {
    return addProductAttribute(res, key, val);
  }, product);

  return newProduct;
}

export function addProductAttribute(product: FullProduct,
                                    label: string,
                                    type: string,
                                    context: string = 'default'): FullProduct {

  const formValue = type == 'price' ? { currency: 'USD', value: null } : null;
  const newFormAttr = { [label]: { type: type, default: formValue } };
  const newShadowAttr = { [label]: 'default' };

  const formAttrs = _.get(product, 'form.product.attributes', {});
  const shadowAttrs = _.get(product, 'shadow.product.attributes', {});

  return assoc(product,
    ['form', 'product', 'attributes'], { ...newFormAttr, ...formAttrs },
    ['shadow', 'product', 'attributes'], { ...newShadowAttr, ...shadowAttrs }
  );
}


export function setProductAttribute(product: FullProduct, label: string, value: string): FullProduct {
  const path = ['form', 'product', 'attributes', label];
  const attribute = _.get(product, path);

  if (!attribute) {
    throw new Error(`Attribute=${label} for product id=${product.id} not found.`);
  }

  switch (attribute.type) {
    case 'price':
      return assoc(product, [...path, 'default', 'value'], value);
    default:
      return assoc(product, [...path, 'default'], value);
  }
}
