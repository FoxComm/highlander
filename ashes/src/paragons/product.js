/**
 * @flow
 */

// libs
import _ from 'lodash';
import { assoc, dissoc, update } from 'sprout-data';

// helpers
import { generateSkuCode } from './product-variant';
import { getJWT } from 'lib/claims';
import * as t from 'paragons/object-types';

// types
import type { ProductVariant } from 'modules/product-variants/details';
import type { ObjectView } from './object';

export type OptionValue = {
  name: string,
  swatch: ?string,
  image: ?string,
  skus: Array<string>,
  variantIds?: Array<number>,
};

export type Option = {
  attributes?: {
    name: {t: string, v: string},
    type?: {t: string, v: string},
  },
  values: Array<OptionValue>,
};

// exported types
export type Product = ObjectView & {
  id?: number,
  variants: Array<ProductVariant>,
  options: Array<Option>,
};

// we should identity sku be feCode first
// because we want to persist sku even if code has been changes
export function productVariantCode(productVariant: ProductVariant): string {
  return productVariant.feCode || _.get(productVariant.attributes, 'code.v');
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
    attributes: {
      title: t.string(''),
    },
    variants: [],
    context: { name: 'default' },
    options: [],
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

  return configureProduct(addEmptyProductVariant(product));
}

export function duplicateProduct(product: Product): Product {
  const cleared = dissoc(product, 'id');

  return update(cleared, 'albums', _.map, album => {
    const cleared = dissoc(album, 'id', 'createdAt', 'updatedAt');

    return update(cleared, 'images', _.map, image => dissoc(image, 'id'));
  });
}

export function createEmptyProductVariant(): Object {
  const pseudoRandomCode = generateSkuCode();
  const emptyPrice = t.price({ currency: 'USD', value: 0 });

  let emptyVariant = {
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

    emptyVariant = { ...emptyVariant, ...merchantAttributes };
  }

  return emptyVariant;
}

export function addEmptyProductVariant(product: Product): Product {
  const emptyVariant = createEmptyProductVariant();
  const newVariants = [emptyVariant, ...product.variants];

  return assoc(product, 'variants', newVariants);
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
  return ensureProductHasVariants(product);
}

function ensureProductHasVariants(product: Product): Product {
  if (_.isEmpty(product.variants)) {
    return assoc(product,
      'variants', [createEmptyProductVariant()]
    );
  }
  return product;
}

export function setVariantAttribute(product: Product,
                                    code: string,
                                    field: string,
                                    value: any): Product {

  const updateAttribute = variant => {
    const variantCode = _.get(variant, 'attributes.code.v');

    return (variantCode == code || variant.feCode == code)
      ? assoc(variant, ['attributes', field, 'v'], value)
      : variant;
  };

  const newVariants = product.variants.map(variant => updateAttribute(variant));

  return assoc(product, 'variants', newVariants);
}
