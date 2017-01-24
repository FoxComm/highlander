// @flow

import React from 'react';

import InventoryAndTransactions from '../skus/inventory-and-transactions';

const InventoryPage = (props) => {
  return (
    <InventoryAndTransactions
      skuId={props.object.middlewarehouseSkuId}
      showSkuLink
    />
  );
};

export default InventoryPage;
