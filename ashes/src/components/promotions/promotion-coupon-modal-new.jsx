/* @flow */
import _ from 'lodash';
import React, { Element, Component } from 'react';

// components
import CouponNew from './coupon-new';

export default class PromoCouponNewModal extends Component {
  props: Props;

  constructor(props: Props) {
    super(props);
  };

  render() {
    return (
      <CouponNew
        promotionId={this.props.params.promotionId}
        isVisible={true}
      />
    );
  }  
}

