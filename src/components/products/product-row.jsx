//libs
import React, { PropTypes } from 'react';
import _ from 'lodash';

// helpers
import { activeStatus, archivedStatus } from 'paragons/common';

// components
import RoundedPill from '../rounded-pill/rounded-pill';
import MultiSelectRow from '../table/multi-select-row';

// types
import type { Product } from 'paragons/product';

type Props = {
  product: Product,
  columns: ?Array,
  params: Object,
};

function setCellContents(product, field) {
  switch (field) {
    case 'image':
      return _.get(product, ['albums', 0, 'images', 0, 'src']);
    case 'state':
      return <RoundedPill text={activeStatus(product)} />;
    default:
      return _.get(product, field);
  }
}

const ProductRow = (props: Props) => {
  const { product, columns, params } = props;
  const key = `product-${product.id}`;
  const isArchived = archivedStatus(product);

  if (isArchived) {
    return (
      <MultiSelectRow
        columns={columns}
        row={product}
        setCellContents={setCellContents}
        params={params} />
    );
  }

  return (
    <MultiSelectRow
      columns={columns}
      linkTo="product-details"
      linkParams={{productId: product.productId, context: product.context}}
      row={product}
      setCellContents={setCellContents}
      params={params} />
  );
};

export default ProductRow;
