/* @flow */

import React, { Component } from 'react';

import ActionLink from 'ui/action-link/action-link';
import Modal from 'ui/modal/modal';
import EditEmail from './edit-email';

import styles from './account-details.css';

type Props = {
  email: string,
  toggleModal: () => void,
  modalVisible: boolean,
};

class Email extends Component {
  props: Props;

  get action() {
    return (
      <ActionLink
        action={this.props.toggleModal}
        title="Edit"
        styleName="action-link"
      />
    );
  }

  get content() {
    const { email, toggleModal, modalVisible } = this.props;
    return (
      <div styleName="content">
        {email}
        <Modal
          show={modalVisible}
          toggle={toggleModal}
        >
          <EditEmail />
        </Modal>
      </div>
    );
  }

  render() {
    return (
      <div styleName="email-block">
        <div styleName="header">
          <div styleName="title">Email</div>
          {this.action}
        </div>
        {this.content}
      </div>
    );
  }
}

export default Email;
