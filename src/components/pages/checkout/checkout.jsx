/* @flow */

import React, { PropTypes } from 'react';
import cssModules from 'react-css-modules';
import styles from './checkout.css';

import Icon from '../../common/icon';
import Shipping from './shipping';

const Checkout = () => {
  return (
    <div styleName="checkout">
      <Icon styleName="logo" name="fc-some_brand_logo" />
      <Shipping isEditing={false}/>
    </div>
  );
};

Checkout.propTypes = {
  children: PropTypes.node,
};

export default cssModules(Checkout, styles);
