/* @flow */

// libs
import _ from 'lodash';
import React from 'react';

// components
import LineItemRow from './summary-line-item';

// styles
import styles from './product-table.css';

type Props = {
  skus: Array<any>,
};

const Products = (props: Props) => {
  const rows = _.map(props.skus, (item) => <LineItemRow {...item} key={item.sku} />);

  return (
    <table styleName="products">
      <thead>
        <tr>
          <th colSpan="2">
            <span styleName="info">
              <span>ITEM</span>
              <span>QTY</span>
            </span>
          </th>
          <th styleName="price">PRICE</th>
        </tr>
      </thead>
      <tbody>
        {rows}
      </tbody>
    </table>
  );
};

export default Products;
