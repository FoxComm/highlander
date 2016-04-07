
// libs
import React, { PropTypes } from 'react';
import { transitionTo } from '../../route-helpers';

// components
import { ListPageContainer, makeTotalCounter } from '../list-page';

// redux
import { actions } from '../../modules/coupons/list';

type CouponsListProps = {
  children: Element,
};

type CouponsListHistory = {
  history: object,
}

const CouponsList = (props: CouponsListProps, context: CouponsListHistory) => {
  const TotalCounter = makeTotalCounter(state => state.coupons.list, actions);
  const addAction = () => transitionTo(context.history, 'new-coupons');

  const navLinks = [
    { title: 'Lists', to: 'coupons' },
    { title: 'Insights', to: 'home' },
    { title: 'Activity Trail', to: 'home' }
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

CouponsList.contextTypes = {
  history: PropTypes.object.isRequired,
};

export default CouponsList;
