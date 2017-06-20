
// libs
import React from 'react';
import PropTypes from 'prop-types';
import { transitionTo } from 'browserHistory';

// components
import { ListPageContainer, makeTotalCounter } from '../list-page';

// redux
import { actions as customersActions } from '../../modules/customers/list';

const CustomersListPage = props => {
  const TotalCounter = makeTotalCounter(state => state.customers.list, customersActions);

  const navLinks = [
    { title: 'Lists', to: 'customers' },
    { title: 'Activity Trail', to: 'customers-activity-trail' }
  ];

  return (
    <ListPageContainer
      title="Customers"
      subtitle={<TotalCounter />}
      addTitle="Customer"
      handleAddAction={ () => transitionTo('customers-new') }
      navLinks={navLinks}
    >
      {props.children}
    </ListPageContainer>
  );
};

CustomersListPage.propTypes = {
  children: PropTypes.node,
};

export default CustomersListPage;
