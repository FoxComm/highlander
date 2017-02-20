// @flow

// libs
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';

// components
import { ModalContainer } from '../modal/base';
import ContentBox from '../content-box/content-box';
import SaveCancel from '../common/save-cancel';
import AdminsTypeahead from '../users-typeahead/admins-typeahead';

type Props = {
  isVisible: boolean,
  onCancel: (event: SyntheticEvent) => void,
  onConfirm: (users: Array<TUser>) => void,
  title: string|Element<*>,
  bodyLabel: string|Element<*>,
  saveLabel: string,
  maxUsers?: number,
}

type State = {
  selected: Array<TUser>,
}

class SelectAdminsModal extends Component {
  props: Props;
  state: State = {
    selected: [],
  };

  get actionBlock() {
    return (
      <a className='fc-modal-close' onClick={this.props.onCancel}>
        <i className='icon-close'></i>
      </a>
    );
  }

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
        className="fc-modal-footer fc-add-watcher-modal__footer"
        onCancel={props.onCancel}
        onSave={this.handleSave}
        saveDisabled={this.isSaveDisabled}
        saveText={props.saveLabel}
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
      <ModalContainer isVisible={props.isVisible}>
        <ContentBox
          title={props.title}
          actionBlock={this.actionBlock}
          footer={this.footer}
          className="fc-add-watcher-modal"
        >
          <div className="fc-modal-body fc-add-watcher-modal__content">
            <AdminsTypeahead
              hideOnBlur
              onSelect={this.handleAdminsSelected}
              label={props.bodyLabel}
              maxUsers={props.maxUsers}
            />
          </div>
        </ContentBox>
      </ModalContainer>
    );
  }
}

export default SelectAdminsModal;
