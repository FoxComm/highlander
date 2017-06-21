/* @flow */

// libs
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { isEmpty, map } from 'lodash';

// components
import Modal from 'components/core/modal';
import SaveCancel from 'components/core/save-cancel';
import CustomersTypeahead from 'components/users-typeahead/customers-typeahead';

type Props = {
  isVisible: boolean,
  suggestState: AsyncState,
  suggested: Array<TUser>,
  onCancel: () => void,
  handleSave: (ids: Array<number>) => void,
  suggestCustomers: (token: string) => Promise<*>,
};

type State = {
  customers: Array<TUser>,
};

export default class SearchCustomersModal extends Component {
  props: Props;

  state: State = {
    customers: [],
  };

  @autobind
  handleSave() {
    const ids = map(this.state.customers, customer => customer.id);
    this.props.handleSave(ids);
  }

  @autobind
  handleSelect(customers: Array<TUser>) {
    this.setState({ customers });
  }

  get footer() {
    const saveDisabled = isEmpty(this.state.customers);

    return <SaveCancel onCancel={this.props.onCancel} onSave={this.handleSave} saveDisabled={saveDisabled} />;
  }

  render() {
    const props = this.props;

    return (
      <Modal title="Select Customers" footer={this.footer} isVisible={props.isVisible} onClose={this.props.onCancel}>
        <CustomersTypeahead
          suggestCustomers={props.suggestCustomers}
          suggested={props.suggested}
          suggestState={props.suggestState}
          onSelect={this.handleSelect}
          view="modal"
        />
      </Modal>
    );
  }
}
