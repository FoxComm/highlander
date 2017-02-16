/* @flow */

// libs
import _ from 'lodash';
import React, { Component, PropTypes, Element } from 'react';
import { autobind } from 'core-decorators';

// styles
import styles from './shipped-item.css';

// components
import Currency from 'components/common/currency';

//types
import type { TShipmentLineItem } from 'paragons/shipment';

const ShippedItem = (props: TShipmentLineItem): Element<*>=> (
  <div styleName="row">
    <div styleName="name">
      <img src={props.imagePath} />
      {props.name}
    </div>
    <div styleName="sku">{props.sku}</div>
    <div styleName="price">
      <Currency value={props.price} />
    </div>
    <div styleName="quantity">1</div>
    <div styleName="state">{props.state}</div>
    <div styleName="total">
      <Currency value={props.price} />
    </div>
  </div>
);

export default ShippedItem;
