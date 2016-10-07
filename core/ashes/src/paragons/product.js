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
import { getJWT } from 'lib/claims';

// types
import type { Sku } from 'modules/skus/details';
import type { Dictionary } from './types';
import type { Attribute, Attributes } from './object';
import type { JWT } from 'lib/claims';

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
  context: Context,
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

// THIS IS A HAAAAACK.
function isMerchant(): boolean {
  const jwt = getJWT();
  if (jwt.email == 'admin@admin.com') {
    return false;
  }

  return true;
}

export function createEmptyProduct(): Product {
  let product = {
    id: null,
    productId: null,
    attributes: {
      title: {t: 'string', v: ''},
    },
    skus: [],
    context: {name: 'default'},
  };

  if (isMerchant()) {
    const merchantAttributes = {
      attributes: {
        description: {t: 'richText', v: ''},
        shortDescription: { t: 'string', v: '' },
        externalUrl: {t: 'string', v: ''},
        externalId: {t: 'string', v: ''},
        type: { t: 'string', v: '' },
        vendor: { t: 'string', v: '' },
        manufacturer: { t: 'string', v: '' },
        audience: { t: 'string', v: '' },
        permalink: { t: 'string', v: '' },
        handle: { t: 'string', v: '' },
        manageInventory: { t: 'bool', v: true },
        backordersAllowed: { t: 'bool', v: false },
        featured: { t: 'bool', v: false },
      },
    };

    product = {...product, ...merchantAttributes };
  }

  return configureProduct(addEmptySku(product));
}

export function addEmptySku(product: Product): Product {
  const pseudoRandomCode = generateSkuCode();

  const emptyPrice = {
    t: 'price',
    v: { currency: 'USD', value: 0 },
  };

  let emptySku = {
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

  if (isMerchant()) {
    const merchantAttributes = {
      attributes: {
        externalId: {t: 'string', v: ''},
        mpn: { t: 'string', v: '' },
        gtin: { t: 'string', v: '' },
        weight: { t: 'string', v: '' },
        height: { t: 'string', v: '' },
        width: { t: 'string', v: '' },
        depth: { t: 'string', v: '' },
      },
    };

    emptySku = { ...emptySku, ...merchantAttributes };
  }

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
  }, product);
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
    const code = _.get(sku, 'attributes.code.v');

    return (code == code || sku.feCode == code)
      ? assoc(sku, attrPath, val)
      : sku;
  };

  const newSkus = product.skus.map(sku => updateAttribute(sku));

  return assoc(product, 'skus', newSkus);
}
