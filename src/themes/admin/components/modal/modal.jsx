'use strict';

import React from 'react';

import ResendModal from '../notifications/resend.jsx';
import { listenTo, stopListeningTo } from '../../lib/dispatcher';

const toggleEvent = 'toggle-modal';

export default class Modal extends React.Component {
  constructor(props) {
    super(props);
    this.onToggleModal = this.onToggleModal.bind(this);
    this.state = {
      component: <ResendModal />,
      isModalOpen: false
    };
  }

  onToggleModal() {
    this.setState({
      isModalOpen: !this.state.isModalOpen
    });
  }

  componentDidMount() {
    listenTo(toggleEvent, this);
  }

  componentWillUnmount() {
    stopListeningTo(toggleEvent, this);
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
