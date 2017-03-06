/* @flow */
import _ from 'lodash';
import React, { PropTypes, Element } from 'react';
import { connect } from 'react-redux';

import { getClaims } from 'lib/claims';

import NavigationItem from './navigation-item';

import CatalogEntry from './entries/catalog';
import CustomersEntry from './entries/customers';
import MarketingEntry from './entries/marketing';
import MerchandisingEntry from './entries/merchandising';
import OrdersEntry from './entries/orders';
import SettingsEntry from './entries/settings';

import type { JWT } from 'lib/claims';

function getMenuItemState(props, to) {
  return _.get(props, ['menuItems', to]);
}

type Props = {
  routes: Array<Object>,
  collapsed: boolean,
  toggleMenuItem: Function,
  token: JWT,
};

function mapStateToProps(state) {
  return {
    token: state.user.current,
  };
}

const Navigation = (props: Props) => {
  const claims = getClaims(props.token);

  return (
    <nav>
      <ul className="fc-sidebar__navigation-list">
        <OrdersEntry
          claims={claims}
          routes={props.routes}
          collapsed={props.collapsed}
          status={getMenuItemState(props, 'orders')}
          toggleMenuItem={props.toggleMenuItem} />
        <CustomersEntry
          claims={claims}
          routes={props.routes}
          collapsed={props.collapsed}
          status={getMenuItemState(props, 'customers')}
          toggleMenuItem={props.toggleMenuItem} />
        <CatalogEntry
          claims={claims}
          routes={props.routes}
          collapsed={props.collapsed}
          status={getMenuItemState(props, 'products')}
          toggleMenuItem={props.toggleMenuItem} />
        <MerchandisingEntry
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

export default connect(mapStateToProps)(Navigation);
