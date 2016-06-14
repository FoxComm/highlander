/**
 * @flow
 */

import _ from 'lodash';
import { assoc, merge } from 'sprout-data';
import { copyShadowAttributes } from './form-shadow-object';
import { generateSkuCode } from './sku';

import type { SkuForm, SkuShadow } from './sku';

import type {
  Product,
  Attribute,
  Attributes,
  Variant,
} from '../modules/products/details';

// export function getAttribute(formAttrs: Attributes, shadowAttrs: ShadowAttributes,
//   label: string): IlluminatedAttribute {
//
//   const shadow = shadowAttrs[label];
//   if(!shadow) return shadow;
//
//   const attribute = formAttrs[shadow.ref];
//
//   const res = {
//     label: label,
//     type: shadow.type,
//     value: attribute,
//   };
//
//   return res;
// }
//
export function createEmptyProduct(): Product {
  const product = {
    id: null,
    attributes: {},
    skus: [],
  };

  return configureProduct(addEmptySku(product));
}

export function addEmptySku(product: Product): Product {
  const pseudoRandomCode = generateSkuCode();

  const emptyPrice = {
    t: 'price',
    v: { currency: 'USD', value: 0 },
  };

  const emptySku = {
    feCode: pseudoRandomCode,
    attributes: {
      title: {
        t: 'string',
        v: '',
      },
      retailPrice: emptyPrice,
      salePrice: emptyPrice,
    },
  };

  const newSkus = [emptySku, ...product.skus];
  return assoc(product, 'skus', newSkus);
}

/**
 * Takes the FullProduct response from the API and ensures that default attributes
 * have been included.
 * @param {FullProduct} product The full product response from Phoenix.
 * @return {FullProduct} Copy of the input that includes all default attributes.
 */
export function configureProduct(product: Product): Product {
  const defaultAttrs = {
    title: 'string',
    description: 'richText',
    url: 'string',
    metaTitle: 'string',
    metaDescription: 'string',
  };

  const defaultSkuAttrs = {
    retailPrice: 'price',
    salePrice: 'price',
    upc: 'string',
  };

  const newProduct: Product = _.reduce(defaultAttrs, (res, val, key) => {
    if (_.get(res, ['attributes', key])) {
      return res;
    }

    const attr = val == 'price'
      ? { t: val, v: { currency: 'USD', value: null } }
      : { t: val, v: null };

    return assoc(res, 'attributes', {
      [key]: attr,
      ...res.attributes,
    });
  }, product);

  return newProduct;
}

export function setSkuAttribute(product: Product,
                                code: string,
                                label: string,
                                type: string,
                                value: string): Product {
  const updateCode = sku => {
    return (sku.code == code || sku.feCode == code)
      ? { ...sku, code: value }
      : sku;
  };

  const attrPath = type == 'price'
    ? ['attributes', label, 'v', 'value']
    : ['attributes', label, 'v'];

  const updateAttribute = sku => {
    return (sku.code == code || sku.feCode == code)
      ? assoc(sku, attrPath, value)
      : sku;
  };

  const updateFn = label == 'code' ? updateCode : updateAttribute;
  const newSkus = product.skus.map(sku => updateFn(sku));
  return assoc(product, 'skus', newSkus);
}
