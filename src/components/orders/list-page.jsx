// libs
import React, { PropTypes } from 'react';
import { transitionTo } from '../../route-helpers';

// components
import { ListPageContainer, makeTotalCounter } from '../list-page';

// redux
import { actions } from '../../modules/orders/list';

const OrderListPage = (props, context) => {
  const TotalCounter = makeTotalCounter(state => state.orders.list, actions);
  const addAction = () => transitionTo(context.history, 'new-order');

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
      handleAddAction={addAction}
      navLinks={navLinks}>
      {props.children}
    </ListPageContainer>
  );
};

OrderListPage.propTypes = {
  children: PropTypes.node,
};

OrderListPage.contextTypes = {
  history: PropTypes.object.isRequired,
};

export default OrderListPage;
