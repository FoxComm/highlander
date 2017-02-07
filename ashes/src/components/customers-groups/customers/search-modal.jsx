/* @flow */

// libs
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { isEmpty } from 'lodash';

// components
import { ModalContainer } from 'components/modal/base';
import ContentBox from 'components/content-box/content-box';
import SaveCancel from 'components/common/save-cancel';
import CustomersTypeahead from './typeahead';

type Props = {
  isVisible: boolean,
  suggestState: string,
  suggested: Array<any>,
  onCancel: Function,
  handleSave: Function,
  suggestGroups: Function,
};

type State = {
  customers: Array<any>,
};

export default class SearchCustomersModal extends Component {

  props: Props;
  state: State = {
    customers: [],
  };

  get actionBlock() {
    return (
      <a className='fc-modal-close' onClick={this.props.onCancel}>
        <i className='icon-close'></i>
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
    this.props.handleSave(this.state.customers);
  }

  @autobind
  handleSelect(customers: Array<any>) {
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
              suggestGroups={props.suggestGroups}
              suggested={props.suggested}
              suggestState={props.suggestState}
              onSelect={this.handleSelect}
            />
          </div>
        </ContentBox>
      </ModalContainer>
    );
  }
}
