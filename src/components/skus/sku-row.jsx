/* @flow */

// libs
import React, { Component, Element, PropTypes } from 'react';
import _ from 'lodash';

// helpers
import { archivedStatus } from 'paragons/common';

// components
import MultiSelectRow from '../table/multi-select-row';

// types
import type { Sku } from '../../modules/skus/details';

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
  const key = `sku-${sku.id}`;
  const isArchived = archivedStatus(sku);

  if (isArchived) {
    return (
      <MultiSelectRow
        columns={columns}
        row={sku}
        setCellContents={setCellContents}
        params={params} />
    );
  }

  return (
    <MultiSelectRow
      columns={columns}
      linkTo="sku-details"
      linkParams={{skuCode: sku.code}}
      row={sku}
      setCellContents={setCellContents}
      params={params} />
  );
};

export default SkuRow;
