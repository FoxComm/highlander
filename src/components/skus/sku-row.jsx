import React, { Component, Element, PropTypes } from 'react';

import _ from 'lodash';

import MultiSelectRow from '../table/multi-select-row';

function setCellContents(sku, field) {
  return _.get(sku, field);
}

const SkuRow = (props) => {
  const { sku, columns, params } = props;
  const key = `sku-${sku.id}`;

  return (
    <MultiSelectRow
      cellKeyPrefix={key}
      columns={columns}
      linkTo="sku-details"
      linkParams={{skuCode: sku.code}}
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

export default SkuRow;
