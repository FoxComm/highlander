/* @flow */
import React from 'react';
import _ from 'lodash';

export default function sanitizeLineItems(error: string, lineItems: Array<mixed>) {
  if (/Following SKUs are out/.test(error)) {
    const skus = error.split('.')[0].split(':')[1].split(',');

    const products = _.reduce(skus, (acc, outOfStock) => {
      const sku = _.find(lineItems, { sku: outOfStock.trim() });
      if (sku) {
        return [
          ...acc,
          sku.name,
        ];
      }

      return acc;
    }, []);

    const singleProduct = products.length === 1;
    const title = singleProduct ? 'Product' : 'Products';
    const verb = singleProduct ? 'is' : 'are';
    const pronoun = singleProduct ? 'it' : 'them';
    return (
      <span>
        {title} <strong>{products.join(', ')}</strong> {verb} out of stock.
        Please remove {pronoun} to complete the checkout.
      </span>
    );
  }

  return null;
}
