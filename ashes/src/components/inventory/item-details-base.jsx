/* @flow weak */

//libs
import React from 'react';
import PropTypes from 'prop-types';

// components
import { Link, IndexLink } from 'components/link';
import { SectionTitle } from '../section-title';
import TabListView from '../tabs/tabs';
import TabView from '../tabs/tab';

const InventoryItemDetailsBase = (props, context) => {
  const transactionsTabActive = context.router.isActive({ name: 'sku-inventory-transactions', params: props.params });

  return (
    <div className="fc-inventory-item-details">
      <div className="fc-inventory-item-details__summary">
        <SectionTitle title="Inventory" />
        <TabListView>
          <TabView draggable={false} selected={!transactionsTabActive}>
            <IndexLink to="sku-inventory-details"
                       params={props.params}
                       className="fc-inventory-item-details__tab-link">
              Inventory
            </IndexLink>
          </TabView>
          <TabView draggable={false} selected={transactionsTabActive}>
            <Link to="sku-inventory-transactions"
                  params={props.params}
                  className="fc-inventory-item-details__tab-link">
              Transactions
            </Link>
          </TabView>
        </TabListView>
      </div>

      {props.children}

    </div>
  );
};

InventoryItemDetailsBase.propTypes = {
  params: PropTypes.object,
  children: PropTypes.node,
};

InventoryItemDetailsBase.contextTypes = {
  router: PropTypes.object.isRequired,
};

export default InventoryItemDetailsBase;
