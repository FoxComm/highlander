/* @flow */
import _ from 'lodash';
import React, { PropTypes, Element } from 'react';

import { getClaims } from 'lib/claims';

import NavigationItem from './navigation-item';

import CatalogEntry from './entries/catalog';
import CustomersEntry from './entries/customers';
import MarketingEntry from './entries/marketing';
import OrdersEntry from './entries/orders';
import SettingsEntry from './entries/settings';

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
          claims={claims}
          routes={props.routes}
          collapsed={props.collapsed}
          status={getMenuItemState(props, 'orders')}
          toggleMenuItem={props.toggleMenuItem} />
        <CatalogEntry
          claims={claims}
          routes={props.routes}
          collapsed={props.collapsed}
          status={getMenuItemState(props, 'products')}
          toggleMenuItem={props.toggleMenuItem} />
        <MarketingEntry
          claims={claims}
          routes={props.routes}
          collapsed={props.collapsed}
          status={getMenuItemState(props, 'gift-cards')}
          toggleMenuItem={props.toggleMenuItem} />
        <SettingsEntry
          claims={claims}
          routes={props.routes}
          collapsed={props.collapsed}
          status={getMenuItemState(props, 'users')}
          toggleMenuItem={props.toggleMenuItem} />
      </ul>
    </nav>
  );
};

export default Navigation;
