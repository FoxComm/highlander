
// libs
import React, { PropTypes } from 'react';
import { transitionTo } from 'browserHistory';

// components
import { ListPageContainer, makeTotalCounter } from '../list-page';

// redux
import { actions } from '../../modules/coupons/list';

type CouponsListProps = {
  children: Element<*>,
};

const CouponsList = (props: CouponsListProps) => {
  const TotalCounter = makeTotalCounter(state => state.coupons.list, actions);
  const addAction = () => transitionTo('coupon-details', {couponId: 'new'});

  const navLinks = [
    { title: 'Lists', to: 'coupons' },
    { title: 'Activity Trail', to: 'coupons-activity-trail' }
  ];

  return (
    <ListPageContainer
      title="Coupons"
      subtitle={<TotalCounter/>}
      addTitle="Coupon"
      handleAddAction={addAction}
      navLinks={navLinks}>
      {props.children}
    </ListPageContainer>
  );
};

CouponsList.propTypes = {
  children: PropTypes.node,
};

export default CouponsList;
