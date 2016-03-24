
/* @flow weak */

import React from 'react';
import styles from './line-item.css';

import Icon from 'ui/icon';
import Currency from 'ui/currency';

const LineItem = props => {
  const click = () => {
    props.deleteLineItem(props.sku);
  };
  return (
    <div styleName="box">
      <div styleName="container">
        <div styleName="image">
          <img src={props.imagePath} />
        </div>
        <div styleName="details">
          <div styleName="product-name">
            {props.name}
          </div>
          <div styleName="quantity">
            QTY: {props.quantity}
          </div>
        </div>
        <div styleName="price">
          <Currency value={props.totalPrice}/>
        </div>
        <div styleName="controls">
          <a styleName="close-button" onClick={click}>
            <Icon name="fc-close" styleName="replace-icon" />
          </a>
        </div>
      </div>
    </div>
  );
};

export default LineItem;
