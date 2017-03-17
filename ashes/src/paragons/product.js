/**
 * @flow
 */

// libs
import _ from 'lodash';
import { assoc, dissoc, update } from 'sprout-data';

// helpers
import { generateSkuCode } from './sku';
import { getJWT } from 'lib/claims';
import * as t from 'paragons/object-types';

// types
import type { JWT } from 'lib/claims';

// we should identity sku be feCode first
// because we want to persist sku even if code has been changes
export function skuId(sku: Sku): string {
  return sku.feCode || _.get(sku.attributes, 'code.v');
}

// THIS IS A HAAAAACK.
function isMerchant(): boolean {
  const jwt = getJWT();
  if (jwt != null && jwt.scope == '1') {
    return false;
  }

  return true;
}

export function createEmptyProduct(): Product {
  let product = {
    productId: null,
    attributes: {
      title: t.string(''),
    },
    skus: [],
    context: { name: 'default' },
    variants: [],
  };

  if (isMerchant()) {
    const merchantAttributes = {
      attributes: {
        description: { t: 'richText', v: '' },
        shortDescription: { t: 'string', v: '' },
        externalUrl: { t: 'string', v: '' },
        externalId: { t: 'string', v: '' },
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

    product = { ...product, ...merchantAttributes };
  }

  return configureProduct(addEmptySku(product));
}

export function duplicateProduct(product: Product): Product {
  const cleared = dissoc(product, 'id');

  return update(cleared, 'albums', _.map, album => {
    const cleared = dissoc(album, 'id', 'createdAt', 'updatedAt');

    return update(cleared, 'images', _.map, image => dissoc(image, 'id'));
  });
}

export function createEmptySku(): Object {
  const pseudoRandomCode = generateSkuCode();
  const emptyPrice = t.price({ currency: 'USD', value: 0 });

  let emptySku = {
    feCode: pseudoRandomCode,
    attributes: {
      code: t.string(''),
      title: t.string(''),
      retailPrice: emptyPrice,
      salePrice: emptyPrice,
    },
  };

  if (isMerchant()) {
    const merchantAttributes = {
      attributes: {
        externalId: t.string(''),
        mpn: t.string(''),
        gtin: t.string(''),
        weight: t.string(''),
        height: t.string(''),
        width: t.string(''),
        depth: t.string(''),
      },
    };

    emptySku = { ...emptySku, ...merchantAttributes };
  }

  return emptySku;
}

export function addEmptySku(product: Product): Product {
  const emptySku = createEmptySku();
  const newSkus = [emptySku, ...product.skus];

  return assoc(product, 'skus', newSkus);
}

/**
 * Takes the FullProduct response from the API and ensures at least one sku
 * have been included.
 *
 * @param {Product} product The full product response from Phoenix.
 *
 * @return {Product}
 */
export function configureProduct(product: Product): Product {
  return ensureProductHasSkus(product);
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
                                value: any): Product {

  const updateAttribute = sku => {
    const skuCode = _.get(sku, 'attributes.code.v');

    return (skuCode == code || sku.feCode == code)
      ? assoc(sku, ['attributes', label, 'v'], value)
      : sku;
  };

  const newSkus = product.skus.map(sku => updateAttribute(sku));

  return assoc(product, 'skus', newSkus);
}
