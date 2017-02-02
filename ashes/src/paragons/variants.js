
import _ from 'lodash';
import Wharf from 'entity-wharf';
import { cartesianProductOf } from 'lib/utils';
import { createEmptyProductVariant, productVariantCode } from './product';
import type { Product } from './product';
import { assoc } from 'sprout-data';
import type { ProductVariant } from 'modules/product-variants/details';
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
 * This function generates all available combinations of option values
 */
export function allOptionsValues(variants: Array<Option>): Array<Object> {
  const opts = optionValuesWithMultipleValues(variants);
  return cartesianProductOf(...opts);
}

function indexBySku(options: Array<Option>): Object {
  if (!dbCache.has(options)) {
    const db = Wharf();
    _.each(options, option => {
      _.each(option.values, option => {
        _.each(option.skuCodes, code => {
          db.add({
            sku: code,
            option,
          });
        });
      });
    });

    dbCache.set(options, db);
  }
  return dbCache.get(options);
}

function indexByName(options: Array<Option>): Object {
  return _.reduce(options, (acc, option) => {
    return _.reduce(option.values, (acc, option) => {
      acc[option.name] = option;
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
  const newVariants = _.filter(product.variants, variant => productVariantCode(variant) != code);
  const newOptions = _.cloneDeep(product.options);
  _.each(newOptions, option => {
    _.each(option.values, optionValue => {
      optionValue.skuCodes = _.filter(optionValue.skuCodes, boundSku => boundSku != code);
    });
  });

  if (!newOptions.length && !newVariants.length) {
    newVariants.push(createEmptyProductVariant());
  }

  return assoc(product,
    'variants', newVariants,
    'options', newOptions
  );
}

export function addProductVariantsByOptionTuples(product: Product, optionValueTuples: Array<Array<OptionValue>>) {
  const newOptions = _.cloneDeep(product.options);
  const indexedOptions = indexByName(newOptions);

  const newProductVariants = _.map(optionValueTuples, tuple => {
    const productVariant = createEmptyProductVariant();
    _.each(tuple, optionValue => {
      const value = indexedOptions[optionValue.name];

      bindProductVariantToOptionsTuple([value], productVariant);
    });
    return productVariant;
  });

  return assoc(product,
    'variants', [...product.variants, ...newProductVariants],
    'options', newOptions
  );
}

export function availableOptionsValues(product: Product): Array<Array<OptionValue>> {
  const indexedOptions = indexBySku(product.options);
  const allOptions = allOptionsValues(product.options);
  const existsOptions = _.map(product.variants, variant => {
    return indexedOptions.q({av: [['sku', productVariantCode(variant)]]}).map(indexedOptions.get).map(x => x.option);
  });

  const identity = value => value.name;

  return _.filter(allOptions, t => {
     return !_.some(existsOptions,
       optionValues => _.intersection(optionValues.map(identity), t.map(identity)).length === t.length
     );
  });
}

function bindProductVariantToOptionsTuple(tuple: Array<OptionValue>, productVariant: ProductVariant): void {
  _.each(tuple, optionValue => {
    optionValue.skuCodes = _.uniq([...optionValue.skuCodes, productVariantCode(productVariant)]);
  });
}

export function autoAssignOptions(product: Product, options: Array<Option>): Product {
  const existingVariants: Array<ProductVariant> = product.variants;
  const indexedOptions = indexBySku(options);
  const newOptions = _.cloneDeep(options);
  const availableValues = allOptionsValues(newOptions);
  // here we assume that there is defined variant (even with feCode only) for each variant
  const existsOptionValues = _.map(existingVariants, variant => {
    return indexedOptions.q({av: [['sku', productVariantCode(variant)]]}).map(indexedOptions.get).map(x => x.option);
  });

  let closestTuples;
  let newVariants = [];

  const unbindAll = () => {
    // unbind all variants
    _.each(newOptions, option => {
      _.each(option.values, optionValue => {
        optionValue.skuCodes = [];
      });
    });
  };

  if (availableValues.length >= existsOptionValues.length) {
    // increase case
    closestTuples = findClosestTuples(existsOptionValues, availableValues, x => x.name);
    unbindAll();

    let lastUsedVariantIndex = null;

    for (let i = 0; i < closestTuples.length; i++) {
      const selectedTupleIndex = closestTuples[i];
      newVariants.push(existingVariants[i]);
      lastUsedVariantIndex = i;

      bindProductVariantToOptionsTuple(availableValues[selectedTupleIndex], existingVariants[i]);
    }
    closestTuples.sort();
    const oldOptionsByName = indexByName(product.options);

    for (let i = 0; i < availableValues.length; i++) {
      if (_.sortedIndexOf(closestTuples, i) !== -1) continue;

      const thereIsNewOptions = _.some(availableValues[i], value => {
        return !(value.name in oldOptionsByName);
      });
      if (!thereIsNewOptions) continue;

      const variant = lastUsedVariantIndex != null && lastUsedVariantIndex < existingVariants.length - 1
        ? existingVariants[++lastUsedVariantIndex] : createEmptyProductVariant();
      newVariants.push(variant);
      bindProductVariantToOptionsTuple(availableValues[i], variant);
    }
  } else {
    // reduce case
    closestTuples = findClosestTuples(availableValues, existsOptionValues, x => x.name);
    unbindAll();

    // do rest job
    for (let i = 0; i < availableValues.length; i++) {
      const selectedTuple = availableValues[i];
      const boundProductVariant = existingVariants[closestTuples[i]];

      newVariants.push(boundProductVariant);
      bindProductVariantToOptionsTuple(selectedTuple, boundProductVariant);
    }
  }

  return assoc(product,
    'variants', newVariants,
    'options', newOptions
  );
}
