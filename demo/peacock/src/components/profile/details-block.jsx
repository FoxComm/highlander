/* @flow */

import React, { Component, Element } from 'react';

// components
import ActionLink from 'ui/action-link/action-link';
import Modal from 'ui/modal/modal';

import styles from './profile.css';

type Props = {
  data?: string | Element<*>,
  toggleModal: () => void,
  modalVisible: boolean,
  actionTitle: string,
  blockTitle: string,
  modalContent: Element<*>,
  actionIcon?: {
    name: string,
    className: string,
  },
};

class DetailsBlock extends Component {
  props: Props;

  get action() {
    const { actionTitle, actionIcon } = this.props;

    return (
      <ActionLink
        action={this.props.toggleModal}
        title={actionTitle}
        styleName="action-link"
        icon={actionIcon}
      />
    );
  }

  get content() {
    const { data, toggleModal, modalVisible, modalContent } = this.props;

    return (
      <div styleName="content">
        {data}
        <Modal
          show={modalVisible}
          toggle={toggleModal}
        >
          {modalContent}
        </Modal>
      </div>
    );
  }

  render() {
    const { blockTitle } = this.props;

    return (
      <div styleName="details-block">
        <div styleName="header">
          <div styleName="title">{blockTitle}</div>
          {this.action}
        </div>
        {this.content}
      </div>
    );
  }
}

export default DetailsBlock;
