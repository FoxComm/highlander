/* @flow */

// libs
import classNames from 'classnames';
import { transitionToLazy } from 'browserHistory';
import React, { Element } from 'react';

// components
import Modal from 'components/core/modal';
import CouponPage from '../coupons/page';
import CouponForm from '../coupons/form';

// styles
import s from './coupon-new.css';

type Props = {
  promotionId: String,
};

export default (props: Props) => {
  const cancelAction = transitionToLazy('promotion-coupons', { promotionId: props.promotionId });

  return (
    <Modal
      className={classNames(s.modal, s['promotion-coupons-new'])}
      title="Add Coupon Code"
      isVisible
      onClose={cancelAction}
    >
      <CouponPage
        params={{
          couponId: 'new',
          promotionId: props.promotionId,
          modalCancelAction: cancelAction,
        }}
      >
        <CouponForm />
      </CouponPage>
    </Modal>
  );
};
