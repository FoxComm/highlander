'use strict';

import React from 'react';

import ResendModal from '../notifications/resend.jsx';
import { listenTo, stopListeningTo } from '../../lib/dispatcher';

const openEvent = 'open-modal';
const closeEvent = 'close-modal';

export default class Modal extends React.Component {
  constructor(props) {
    super(props);
    this.onOpenModal = this.onOpenModal.bind(this);
    this.onCloseModal = this.onCloseModal.bind(this);
    this.state = {
      component: <ResendModal />,
      isModalOpen: false
    };
  }

  onOpenModal() {
    this.setState({
      isModalOpen: true
    });
  }

  onCloseModal() {
    this.setState({
      isModalOpen: false
    });
  }

  componentDidMount() {
    listenTo(openEvent, this);
    listenTo(closeEvent, this);
  }

  componentWillUnmount() {
    stopListeningTo(openEvent, this);
    stopListeningTo(closeEvent, this);
  }

  render() {
    return (
      <div>
        <div role='dialog' id='modal-wrap' className={this.state.isModalOpen ? 'show' : 'hide'}>
          <div className='modal-overlay'></div>
          <div className='modal'>
            {this.state.component}
          </div>
        </div>
      </div>
    );
  }
}
