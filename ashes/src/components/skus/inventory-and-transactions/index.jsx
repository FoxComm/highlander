// @flow

// libs
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import styles from './container.css';

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
  showSkuLink?: boolean,
  readOnly?: boolean,
}

class InventoryAndTransactions extends Component {
  props: Props;
  state: State = {
    currentView: 'inventory',
  };

  get body() {
    const { skuId, showSkuLink, readOnly } = this.props;
    if (this.state.currentView == 'inventory') {
      return (
        <SkuInventory
          skuId={skuId}
          showSkuLink={showSkuLink}
          readOnly={readOnly}
        />
      );
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
      <div styleName="root">
        <div styleName="summary">
            <TabListView>
              <TabView draggable={false} selected={!transactionsTabActive} onClick={this.selectInventory}>
                <div styleName="tab-link" >
                  Inventory
                </div>
              </TabView>
              <TabView draggable={false} selected={transactionsTabActive} onClick={this.selectTransactions}>
                <div styleName="tab-link">
                  Transactions
                </div>
              </TabView>
            </TabListView>
        </div>
        {this.body}
      </div>
    );
  }
}

export default InventoryAndTransactions;
