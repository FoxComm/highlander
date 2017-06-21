/* @flow */

// libs
import _ from 'lodash';
import React from 'react';
import { skuIdentity } from '@foxcomm/wings/lib/paragons/sku';

// components
import LineItemRow from './summary-line-item';

// styles
import styles from './product-table.css';

type Props = {
  skus: Array<any>,
  confirmationPage?: boolean,
};

const Products = (props: Props) => {
  const rows = _.map(props.skus, (item) => {
    return (
      <LineItemRow
        {...item}
        key={skuIdentity(item)}
        confirmationPage={props.confirmationPage}
      />
    );
  });

  return (
    <div styleName="products">
      {rows}
    </div>
  );
};

export default Products;
