// @flow

// libs
import React, { Component } from 'react';
import { autobind } from 'core-decorators';

// components
import TabListView from 'components/tabs/tabs';
import TabView from 'components/tabs/tab';
import SkuInventory from './sku-inventory';
import SkuTransactions from './sku-transactions';

type State = {
  currentView: 'inventory' | 'transactions',
}

type Props = {
  skuId: number,
}

class InventoryTransactionsContainer extends Component {
  props: Props;
  state: State = {
    currentView: 'inventory',
  };

  get body() {
    const { skuId } = this.props;
    if (this.state.currentView == 'inventory') {
      return <SkuInventory skuId={skuId} />;
    } else {
      return <SkuTransactions skuId={skuId} />;
    }
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
        {this.body}
      </div>
    );
  }
}

export default InventoryTransactionsContainer;
