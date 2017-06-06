/* @flow */

// libs
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { isEmpty, map } from 'lodash';

// components
import { ModalContainer } from 'components/modal/base';
import ContentBox from 'components/content-box/content-box';
import SaveCancel from 'components/core/save-cancel';
import CustomersTypeahead from 'components/users-typeahead/customers-typeahead';
import Icon from 'components/core/icon';

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

  get actionBlock() {
    return (
      <a className='fc-modal-close' onClick={this.props.onCancel}>
        <Icon name='close' />
      </a>
    );
  }

  get footer() {
    return (
      <SaveCancel
        className="fc-modal-footer fc-add-watcher-modal__footer"
        onCancel={this.props.onCancel}
        onSave={this.handleSave}
        saveDisabled={this.isSaveDisabled}
      />
    );
  }

  get isSaveDisabled(): boolean {
    return isEmpty(this.state.customers);
  }

  @autobind
  handleSave() {
    const ids = map(this.state.customers, customer => customer.id);
    this.props.handleSave(ids);
  }

  @autobind
  handleSelect(customers: Array<TUser>) {
    this.setState({ customers });
  }

  render() {
    const props = this.props;

    return (
      <ModalContainer isVisible={props.isVisible}>
        <ContentBox
          title="Select Customers"
          actionBlock={this.actionBlock}
          footer={this.footer}
          className="fc-add-watcher-modal"
        >
          <div className="fc-modal-body fc-add-watcher-modal__content">
            <CustomersTypeahead
              suggestCustomers={props.suggestCustomers}
              suggested={props.suggested}
              suggestState={props.suggestState}
              onSelect={this.handleSelect}
              view="modal"
            />
          </div>
        </ContentBox>
      </ModalContainer>
    );
  }
}
