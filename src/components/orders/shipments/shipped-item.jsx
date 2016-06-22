/* @flow */

// libs
import _ from 'lodash';
import React, { Component, PropTypes, Element } from 'react';
import { autobind } from 'core-decorators';

// styles
import styles from './shipped-item.css';

// components
import Currency from '../../common/currency';

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
    <div styleName="cell">{props.sku}</div>
    <div styleName="cell">
      <Currency value={props.price} />
    </div>
    <div styleName="cell">{props.quantity}</div>
    <div styleName="cell">{props.state}</div>
    <div styleName="cell">
      <Currency value={props.price} />
    </div>
  </div>
);

export default ShippedItem;
