
// libs
import React, { PropTypes } from 'react';
import { transitionTo } from 'browserHistory';

// components
import { ListPageContainer, makeTotalCounter } from '../list-page';

// redux
import { actions as inventoryActions } from 'modules/inventory/list';

const InventoryListPage = (props, context) => {
  const TotalCounter = makeTotalCounter(state => state.inventory.list, inventoryActions);

  const navLinks = [
    { title: 'Lists', to: 'inventory' },
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
