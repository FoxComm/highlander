
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

function findClosestTuples(smallCartesian, bigCartesian, identity) {
  return _.reduce(smallCartesian, (acc, tuple) => {
    const smallValues = tuple.map(identity);
    const foundIndex = maxIndexBy(bigCartesian, t => _.intersection(smallValues, t.map(identity)).length, acc);
    return [...acc, foundIndex];
  }, []);
}

export function autoAssignVariants(existsSkus: Array<Sku>, variants) {
  const indexedVariants = indexVariants(variants);
  const newVariants = _.cloneDeep(variants);
  const availableValues = availableVariantsValues(newVariants);
  const existsValues = _.map(existsSkus, sku => {
    return indexedVariants.q({av: [['sku', skuCode(sku)]]}).map(indexedVariants.get).map(x => {
      return {
        name: x.variant,
      };
    });
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

  const bindTuple = (tuple, sku) => {
    _.each(tuple, variantValue => {
      variantValue.skuCodes = _.uniq([...variantValue.skuCodes, skuCode(sku)]);
    });
  };

  if (availableValues.length >= existsValues.length) {
    closestTuples = findClosestTuples(existsValues, availableValues, x => x.name);
    console.log('closest', closestTuples);
    unbindAll();

    let lastUsedSkuIndex = null;

    for (let i = 0; i < closestTuples.length; i++) {
      const selectedTupleIndex = closestTuples[i];
      newSkus.push(existsSkus[i]);
      lastUsedSkuIndex = i;

      bindTuple(availableValues[selectedTupleIndex], existsSkus[i]);
    }
    closestTuples.sort();

    for (let i = 0; i < availableValues.length; i++) {
      if (_.sortedIndexOf(closestTuples, i) !== -1) continue;

      const sku = lastUsedSkuIndex != null && lastUsedSkuIndex < existsSkus.length
        ? existsSkus[++lastUsedSkuIndex] : createEmptySku();
      newSkus.push(sku);
      bindTuple(availableValues[i], sku);
    }
  } else {
    closestTuples = findClosestTuples(availableValues, existsValues, x => x.name);
    unbindAll();
    let boundTupleIndex;
    let nextBoundTuple = 0;
    for (let i = 0; i < availableValues.length; i++) {
      boundTupleIndex = i in closestTuples ? closestTuples[i] : nextBoundTuple++;
      const selectedTuple = availableValues[i];
      const boundSku = existsSkus[boundTupleIndex];
      newSkus.push(boundSku);
      bindTuple(selectedTuple, boundSku);
    }
  }

  return {
    skus: newSkus,
    variants: newVariants,
  };
}

/**
 * Updates product after variants has updated
 * if allowDuplicate is false also remove skus with same variants
 */
export function updateVariants(product: Product, newVariants: Array<Option>): Product {
  const {skus, variants} = autoAssignVariants(product.skus, newVariants);

  return assoc(product,
    'skus', skus,
    'variants', variants
  );
}
