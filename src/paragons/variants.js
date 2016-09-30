
import _ from 'lodash';
import Wharf from 'entity-wharf';
import { cartesianProductOf } from 'lib/utils';
import { createEmptySku } from './product';
import type { Product } from './product';
import { assoc } from 'sprout-data';
import type { Sku } from 'modules/skus/details';
import invariant from 'invariant';

const dbCache = new Map();

/**
 * This function replaces variants without options from variant array
 * Be careful, it returns list of variant options, not variants theirselves
 */
export function variantValuesWithMultipleOptions(variants: Array<Option>): Array<Object> {
  return _.reduce(variants, (acc, variant) => {
    if (_.isEmpty(variant.values)) {
      return acc;
    }
    return [...acc, variant.values];
  }, []);
}

/**
 * This function generates all available combinations of variant values
 */
export function availableVariantsValues(variants: Array<any>): Array<Object> {
  const opts = variantValuesWithMultipleOptions(variants);
  return cartesianProductOf(...opts);
}


function indexVariants(variants): Object {
  if (!dbCache.has(variants)) {
    const db = Wharf();
    _.each(variants, variant => {
      _.each(variant.values, value => {
        _.each(value.skuCodes, code => {
          db.add({
            sku: code,
            variant: value.name,
          });
        });
      });
    });

    dbCache.set(variants, db);
  }
  return dbCache.get(variants);
}

function skuCode(sku): string {
  const realCode = _.get(sku.attributes, 'code.v');
  return realCode || sku.feCode;
}

export function autoAssignVariants(skus: Array<Sku>, variants) {
  const sortedSkus = [].slice.call(skus);

  const indexedVariants = indexVariants(variants);
  const skuVariantsCount = sku => indexedVariants.q({av: [['sku', skuCode(sku)]]}).length;
  const newVariants = _.cloneDeep(variants);
  const availableValues = availableVariantsValues(newVariants);

  invariant(availableValues.length === skus.length, 'invalid skus length');

  sortedSkus.sort((sku1, sku2) => {
    const variantsCount1 = skuVariantsCount(sku1);
    const variantsCount2 = skuVariantsCount(sku2);
    return variantsCount1 > variantsCount2 ? -1 : 1;
  });

  _.each(sortedSkus, sku => {
    // find most closest variant tuple
    const skuVariants = indexedVariants.q({av: [['sku', skuCode(sku)]]}).map(indexedVariants.get).map(x => x.variant);
    const closestVariantTuple = _.findIndex(availableValues, variantTuple => {
      return _.intersection(skuVariants, variantTuple.map(v => v.name)).length === skuVariants.length;
    });
    const selectedTupleIndex = closestVariantTuple == -1 ? 0 : closestVariantTuple;
    const selectedTuple = availableValues.splice(selectedTupleIndex, 1)[0];

    // make sure all values from selected tuple has this sku
    _.each(selectedTuple, variant => {
      variant.skuCodes = _.uniq([...variant.skuCodes, skuCode(sku)]);
    });
  });

  return newVariants;
}

/**
 * Updates product after variants has updated
 * if allowDuplicate is false also remove skus with same variants
 */
export function updateVariants(
  product: Product,
  variants: Array<Option>,
  allowDuplicate: boolean = false): Product
{
  // what we should do:
  // 1. Add empty skus for new variants if there are not enough
  // 2. Remove skus if there are no more variants for them

  const existsValues = availableVariantsValues(product.variants);
  const newValues = availableVariantsValues(variants);

  const valueIdentity = values => _.map(values, x => x.name).join('\u008b');

  const addedValues = _.differenceBy(newValues, existsValues, valueIdentity);
  const deletedValues = _.differenceBy(existsValues, newValues, valueIdentity);

  let notEnoughSkus = newValues.length - product.skus.length;
  const newSkus = [];
  while (notEnoughSkus-- > 0) {
    const emptySku = createEmptySku();
    newSkus.push(emptySku);
  }


}
