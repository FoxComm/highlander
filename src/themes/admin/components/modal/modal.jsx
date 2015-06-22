'use strict';

import React from 'react';

import { listenTo, stopListeningTo } from '../../lib/dispatcher';

const toggleEvent = 'toggle-modal';

export default class Modal extends React.Component {
  constructor(props) {
    super(props);
    this.onToggleModal = this.onToggleModal.bind(this);
    this.state = {
      component: null,
      isModalOpen: false
    };
  }

  onToggleModal(component) {
    let isOpen = !this.state.isModalOpen;
    this.setState({
      isModalOpen: isOpen,
      component: isOpen ? component : null
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
          <div id='modal' className='modal'>
            {this.state.component}
          </div>
        </div>
      </div>
    );
  }
}
