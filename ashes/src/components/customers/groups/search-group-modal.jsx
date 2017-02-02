/* @flow */

// libs
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { isEmpty } from 'lodash';

// components
import { ModalContainer } from 'components/modal/base';
import ContentBox from 'components/content-box/content-box';
import SaveCancel from 'components/common/save-cancel';
import GroupsTypeahead from './groups-typeahead';

type Props = {
  isVisible: boolean,
  suggestState: string,
  suggested: Array<TCustomerGroupShort>,
  onCancel: Function,
  handleSave: Function,
  suggestGroups: Function,
};

type State = {
  groups: Array<TCustomerGroupShort>,
};

export default class SearchGroupModal extends Component {

  props: Props;
  state: State = {
    groups: [],
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
    return isEmpty(this.state.groups);
  }

  @autobind
  handleSave() {
    this.props.handleSave(this.state.groups);
  }

  @autobind
  handleSelect(groups: Array<TCustomerGroupShort>) {
    this.setState({ groups });
  }

  render() {
    const props = this.props;
    return (
      <ModalContainer isVisible={props.isVisible}>
        <ContentBox
          title="Select Groups"
          actionBlock={this.actionBlock}
          footer={this.footer}
          className="fc-add-watcher-modal"
        >
          <div className="fc-modal-body fc-add-watcher-modal__content">
            <GroupsTypeahead
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
