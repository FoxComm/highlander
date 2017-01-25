// @flow

import React from 'react';

import InventoryAndTransactions from '../skus/inventory-and-transactions';

const InventoryPage = (props) => {
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
