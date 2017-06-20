/* @flow */

import React from 'react';

// libs
import _ from 'lodash';
import { activeStatus, isArchived } from 'paragons/common';

// components
import { RoundedPill } from 'components/core/rounded-pill';
import MultiSelectRow from '../table/multi-select-row';

// styles
import styles from './product-row.css';

type Props = {
  product: Product,
  columns?: Columns,
  params: Object,
};

const ProductRow = (props: Props) => {
  const { product, columns, params } = props;

  const setCellContents = (product: Object, field: string) => {
    switch (field) {
      case 'skus':
        return _.size(_.get(product, 'skus'));
      case 'image':
        return _.get(product, ['albums', 0, 'images', 0, 'src']);
      case 'state':
        return <RoundedPill text={activeStatus(product)} />;
      default:
        return _.get(product, field);
    }
  };

  const commonParams = {
    columns,
    row: product,
    setCellContents,
    params,
  };

  if (isArchived(product)) {
    return <MultiSelectRow {...commonParams} />;
  }

  return (
    <MultiSelectRow
      { ...commonParams }
      linkTo="product-details"
      linkParams={{productId: product.productId, context: product.context}}
    />
  );
};

export default ProductRow;
