/* @flow weak */

// libs
import _ from 'lodash';
import React, { PropTypes, Component } from 'react';
import { autobind } from 'core-decorators';

// components
import SelectableList from '../selectable-list/selectable-list';
import CustomerRow from './customer-row';

import styles from '../selectable-list/selectable-list.css';

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

  renderCustomer(customer: ItemType) {
    return <CustomerRow customer={customer} key={customer.id}/>;
  }

  render() {
    return (
      <SelectableList
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

