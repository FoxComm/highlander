/* @flow weak */
import { autobind } from 'core-decorators';
import React, { PropTypes } from 'react';
import { PrimaryButton } from '../common/buttons';
import SelectableList from '../selectable-list/selectable-list';
import CustomerRow from './customer-row';

import styles from '../selectable-list/selectable-list.css';

import type { ItemType } from '../selectable-list/selectable-list';

type Props = {
  items: Array<ItemType>;
  onAddCustomers: (selectedItemsMap: {[key: string|number]: any}) => any;
  toggleVisibility: (visibility: boolean) => void;
};


export default class ChooseCustomers extends React.Component {
  props: Props;

  @autobind
  handleClickAddCustomers(event: SyntheticEvent) {
    event.preventDefault();
    this.props.toggleVisibility(false);
    this.props.onAddCustomers(this.refs.customers.selectedItemsMap());
  }

  @autobind
  renderCustomer(customer: ItemType) {
    return <CustomerRow customer={customer} />;
  }

  render() {
    const { items } = this.props;

    const buttonDisabled = this.refs.customers && this.refs.customers.selectedIds.length === 0;

    return (
      <SelectableList
        popup={false}
        items={items}
        ref="customers"
        emptyMessage="No customers found."
        renderItem={this.renderCustomer}
      >
        <PrimaryButton
          styleName="choose-button"
          disabled={buttonDisabled}
          onClick={this.handleClickAddCustomers}>
          Add Customers
        </PrimaryButton>
      </SelectableList>
    );
  }
}
