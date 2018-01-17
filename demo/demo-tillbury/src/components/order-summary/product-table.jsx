/* @flow */

// libs
import _ from 'lodash';
import React from 'react';
import { skuIdentity } from '@foxcommerce/wings/lib/paragons/sku';

// components
import LineItemRow from './summary-line-item';

// styles
import styles from './product-table.css';

type Props = {
  skus: Array<any>,
};

const Products = (props: Props) => {
  const rows = _.map(props.skus, item => <LineItemRow {...item} key={skuIdentity(item)} />);

  return (
    <div styleName="products">
      {rows}
    </div>
  );
};

export default Products;
