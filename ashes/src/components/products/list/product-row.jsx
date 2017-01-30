/* @flow */

//libs
import React, { Element } from 'react';
import _ from 'lodash';

// helpers
import { activeStatus, isArchived } from 'paragons/common';

// components
import RoundedPill from 'components/rounded-pill/rounded-pill';
import MultiSelectRow from 'components/table/multi-select-row';

// types
import type { Product } from 'paragons/product';

type Props = {
  product: Product,
  columns?: Array<Object>,
  params: Object,
  toggleIcon: Element,
};

const ProductRow = (props: Props) => {
  const { product, columns, params } = props;

  const setCellContents = (product, field, options) => {
    switch (field) {
      case 'image':
        return _.get(product, ['albums', 0, 'images', 0, 'src']);
      case 'state':
        return <RoundedPill text={activeStatus(product)} />;
      case 'skus':
        return product.skus.length;
      case 'createdAt':
        return new Date().toISOString();
      case 'skuCode':
        return '—';
      case 'selectColumn':
        return (
          <div>
            {options.checkbox}
            {props.toggleIcon}
          </div>
        );
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
