/* @flow */

// libs
import React from 'react';
import _ from 'lodash';
import { transitionTo } from 'browserHistory';

// helpers
import { activeStatus, isArchived } from 'paragons/common';

// components
import RoundedPill from '../rounded-pill/rounded-pill';
import MultiSelectRow from '../table/multi-select-row';

type Props = {
  product: Product,
  columns?: Array<Object>,
  params: Object,
};

function setCellContents(product, field, onCellClick) {
  switch (field) {
    case 'skus':
      return _.size(_.get(product, 'skus'));
    case 'image':
      return _.get(product, ['albums', 0, 'images', 0, 'src']);
    case 'state':
      return <RoundedPill text={activeStatus(product)} />;
    case 'unlink':
      return <button onClick={(event) => onCellClick(event, product) }>Unlink</button>;
    default:
      return _.get(product, field);
  }
}

const ProductRow = (props: Props) => {
  const { product, columns, params, onCellClick} = props;
  const commonParams = {
    columns,
    row: product,
    setCellContents,
    params,
  };

  if (isArchived(product)) {
    return <MultiSelectRow {...commonParams} />;
  }

const onClick = () => transitionTo("product-details", {
  productId: product.productId,
  context: product.context
});

  return (
    <MultiSelectRow
      { ...commonParams }
      onClick={onClick}
      onCellClick={onCellClick}
    />
  );
};

export default ProductRow;
