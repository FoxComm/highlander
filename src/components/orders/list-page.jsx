// libs
import React, { PropTypes } from 'react';

// components
import { ListPageContainer, makeTotalCounter } from '../list-page';

// redux
import { actions } from '../../modules/orders/list';

const OrderListPage = props => {
  const TotalCounter = makeTotalCounter(state => state.orders.list, actions);

  const navLinks = [
    { title: 'Lists', to: 'orders' },
    { title: 'Insights', to: '' },
    { title: 'Activity Trail', to: 'orders-activity-trail' }
  ];

  return (
    <ListPageContainer
      title="Orders"
      subtitle={<TotalCounter/>}
      addTitle="Order"
      navLinks={navLinks}>
      {props.children}
    </ListPageContainer>
  );
};

OrderListPage.propTypes = {
  children: PropTypes.node,
};

export default OrderListPage;
