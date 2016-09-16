/* @flow */
import _ from 'lodash';
import React, { PropTypes } from 'react';

import { getClaims } from 'lib/claims'; 

import NavigationItem from './navigation-item';

import CatalogEntry from './entities/catalog';
import CustomersEntry from './entities/customers';
import MarketingEntry from './entities/marketing';
import OrdersEntry from './entities/orders';
import SettingsEntry from './entities/settings';

function getMenuItemState(props, to) {
  return _.get(props, ['menuItems', to]);
}

type Props = {
  routes: Array<Object>,
  collapsed: boolean,
  toggleMenuItem: Function,
};

const Navigation = (props: Props): Element => {
  const claims = getClaims();

  return (
    <nav>
      <ul className="fc-sidebar__navigation-list">
        <CustomersEntry
          claims={claims}
          routes={props.routes}
          collapsed={props.collapsed}
          status={getMenuItemState(props, 'customers')}
          toggleMenuItem={props.toggleMenuItem} />
        <OrdersEntry
          routes={props.routes}
          collapsed={props.collapsed}
          status={getMenuItemState(props, 'carts')}
          toggleMenuItem={props.toggleMenuItem} />
        <CatalogEntry
          routes={props.routes}
          collapsed={props.collapsed}
          status={getMenuItemState(props, 'products')}
          toggleMenuItem={props.toggleMenuItem} />
        <MarketingEntry
          routes={props.routes}
          collapsed={props.collapsed}
          status={getMenuItemState(props, 'gift-cards')}
          toggleMenuItem={props.toggleMenuItem} />
        <SettingsEntry
          routes={props.routes}
          collapsed={props.collapsed}
          status={getMenuItemState(props, 'users')}
          toggleMenuItem={props.toggleMenuItem} />
      </ul>
    </nav>
  );
};

export default Navigation;
