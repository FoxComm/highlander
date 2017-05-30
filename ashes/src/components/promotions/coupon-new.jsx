/* @flow */

// libs
import { autobind } from 'core-decorators';
import { transitionTo } from 'browserHistory';
import React, { Element, Component } from 'react';

// components
import Modal from 'components/core/modal';
import wrapModal from '../modal/wrapper';
import ContentBox from '../content-box/content-box';
import CouponPage from '../coupons/page';
import CouponForm from '../coupons/form';

// styles
import styles from './coupon-new.css';

type Props = {
  promotionId: String,
};

class CouponNew extends Component {
  props: Props;

  @autobind
  cancelAction() {
    transitionTo('promotion-coupons', { promotionId: this.props.promotionId });
  }

  render() {
    const actionBlock = <i onClick={this.cancelAction} className="fc-btn-close icon-close" title="Close" />;

    return (
      <div styleName="promotion-coupons-new">
        <ContentBox actionBlock={actionBlock} title="Add Coupon Code">
          <CouponPage params={{
            couponId: 'new',
            promotionId: this.props.promotionId,
            modalCancelAction: this.cancelAction
          }}>
            <CouponForm />
          </CouponPage>
        </ContentBox>
      </div>
    );
  }
}

const Wrapped: Class<React.Component<void, Props, any>> = wrapModal(CouponNew);

export default Wrapped;
