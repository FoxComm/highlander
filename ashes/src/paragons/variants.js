
import _ from 'lodash';
import Wharf from 'entity-wharf';
import { cartesianProductOf } from 'lib/utils';
import { createEmptySku, productVariantId } from './product';
import type { Product } from './product';
import { assoc } from 'sprout-data';
import type { Sku } from 'modules/skus/details';
import type { Option, OptionValue } from 'paragons/product';

const dbCache = new Map();

/**
 * This is a convenience function that iterates through a product and creates a
 * mapping from Variant(SKU) => Option => OptionValue.
 * For example SKU-BRO => Size => L.
 */
export function mapVariantsToOptions(options: Array<Option>): Object {
  return _.reduce(options, (res, option) => {
    const optionName = _.get(option, 'attributes.name.v');
    return _.reduce(option.values, (res, value) => {
      return _.reduce(value.skuCodes, (res, skuCode) => {
        return assoc(res, [skuCode, optionName], value.name);
      }, res);
    }, res);
  }, {});
}


/**
 * This function replaces options without option values from option array
 * Returns list of options with one or more value
 */
export function optionsWithMultipleValues(options: Array<any>): Array<Object> {
  return _.reduce(options, (acc, option) => {
    if (_.isEmpty(option.values)) {
      return acc;
    }
    return acc.concat([option]);
  }, []);
}

/**
 * This function replaces options without option values from option array
 * Be careful, it returns list of option values, not options theirselves
 */
export function optionValuesWithMultipleValues(options: Array<Option>): Array<Array<OptionValue>> {
  return _.reduce(options, (acc, option) => {
    if (_.isEmpty(option.values)) {
      return acc;
    }
    return [...acc, option.values];
  }, []);
}

/**
 * This function generates all available combinations of variant values
 */
export function allOptionsValues(variants: Array<Option>): Array<Object> {
  const opts = optionValuesWithMultipleValues(variants);
  return cartesianProductOf(...opts);
}

function indexBySku(options: Array<Option>): Object {
  if (!dbCache.has(options)) {
    const db = Wharf();
    _.each(options, option => {
      _.each(option.values, value => {
        _.each(value.skuCodes, code => {
          db.add({
            sku: code,
            variant: value,
          });
        });
      });
    });

    dbCache.set(options, db);
  }
  return dbCache.get(options);
}

function indexByName(variants: Array<Option>): Object {
  return _.reduce(variants, (acc, variant) => {
    return _.reduce(variant.values, (acc, value) => {
      acc[value.name] = value;
      return acc;
    }, acc);
  }, {});
}

function maxIndexBy(collection: Array<any>, iteratee: (item: any) => number, skipIndexes: Array<number> = []) {
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
function findClosestTuples(smallCartesian: Array<Array<any>>,
                           bigCartesian: Array<Array<any>>,
                           identity: (item: any) => string) {
  return _.reduce(smallCartesian, (acc, tuple) => {
    const smallValues = tuple.map(identity);
    const foundIndex = maxIndexBy(bigCartesian, t => _.intersection(smallValues, t.map(identity)).length, acc);
    return [...acc, foundIndex];
  }, []);
}

export function deleteVariantCombination(product: Product, code: string): Product {
  const newVariants = _.filter(product.variants, sku => productVariantId(sku) != code);
  const newOptions = _.cloneDeep(product.options);
  _.each(newOptions, option => {
    _.each(option.values, optionValue => {
      optionValue.skuCodes = _.filter(optionValue.skuCodes, boundSku => boundSku != code);
    });
  });

  if (!newOptions.length && !newVariants.length) {
    newVariants.push(createEmptySku());
  }

  return assoc(product,
    'variants', newVariants,
    'options', newOptions
  );
}

export function addSkusForVariants(product: Product, variantTuples: Array<Array<OptionValue>>) {
  const newOptions = _.cloneDeep(product.options);
  const indexedOptions = indexByName(newOptions);

  const newSkus = _.map(variantTuples, tuple => {
    const sku = createEmptySku();
    _.each(tuple, variantValue => {
      const value = indexedOptions[variantValue.name];

      bindSkuToVariantsTuple([value], sku);
    });
    return sku;
  });

  return assoc(product,
    'variants', [...product.variants, ...newSkus],
    'options', newOptions
  );
}

export function availableOptionsValues(product: Product): Array<Array<OptionValue>> {
  const indexedOptions = indexBySku(product.options);
  const allOptions = allOptionsValues(product.options);
  const existsVariants = _.map(product.variants, variant => {
    return indexedOptions.q({av: [['sku', productVariantId(variant)]]}).map(indexedOptions.get).map(x => x.variant);
  });

  const identity = value => value.name;

  return _.filter(allOptions, t => {
     return !_.some(existsVariants,
       values => _.intersection(values.map(identity), t.map(identity)).length === t.length
     );
  });
}

function bindSkuToVariantsTuple(tuple: Array<OptionValue>, sku: string): void {
  _.each(tuple, variantValue => {
    variantValue.skuCodes = _.uniq([...variantValue.skuCodes, productVariantId(sku)]);
  });
}

export function autoAssignOptions(product: Product, options: Array<Option>): Product {
  const existingVariants: Array<Sku> = product.variants;
  const indexedOptions = indexBySku(options);
  const newOptions = _.cloneDeep(options);
  const availableValues = allOptionsValues(newOptions);
  // here we assume that there is defined sku (even with feCode only) for each variant
  const existsValues = _.map(existingVariants, sku => {
    return indexedOptions.q({av: [['sku', productVariantId(sku)]]}).map(indexedOptions.get).map(x => x.variant);
  });

  let closestTuples;
  let newSkus = [];

  const unbindAll = () => {
    // unbind all skus
    _.each(newOptions, option => {
      _.each(option.values, value => {
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
      newSkus.push(existingVariants[i]);
      lastUsedSkuIndex = i;

      bindSkuToVariantsTuple(availableValues[selectedTupleIndex], existingVariants[i]);
    }
    closestTuples.sort();
    const oldOptionsByName = indexByName(product.options);

    for (let i = 0; i < availableValues.length; i++) {
      if (_.sortedIndexOf(closestTuples, i) !== -1) continue;

      const thereIsNewOptions = _.some(availableValues[i], value => {
        return !(value.name in oldOptionsByName);
      });
      if (!thereIsNewOptions) continue;

      const sku = lastUsedSkuIndex != null && lastUsedSkuIndex < existingVariants.length - 1
        ? existingVariants[++lastUsedSkuIndex] : createEmptySku();
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
      const boundSku = existingVariants[closestTuples[i]];

      newSkus.push(boundSku);
      bindSkuToVariantsTuple(selectedTuple, boundSku);
    }
  }

  return assoc(product,
    'variants', newSkus,
    'options', newOptions
  );
}
