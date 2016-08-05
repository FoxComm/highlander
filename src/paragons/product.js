/**
 * @flow
 */

// libs
import _ from 'lodash';
import { assoc, merge } from 'sprout-data';

// helpers
import { copyShadowAttributes } from './form-shadow-object';
import { generateSkuCode } from './sku';

// types
import type { Sku } from 'modules/skus/details';
import type { Dictionary } from './types';
import type { Attribute, Attributes } from './object';

// exported types
export type Product = {
  id: ?number,
  productId: ?number,
  attributes: Attributes,
  skus: Array<Sku>,
  context: string,
};

export type Variant = {
  name: ?string,
  type: ?string,
  values: Dictionary<VariantValue>,
};

export type VariantValue = {
  id: number,
  swatch: ?string,
  image: ?string,
};


export function createEmptyProduct(): Product {
  const product = {
    id: null,
    productId: null,
    attributes: {},
    skus: [],
    context: '',
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
      code: {
        t: 'string',
        v: '',
      },
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
 *
 * @param {Product} product The full product response from Phoenix.
 *
 * @return {Product} Copy of the input that includes all default attributes.
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

  return _.reduce(defaultAttrs, (res, val, key) => {
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
}

export function setSkuAttribute(product: Product,
                                code: string,
                                label: string,
                                type: string,
                                value: string): Product {
  const attrPath = type == 'price'
    ? ['attributes', label, 'v', 'value']
    : ['attributes', label, 'v'];

  const val = type == 'price' ? parseInt(value) : value;

  const updateAttribute = sku => {
    const code = _.get(sku, 'attributes.code.v');

    return (code == code || sku.feCode == code)
      ? assoc(sku, attrPath, val)
      : sku;
  };

  const newSkus = product.skus.map(sku => updateAttribute(sku));

  return assoc(product, 'skus', newSkus);
}
