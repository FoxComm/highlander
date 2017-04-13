/* @flow */

import React, { Component } from 'react';

import ActionLink from 'ui/action-link/action-link';
import Modal from 'ui/modal/modal';
import EditName from './edit-name';

import styles from './account-details.css';

type Props = {
  name: string,
  toggleModal: () => void,
  modalVisible: boolean,
};

class Name extends Component {
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
    const { name, toggleModal, modalVisible } = this.props;
    return (
      <div styleName="content">
        {name}
        <Modal
          show={modalVisible}
          toggle={toggleModal}
        >
          <EditName />
        </Modal>
      </div>
    );
  }

  render() {
    return (
      <div styleName="name-block">
        <div styleName="header">
          <div styleName="title">First and last name</div>
          {this.action}
        </div>
        {this.content}
      </div>
    );
  }
}

export default Name;
