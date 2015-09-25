'use strict';

import React from 'react/addons';

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
    component = component || null;
    this.setState({
      isModalOpen: isOpen,
      component: React.addons.createFragment({'component': component})
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
        <div role='dialog' className={this.state.isModalOpen ? 'is-shown' : 'is-hidden'}>
          <div className='fc-modal-overlay'></div>
          <div className='fc-modal'>
            {this.state.component}
          </div>
        </div>
      </div>
    );
  }
}
