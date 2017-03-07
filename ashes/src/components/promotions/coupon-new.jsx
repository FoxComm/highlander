/* @flow */
import _ from 'lodash';
import React, { Element, Component } from 'react';
import { autobind } from 'core-decorators';
import { transitionTo } from 'browserHistory';

import styles from './coupon-new.css';

// components
import wrapModal from '../modal/wrapper';
import ContentBox from '../content-box/content-box';
import CouponPage from '../coupons/page';
import CouponForm from '../coupons/form';

class CouponNew extends Component {
  props: Props;

  constructor(props: Props) {
 		super(props);
  }

  @autobind
  cancelAction(){
    transitionTo('promotion-coupons', {promotionId: this.props.promotionId});
  };

  render() {
  	let actionBlock = <i onClick={this.cancelAction} className="fc-btn-close icon-close" title="Close" />;
    return (
      <div styleName="promotion-coupons-new">
	      <ContentBox actionBlock={actionBlock} title="Add Coupon Code">
          <CouponPage params={{
          		couponId: 'new', 
              promotionId: this.props.promotionId,
          		modalCancelAction: this.cancelAction
          	}}>
          	<CouponForm/>
          </CouponPage>	
        </ContentBox>
      </div>
    );
  }  
}

export default wrapModal(CouponNew);
