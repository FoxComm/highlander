// @flow

import React from 'react';

import InventoryAndTransactions from '../skus/inventory-and-transactions';

import type { ProductVariant } from 'modules/product-variants/details';

type Props = {
  object: ProductVariant
}

const InventoryPage = (props: Props) => {
  return (
    <InventoryAndTransactions
      skuId={props.object.middlewarehouseSkuId}
      // @TODO: get rid of passing code here
      skuCode={props.object.attributes.code.v}
      showSkuLink
      readOnly
    />
  );
};

export default InventoryPage;
