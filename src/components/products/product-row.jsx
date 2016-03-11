import React, { PropTypes } from 'react';

import _ from 'lodash';
import { transitionTo } from '../../route-helpers';

import MultiSelectRow from '../table/multi-select-row';

function setCellContents(product, field) {
  if (field === 'image') {
    return _.get(product, ['images', 0]);
  }

  return _.get(product, field);
}

const ProductRow = (props, context) => {
  const { product, columns, params } = props;
  const key = `product-${product.id}`;
  const clickAction = () => {
    transitionTo(context.history, 'product', { product: product.id });
  };

  return (
    <MultiSelectRow
      cellKeyPrefix={key}
      columns={columns}
      onClick={clickAction}
      row={product}
      setCellContents={setCellContents}
      params={params} />
  );
};

ProductRow.propTypes = {
  product: PropTypes.object.isRequired,
  columns: PropTypes.array,
  params: PropTypes.object.isRequired,
};

ProductRow.contextTypes = {
  history: PropTypes.object.isRequired
};

export default ProductRow;
