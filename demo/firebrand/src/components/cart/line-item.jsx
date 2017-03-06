
/* @flow weak */

import React from 'react';
import styles from './line-item.css';

import localized from 'lib/i18n';

import Icon from 'ui/icon';
import Currency from 'ui/currency';

const LineItem = props => {
  const { t } = props;
  const click = () => {
    props.deleteLineItem(props.sku);
  };
  return (
    <div styleName="box">
      <div styleName="image">
        <img src={props.imagePath} />
      </div>
      <div styleName="container">
        <div styleName="details">
          <div styleName="product-name">
            {props.name}
          </div>
          <div styleName="quantity">
            {t('QTY')}: {props.quantity}
          </div>
        </div>
        <div styleName="price">
          <Currency value={props.totalPrice}/>
        </div>
      </div>
      <div styleName="controls">
        <a styleName="close-button" onClick={click}>
          <Icon name="fc-close" styleName="replace-icon" />
        </a>
      </div>
    </div>
  );
};

export default localized(LineItem);
