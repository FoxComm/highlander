/* @flow */
import _ from 'lodash';
import React, { Element, Component } from 'react';
import { autobind } from 'core-decorators';

import styles from './coupon-new.css';

// components
import wrapModal from '../modal/wrapper';
import ContentBox from '../content-box/content-box';
import { FormField } from '../forms';
import SaveCancel from '../common/save-cancel';
import RadioButton from '../forms/radio-button';
import * as widgets from './widgets';


import CouponPage from '../coupons/page';
import CouponForm from '../coupons/form';

class CouponNew extends Component {
  props: Props;

  constructor(props: Props) {
 		super(props);
  }


  render() {
    return (
      <div className="fc-promotion-coupon">
        <div className="fc-modal-container">
	      <ContentBox title="Add Coupon Code">
	      	<div className="fc-clearfix">
	          <CouponPage params={{couponId: 'new', couponModal: true, hideTitle: true}}>
	          	<CouponForm></CouponForm>
	          </CouponPage>	
	        </div>  
          </ContentBox>
        </div>
      </div>
    );
  }  
}

export default wrapModal(CouponNew);
