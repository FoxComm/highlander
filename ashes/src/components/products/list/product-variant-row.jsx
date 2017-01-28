/* @flow */

// libs
import _ from 'lodash';
import React, { Component, Element } from 'react';
import { activeStatus, isArchived } from 'paragons/common';

// components
import MultiSelectRow from 'components/table/multi-select-row';
import RoundedPill from 'components/rounded-pill/rounded-pill';

// types
import type { ProductVariant } from 'modules/product-variants/list';

type Props = {
  productVariant: ProductVariant,
  columns?: Array<Object>,
  params: Object,
};

function setCellContents(productVariant, field) {
  switch (field) {
    //case 'image':
      //return _.get(productVariant, ['albums', 0, 'images', 0, 'src']);
    case 'state':
      return <RoundedPill text={activeStatus(productVariant)} />;
    case 'skus':
      return '—';
    case 'createdAt':
      return new Date().toISOString();
    default:
      return _.get(productVariant, field);
  }
}

const ProductVariantRow = (props: Props) => {
  const { productVariant, columns, params } = props;
  const commonParams = {
    columns,
    row: productVariant,
    setCellContents,
    params,
  };

  if (isArchived(productVariant)) {
    return <MultiSelectRow {...commonParams} />;
  }

  return (
    <MultiSelectRow
      { ...commonParams }
      linkTo="product-variant-details"
      linkParams={{productVariantId: productVariant.id}}
    />
  );
};

export default ProductVariantRow;
