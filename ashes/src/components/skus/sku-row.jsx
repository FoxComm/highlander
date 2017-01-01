/* @flow */

// libs
import React, { Component, Element, PropTypes } from 'react';
import _ from 'lodash';

// helpers
import { isArchived } from 'paragons/common';

// components
import MultiSelectRow from '../table/multi-select-row';

// types
import type { Sku } from 'modules/skus/list';

type Props = {
  sku: Sku,
  columns?: Array<Object>,
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
      linkParams={{skuCode: sku.id}}
    />
  );
};

export default SkuRow;
