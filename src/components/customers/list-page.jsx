
// libs
import React, { PropTypes } from 'react';
import { transitionTo } from '../../route-helpers';

// components
import { ListPageContainer, makeTotalCounter } from '../list-page';

// redux
import { actions as customersActions } from '../../modules/customers/list';

const CustomersListPage = (props, context) => {
  const TotalCounter = makeTotalCounter(state => state.customers.list, customersActions);

  const navLinks = [
    { title: 'Lists', to: 'customers' },
    { title: 'Customer Groups', to: 'groups' },
    { title: 'Insights', to: '' },
    { title: 'Activity Trail', to: 'customers-activity-trail' }
  ];

  return (
    <ListPageContainer
      title="Customers"
      subtitle={<TotalCounter url="customers_search_view/_search" />}
      addTitle="Customer"
      handleAddAction={ () => transitionTo(context.history, 'customers-new') }
      navLinks={navLinks}
    >
      {props.children}
    </ListPageContainer>
  );
};

CustomersListPage.propTypes = {
  children: PropTypes.node,
};

CustomersListPage.contextTypes = {
  history: PropTypes.object.isRequired,
};

export default CustomersListPage;
