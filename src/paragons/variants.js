
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
export function allVariantsValues(variants: Array<any>): Array<Object> {
  const opts = variantValuesWithMultipleOptions(variants);
  return cartesianProductOf(...opts);
}

function indexBySku(variants): Object {
  if (!dbCache.has(variants)) {
    const db = Wharf();
    _.each(variants, variant => {
      _.each(variant.values, value => {
        _.each(value.skuCodes, code => {
          db.add({
            sku: code,
            variant: value,
          });
        });
      });
    });

    dbCache.set(variants, db);
  }
  return dbCache.get(variants);
}

function indexByName(variants): Object {
  return _.reduce(variants, (acc, variant) => {
    return _.reduce(variant.values, (acc, value) => {
      acc[value.name] = value;
      return acc;
    }, acc);
  }, {});
}

function skuCode(sku): string {
  const realCode = _.get(sku.attributes, 'code.v');
  return realCode || sku.feCode;
}

function maxIndexBy(collection, iteratee, skipIndexes = []) {
  let maxValue = -Infinity;
  let maxIndex = -1;
  skipIndexes = [].slice.call(skipIndexes).sort();
  for (let i = 0; i < collection.length; i++) {
    if (_.sortedIndexOf(skipIndexes, i) !== -1) continue;
    const val = iteratee(collection[i]);
    if (val > maxValue) {
      maxValue = val;
      maxIndex = i;
    }
  }
  return maxIndex;
}

// returns tuple indexes of bigCartestian which are most closest to smallCartesian tuples
// offsets in result are corresponding to offsets in smallCartesian
// result array has same length as smallCartesian
function findClosestTuples(smallCartesian, bigCartesian, identity) {
  return _.reduce(smallCartesian, (acc, tuple) => {
    const smallValues = tuple.map(identity);
    const foundIndex = maxIndexBy(bigCartesian, t => _.intersection(smallValues, t.map(identity)).length, acc);
    return [...acc, foundIndex];
  }, []);
}

export function deleteVariantCombination(product, code) {
  const newSkus = _.filter(product.skus, sku => skuCode(sku) != code);
  const newVariants = _.cloneDeep(product.variants);
  _.each(newVariants, variant => {
    _.each(variant.values, variantValue => {
      variantValue.skuCodes = _.filter(variantValue.skuCodes, boundSku => boundSku != code);
    });
  });

  return assoc(product,
    'skus', newSkus,
    'variants', newVariants
  );
}

export function addSkusForVariants(product, variantTuples) {
  const newVariants = _.cloneDeep(product.variants);
  const indexedVariants = indexByName(newVariants);

  const newSkus = _.map(variantTuples, tuple => {
    const sku = createEmptySku();
    _.each(tuple, variantValue => {
      const value = indexedVariants[variantValue.name];

      bindSkuToVariantsTuple([value], sku);
    });
    return sku;
  });

  return assoc(product,
    'skus', [...product.skus, ...newSkus],
    'variants', newVariants
  );
}

export function availableVariantsValues(product) {
  const indexedVariants = indexBySku(product.variants);
  const allVariants = allVariantsValues(product.variants);
  const existsVariants = _.map(product.skus, sku => {
    return indexedVariants.q({av: [['sku', skuCode(sku)]]}).map(indexedVariants.get).map(x => x.variant);
  });

  const identity = value => value.name;

  return _.filter(allVariants, t => {
     return !_.some(existsVariants,
       values => _.intersection(values.map(identity), t.map(identity)).length === t.length
     );
  });
}

function bindSkuToVariantsTuple(tuple, sku) {
  _.each(tuple, variantValue => {
    variantValue.skuCodes = _.uniq([...variantValue.skuCodes, skuCode(sku)]);
  });
}

export function autoAssignVariants(product, variants) {
  const existsSkus: Array<Sku> = product.skus;
  const indexedVariants = indexBySku(variants);
  const newVariants = _.cloneDeep(variants);
  const availableValues = allVariantsValues(newVariants);
  // here we assume that there is defined sku (even with feCode only) for each variant
  const existsValues = _.map(existsSkus, sku => {
    return indexedVariants.q({av: [['sku', skuCode(sku)]]}).map(indexedVariants.get).map(x => x.variant);
  });

  let closestTuples;
  let newSkus = [];

  const unbindAll = () => {
    // unbind all skus
    _.each(newVariants, variant => {
      _.each(variant.values, value => {
        value.skuCodes = [];
      });
    });
  };

  if (availableValues.length >= existsValues.length) {
    // increase case
    closestTuples = findClosestTuples(existsValues, availableValues, x => x.name);
    unbindAll();

    let lastUsedSkuIndex = null;

    for (let i = 0; i < closestTuples.length; i++) {
      const selectedTupleIndex = closestTuples[i];
      newSkus.push(existsSkus[i]);
      lastUsedSkuIndex = i;

      bindSkuToVariantsTuple(availableValues[selectedTupleIndex], existsSkus[i]);
    }
    closestTuples.sort();
    const oldVariantsByName = indexByName(product.variants);

    for (let i = 0; i < availableValues.length; i++) {
      if (_.sortedIndexOf(closestTuples, i) !== -1) continue;

      const thereIsNewVariants = _.some(availableValues[i], value => {
        return !(value.name in oldVariantsByName);
      });
      if (!thereIsNewVariants) continue;

      const sku = lastUsedSkuIndex != null && lastUsedSkuIndex < existsSkus.length - 1
        ? existsSkus[++lastUsedSkuIndex] : createEmptySku();
      newSkus.push(sku);
      bindSkuToVariantsTuple(availableValues[i], sku);
    }
  } else {
    // reduce case
    closestTuples = findClosestTuples(availableValues, existsValues, x => x.name);
    unbindAll();

    // do rest job
    for (let i = 0; i < availableValues.length; i++) {
      const selectedTuple = availableValues[i];
      const boundSku = existsSkus[closestTuples[i]];

      newSkus.push(boundSku);
      bindSkuToVariantsTuple(selectedTuple, boundSku);
    }
  }

  return assoc(product,
    'skus', newSkus,
    'variants', newVariants
  );
}
