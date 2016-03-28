
import _ from 'lodash';
import React from 'react';
import styles from './order-summary.css';
import { connect } from 'react-redux';

import TermValueLine from 'ui/term-value-line';
import Currency from 'ui/currency';
import LineItemRow from './summary-line-item';

const getState = state => ({ ...state.cart });

const OrderSummary = props => {
  const rows = _.map(props.skus, (item) => <LineItemRow {...item} key={item.sku} />);
  return (
    <div styleName="order-summary">
      <div styleName="title">ORDER SUMMARY</div>
      <table styleName="products-table">
        <thead>
          <tr>
            <th styleName="product-image">ITEM</th>
            <th styleName="product-name" />
            <th styleName="product-qty">QTY</th>
            <th styleName="product-price">PRICE</th>
          </tr>
        </thead>
        <tbody>
          {rows}
        </tbody>
      </table>
      <ul styleName="price-summary">
        <li>
          <TermValueLine>
            <span>SUBTOTAL</span>
            <Currency value={props.totals.subTotal} />
          </TermValueLine>
        </li>
        <li>
          <TermValueLine>
            <span>SHIPPING</span>
            <Currency value={props.shipping} />
          </TermValueLine>
        </li>
        <li>
          <TermValueLine>
            <span>TAX</span>
            <Currency value={props.totals.taxes} />
          </TermValueLine>
        </li>
      </ul>
      <TermValueLine styleName="grand-total">
        <span>GRAND TOTAL</span>
        <Currency value={props.totals.total} />
      </TermValueLine>
    </div>
  );
};

export default connect(getState, {})(OrderSummary);
