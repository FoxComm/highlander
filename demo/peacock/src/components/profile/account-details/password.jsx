/* @flow */

import React, { Component } from 'react';

import ActionLink from 'ui/action-link/action-link';
import Modal from 'ui/modal/modal';
import ChangePassword from './change-password';

import styles from './account-details.css';

import type { AccountDetailsProps } from 'types/profile';

type Props = AccountDetailsProps & {
  modalVisible: boolean,
  toggleModal: () => void,
};

class Password extends Component {
  props: Props;

  get action() {
    return (
      <ActionLink
        action={this.props.toggleModal}
        title="Change"
        styleName="action-link"
      />
    );
  }

  get content() {
    const {account, toggleModal, modalVisible } = this.props;
    return (
      <div styleName="content">
        <Modal
          show={modalVisible}
          toggle={toggleModal}
        >
          <ChangePassword
            account={account}
          />
        </Modal>
      </div>
    );
  }

  render() {
    return (
      <div styleName="password-block">
        <div styleName="header">
          <div styleName="title">Password</div>
          {this.action}
        </div>
        {this.content}
      </div>
    );
  }
}

export default Password;
