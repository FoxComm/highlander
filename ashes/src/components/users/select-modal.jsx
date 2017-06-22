// @flow

// libs
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';

// components
import Modal from 'components/core/modal';
import SaveCancel from 'components/core/save-cancel';
import AdminsTypeahead from '../users-typeahead/admins-typeahead';

type Props = {
  isVisible: boolean,
  onCancel: (event: SyntheticEvent) => void,
  onConfirm: (users: Array<TUser>) => void,
  title: string | Element<*>,
  bodyLabel: string | Element<*>,
  saveLabel: string,
  maxUsers?: number,
};

type State = {
  selected: Array<TUser>,
};

class SelectAdminsModal extends Component {
  props: Props;
  state: State = {
    selected: [],
  };

  get isSaveDisabled(): boolean {
    return !this.state.selected.length;
  }

  @autobind
  handleSave() {
    this.props.onConfirm(this.state.selected);
  }

  get footer() {
    const { props } = this;

    return (
      <SaveCancel
        onCancel={props.onCancel}
        onSave={this.handleSave}
        saveDisabled={this.isSaveDisabled}
        saveLabel={props.saveLabel}
      />
    );
  }

  @autobind
  handleAdminsSelected(admins: Array<TUser>) {
    this.setState({
      selected: admins,
    });
  }

  render() {
    const { props } = this;

    return (
      <Modal isVisible={props.isVisible} title={props.title} footer={this.footer}>
        <AdminsTypeahead
          hideOnBlur
          onSelect={this.handleAdminsSelected}
          label={props.bodyLabel}
          maxUsers={props.maxUsers}
          view="modal"
        />
      </Modal>
    );
  }
}

export default SelectAdminsModal;
