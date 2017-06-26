/* @flow */

// libs
import React, { Element } from 'react';
import _ from 'lodash';

// helpers
import { isArchived } from 'paragons/common';

// components
import MultiSelectRow from '../table/multi-select-row';

type Props = {
  sku: SkuSearchItem,
  columns?: Columns,
  params: Object,
};

function setCellContents(sku, field) {
  return _.get(sku, field);
}

const SkuRow = (props: Props) => {
  const { sku, columns, params } = props;
  const commonParams = {
    columns,
    row: sku,
    setCellContents,
    params,
  };

  if (isArchived(sku)) {
    return <MultiSelectRow {...commonParams} />;
  }

  return (
    <MultiSelectRow
      { ...commonParams }
      linkTo="sku-details"
      linkParams={{skuCode: sku.skuCode}}
    />
  );
};

export default SkuRow;
