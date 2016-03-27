
import _ from 'lodash';
import React from 'react';
import styles from './order-summary.css';
import { connect } from 'react-redux';

import TermValueLine from 'ui/term-value-line';
import Currency from 'ui/currency';

const LineItemRow = props => {
  console.log(props);
  return (
    <tr>
      <td styleName="product-image">
        <img src={props.imagePath} />
      </td>
      <td styleName="product-name">{props.name}</td>
      <td styleName="product-qty">{props.quantity}</td>
      <td styleName="product-price"><Currency value={props.totalPrice} /></td>
    </tr>
  );
};

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
            <Currency value={props.subtotal} />
          </TermValueLine>
        </li>
        <li>
          <TermValueLine>
            <span>SHIPPING</span>
            $0.00
          </TermValueLine>
        </li>
        <li>
          <TermValueLine>
            <span>TAX</span>
            $9.00
          </TermValueLine>
        </li>
      </ul>
      <TermValueLine styleName="grand-total">
        <span>GRAND TOTAL</span>
        $159.00
      </TermValueLine>
    </div>
  );
};

export default connect(getState, {})(OrderSummary);
