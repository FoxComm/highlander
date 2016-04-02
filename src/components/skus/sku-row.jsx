import React, { Component, Element, PropTypes } from 'react';

import _ from 'lodash';
import { transitionTo } from '../../route-helpers';

import MultiSelectRow from '../table/multi-select-row';

function setCellContents(sku, field) {
  return _.get(sku, field);
}

const SkuRow = (props, context) => {
  const { sku, columns, params } = props;
  const key = `sku-${sku.code}`;
  const clickAction = () => {
    transitionTo(context.history, 'sku-details', { skuCode: sku.code});
  };

  return (
    <MultiSelectRow
      cellKeyPrefix={key}
      columns={columns}
      onClick={clickAction}
      row={sku}
      setCellContents={setCellContents}
      params={params} />
  );
};

SkuRow.propTypes = {
  sku: PropTypes.object.isRequired,
  columns: PropTypes.array.isRequired,
  params: PropTypes.object.isRequired,
};

SkuRow.contextTypes = {
  history: PropTypes.object.isRequired,
};

export default SkuRow;
