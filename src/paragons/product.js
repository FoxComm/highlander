/**
 * @flow
 */

// libs
import _ from 'lodash';
import { assoc } from 'sprout-data';
import { skuEmptyAttributes } from './sku';
import { isSatisfied } from 'paragons/object';

// helpers
import { generateSkuCode, isSkuValid } from './sku';

// types
import type { Sku } from 'modules/skus/details';
import type { Attribute, Attributes } from './object';

type Context = {
  name: string,
  attributes?: {
    lang: string,
    modality: string,
  },
}

// exported types
export type Product = {
  id: ?number,
  productId: ?number,
  attributes: Attributes,
  skus: Array<Sku>,
  variants: Array<Option>,
  context: Context,
};

export type Option = {
  attributes?: {
    name: {
      t: ?string,
      v: ?string,
    },
    type?: {
      t: string,
      v: string,
    },
  },
  values: Array<OptionValue>,
};

export type OptionValue = {
  name: string,
  swatch: ?string,
  image: ?string,
  skuCodes: Array<string>,
};

export const options = {
  title: { required: true },
};

export function isProductValid(product: Product): boolean {
  // Validate all required product fields.
  const validProductKeys: boolean = isSatisfied(product, options);

  if (!validProductKeys) {
    return false;
  }

  // Validate required SKU fields.
  const skus = _.get(product, 'skus', []);
  for (let i = 0; i < skus.length; i++) {
    const validSku = isSkuValid(skus[i]);
    if (!validSku) return false;
  }

  return true;
}

export function createEmptyProduct(): Product {
  const product = {
    id: null,
    productId: null,
    attributes: {
      title: {t: 'string', v: ''},
    },
    skus: [],
    context: {name: 'default'},
    variants: [],
  };

  return configureProduct(addEmptySku(product));
}

export function createEmptySku(): Object {
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
  return emptySku;
}

export function addEmptySku(product: Product): Product {
  const emptySku = createEmptySku();

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
  }, ensureProductHasSkus(product));
}

function ensureProductHasSkus(product: Product): Product {
  if (_.isEmpty(product.skus)) {
    return assoc(product,
      'skus', [createEmptySku()]
    );
  }
  return product;
}

export function setSkuAttribute(product: Product,
                                code: string,
                                label: string,
                                value: string): Product {
  const type = _.get(skuEmptyAttributes, [label, 't'], 'string');

  const attrPath = type == 'price'
    ? ['attributes', label, 'v', 'value']
    : ['attributes', label, 'v'];

  const val = type == 'price' ? parseInt(value) : value;

  const updateAttribute = sku => {
    const skuCode = _.get(sku, 'attributes.code.v');

    return (skuCode == code || sku.feCode == code)
      ? assoc(sku, attrPath, val)
      : sku;
  };

  const newSkus = product.skus.map(sku => updateAttribute(sku));

  return assoc(product, 'skus', newSkus);
}
