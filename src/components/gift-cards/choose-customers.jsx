
import { autobind } from 'core-decorators';
import React, { PropTypes } from 'react';
import { PrimaryButton } from '../common/buttons';
import SelectableList from '../selectable-list/selectable-list';
import CustomerRow from './customer-row';

import styles from '../selectable-list/selectable-list.css';

export default class ChooseCustomers extends React.Component {

  static propTypes = {
    items: PropTypes.array.isRequired,
    onAddCustomers: PropTypes.func.isRequired,
    toggleVisibility: PropTypes.func,
  };

  @autobind
  handleClickAddCustomers(event) {
    event.preventDefault();
    this.props.toggleVisibility(false);
    this.props.onAddCustomers(this.refs.customers.selectedItemsMap());
  }

  @autobind
  renderCustomer(customer) {
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
