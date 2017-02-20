/* @flow */
import _ from 'lodash';
import React, { Element, Component } from 'react';

// components
import Coupon from './coupon';

export default class PromoCouponModal extends Component {
  props: Props;

  constructor(props: Props) {
    super(props);
  };

  render() {
    return (
      <div className="fc-promotion-coupon">
        <Coupon
          isVisible={true}
        />
      </div>
    );
  }  
}

