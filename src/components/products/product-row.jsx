import React, { PropTypes } from 'react';
import _ from 'lodash';
import { activeStatus } from '../../paragons/common';

import RoundedPill from '../rounded-pill/rounded-pill';
import MultiSelectRow from '../table/multi-select-row';

function setCellContents(product, field) {
  switch (field) {
    case 'image':
      return _.get(product, ['images', 0]);
    case 'state':
      return <RoundedPill text={activeStatus(product)} />;
    default:
      return _.get(product, field);
  }
}

const ProductRow = (props) => {
  const { product, columns, params } = props;
  const key = `product-${product.id}`;

  return (
    <MultiSelectRow
      cellKeyPrefix={key}
      columns={columns}
      linkTo="product-details"
      linkParams={{productId: product.productId, context: product.context}}
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

export default ProductRow;
