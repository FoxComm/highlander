
//libs
import React, { PropTypes } from 'react';

// components
import { SectionTitle } from '../section-title';
import TabListView from '../tabs/tabs';
import TabView from '../tabs/tab';
import { Link, IndexLink } from '../link';

export default class InventoryItemDetails extends React.Component {
  render() {
    return (
      <div className="fc-inventory-item-details">
        <div className="fc-grid">
          <div className="fc-col-md-1-1">
            <SectionTitle title="Inventory" />
          </div>
        </div>
        <TabListView>
          <TabView draggable={false} selected={true} >
            <IndexLink to="inventory-item-details"
                       params={this.props.params}
                       className="fc-inventory-item-details__tab-link">
              Inventory
            </IndexLink>
          </TabView>
          <TabView draggable={false} selected={false} >
            <Link to="inventory-item-transactions"
                  params={this.props.params}
                  className="fc-inventory-item-details__tab-link">
              Transactions
            </Link>
          </TabView>
        </TabListView>
      </div>
    );
  }
}
