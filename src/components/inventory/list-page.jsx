
// libs
import React, { PropTypes } from 'react';
import { transitionTo } from '../../route-helpers';

// components
import { ListPageContainer, makeTotalCounter } from '../list-page';

// redux
import { actions as customersActions } from '../../modules/customers/list';

const InventoryListPage = (props, context) => {
  const TotalCounter = makeTotalCounter(state => state.inventory.list, customersActions);

  const navLinks = [
    { title: 'Lists', to: 'inventory' },
    { title: 'Insights', to: '' },
    { title: 'Activity Trail', to: 'inventory-activity-trail' }
  ];

  return (
    <ListPageContainer
      title="Inventory"
      subtitle={<TotalCounter />}
      navLinks={navLinks} >
      {props.children}
    </ListPageContainer>
  );
};

InventoryListPage.propTypes = {
  children: PropTypes.node,
};

export default InventoryListPage;
