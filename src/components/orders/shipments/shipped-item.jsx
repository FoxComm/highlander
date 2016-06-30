/* @flow */

// libs
import _ from 'lodash';
import React, { Component, PropTypes, Element } from 'react';
import { autobind } from 'core-decorators';

// styles
import styles from 'components/orders/shipments/shipped-item.css';

// components
import Currency from 'components/common/currency';

//types
type Props = {
  imagePath: string;
  name: string;
  price: number;
  quantity: number;
  sku: string;
  state: string;
};


const ShippedItem = (props: Props): Element => (
  <div styleName="row">
    <div styleName="name">
      <img src={props.imagePath} />
      {props.name}
    </div>
    <div styleName="sku">{props.sku}</div>
    <div styleName="price">
      <Currency value={props.price} />
    </div>
    <div styleName="quantity">{props.quantity}</div>
    <div styleName="state">{props.state}</div>
    <div styleName="total">
      <Currency value={props.price} />
    </div>
  </div>
);

export default ShippedItem;
