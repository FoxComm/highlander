/* @flow weak */

// libs
import _ from 'lodash';
import React, { PropTypes, Component } from 'react';
import { autobind } from 'core-decorators';

// components
import SelectableList from '../selectable-list/selectable-list';
import CustomerRow from './customer-row';

import styles from '../selectable-list/selectable-list.css';

import type { Customer } from 'paragons/customer';
import type { ItemType } from '../selectable-list/selectable-list';

type Props = {
  items: Array<ItemType>,
  onAddCustomers: (selectedItemsMap: {[key: string|number]: any}) => any,
  toggleVisibility: (visibility: boolean) => void,
  clearInputState: Function,
};

class ChooseCustomers extends Component {
  props: Props;

  @autobind
  handleClickAddCustomers() {
    this.props.toggleVisibility(false);
    this.props.onAddCustomers(this.refs.customers.selectedItemsMap());
    this.props.clearInputState();
  };

  renderCustomer(item: ItemType) {
    // Cast the ItemType to a customer row.
    const customer = ((item: any): Customer);
    return <CustomerRow customer={customer} key={customer.id}/>;
  }

  render() {
    return (
      <SelectableList
        visible={true}
        popup={false}
        items={this.props.items}
        ref="customers"
        emptyMessage="No customers found."
        renderItem={this.renderCustomer}
        onSelect={this.handleClickAddCustomers}
        actionTitle="Add Customers" />
    );
  }
}

export default ChooseCustomers;

