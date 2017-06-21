/* @flow */
import _ from 'lodash';
import React, { Element, Component } from 'react';

// components
import CouponNew from './coupon-new';

type Params = {
	promotionId: String,
};

type Props = {
	params: Params,
};

export default class PromoCouponNewModal extends Component {
  props: Props;

  render() {
    return (
      <CouponNew
        promotionId={this.props.params.promotionId}
        isVisible
      />
    );
  }
}

