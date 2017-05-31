/**
 * @flow
 */

// libs
import _ from 'lodash';
import { assoc, dissoc, update } from 'sprout-data';

// helpers
import { generateSkuCode } from './sku';
import * as t from 'paragons/object-types';

// we should identity sku be feCode first
// because we want to persist sku even if code has been changes
export function skuId(sku: Sku): string {
  return sku.feCode || _.get(sku.attributes, 'code.v');
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

  return configureProduct(addEmptySku(product));
}

export function duplicateProduct(product: Product): Product {
  const cleared = dissoc(product, 'id', 'slug');

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
                                value: any,
                                type: any): Product {

  const updateAttribute = sku => {
    const skuCode = _.get(sku, 'attributes.code.v');

    if (skuCode == code || sku.feCode == code) {
      let newSku = assoc(sku, ['attributes', label, 'v'], value);

      if (type) {
        newSku = assoc(newSku, ['attributes', label, 't'], type);
      }

      return newSku;
    }

    return sku;
  };

  const newSkus = product.skus.map(sku => updateAttribute(sku));

  return assoc(product, 'skus', newSkus);
}
