/* @flow  */

//libs
import React, { Component } from 'react';
import { autobind } from 'core-decorators';

// components
import TabListView from 'components/tabs/tabs';
import TabView from 'components/tabs/tab';
import SkuInventory from './sku-inventory';

type State = {
  currentView: 'inventory' | 'transactions',
}

class InventoryTransactionsContainer extends Component {
  state: State = {
    currentView: 'inventory',
  };

  get children() {
    const { children, ...restProps } = this.props;

    return React.cloneElement(children, restProps);
  }

  @autobind
  selectTransactions() {
    this.setState({
      currentView: 'transactions',
    });
  }

  @autobind
  selectInventory() {
    this.setState({
      currentView: 'inventory',
    });
  }

  render() {
    const transactionsTabActive = this.state.currentView == 'transactions';

    return (
      <div className="fc-inventory-item-details">
        <div className="fc-inventory-item-details__summary">
          <div className="fc-grid">
            <div className="fc-col-md-1-1">
              <TabListView>
                <TabView draggable={false} selected={!transactionsTabActive} onClick={this.selectInventory}>
                  <div className="fc-inventory-item-details__tab-link" >
                    Inventory
                  </div>
                </TabView>
                <TabView draggable={false} selected={transactionsTabActive} onClick={this.selectTransactions}>
                  <div className="fc-inventory-item-details__tab-link">
                    Transactions
                  </div>
                </TabView>
              </TabListView>
            </div>
          </div>
        </div>
        {this.children}
      </div>
    );
  }
}

export default InventoryTransactionsContainer;
