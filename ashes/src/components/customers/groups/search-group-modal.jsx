/* @flow */

// libs
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { isEmpty } from 'lodash';

// components
import Modal from 'components/core/modal';
import SaveCancel from 'components/core/save-cancel';
import GroupsTypeahead from './groups-typeahead';

type Props = {
  isVisible: boolean,
  suggestState: Object,
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

  @autobind
  handleSave() {
    this.props.handleSave(this.state.groups);
  }

  @autobind
  handleSelect(groups: Array<TCustomerGroupShort>) {
    this.setState({ groups });
  }

  get footer() {
    const saveDisabled = isEmpty(this.state.groups);

    return <SaveCancel onCancel={this.props.onCancel} onSave={this.handleSave} saveDisabled={saveDisabled} />;
  }

  render() {
    const props = this.props;

    return (
      <Modal title="Select Groups" footer={this.footer} isVisible={props.isVisible} onClose={props.onCancel}>
        <GroupsTypeahead
          suggestGroups={props.suggestGroups}
          suggested={props.suggested}
          suggestState={props.suggestState}
          onSelect={this.handleSelect}
          view="modal"
        />
      </Modal>
    );
  }
}
