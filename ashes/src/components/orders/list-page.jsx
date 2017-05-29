// libs
import React from 'react';
import PropTypes from 'prop-types';
import { transitionTo } from 'browserHistory';

// components
import { ListPageContainer, makeTotalCounter } from '../list-page';

// redux
import { actions } from '../../modules/orders/list';

const OrderListPage = (props) => {
  const TotalCounter = makeTotalCounter(state => state.orders.list, actions);
  const addAction = () => transitionTo('new-order');

  const navLinks = [
    { title: 'Lists', to: 'orders' },
    { title: 'Activity Trail', to: 'orders-activity-trail' }
  ];

  return (
    <ListPageContainer
      title="Orders"
      subtitle={<TotalCounter />}
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

export default OrderListPage;
