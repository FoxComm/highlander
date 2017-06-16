/* @flow */

import React, { Element } from 'react';
import { connect } from 'react-redux';

import { getClaims } from 'lib/claims';

import CatalogEntry from './entries/catalog';
import CustomersEntry from './entries/customers';
import InventoryEntry from './entries/inventory';
import MarketingEntry from './entries/marketing';
import MerchandisingEntry from './entries/merchandising';
import OrdersEntry from './entries/orders';
import SettingsEntry from './entries/settings';

import type { JWT } from 'lib/claims';

type Params = { [key: string]: string };

function mapStateToProps(state) {
  return {
    token: state.user.current,
  };
}

const Navigation = ({ routes, token, params }: { routes: Array<Object>, token: JWT, params: Params }) => {
  const claims = getClaims(token);

  return (
    <nav>
      <ul className="fc-sidebar__navigation-list">
        <OrdersEntry
          claims={claims}
          routes={routes}
        />
        <CustomersEntry
          claims={claims}
          routes={routes}
        />
        <CatalogEntry
          claims={claims}
          routes={routes}
        />
        <InventoryEntry
          claims={claims}
          routes={routes}
        />
        <MerchandisingEntry
          claims={claims}
          routes={routes}
          currentParams={params}
        />
        <MarketingEntry
          claims={claims}
          routes={routes}
        />
        <SettingsEntry
          claims={claims}
          routes={routes}
        />
      </ul>
    </nav>
  );
};

export default connect(mapStateToProps)(Navigation);
