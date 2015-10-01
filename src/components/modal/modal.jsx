'use strict';

import React from 'react/addons';
import { listenTo, stopListeningTo } from '../../lib/dispatcher';
import _ from 'lodash';

const toggleEvent = 'toggle-modal';

export default class Modal extends React.Component {
  constructor(props) {
    super(props);
    this.onToggleModal = this.onToggleModal.bind(this);
    this.state = {
      component: null,
      isOpen: false
    };

    this.boundKeyUpHandler = _.bind(this.handleKeyUp, this);
  }

  onToggleModal(component) {
    let isOpen = !this.state.isOpen;
    component = component || null;
    this.setState({
      isOpen: isOpen,
      component: React.addons.createFragment({'component': component})
    });

    document.body.classList[isOpen ? 'add' : 'remove']('fc-is-modal-opened');
  }

  componentDidMount() {
    listenTo(toggleEvent, this);
    document.addEventListener('keyup', this.boundKeyUpHandler, true);
  }

  componentWillUnmount() {
    stopListeningTo(toggleEvent, this);
    document.removeEventListener('keyup', this.boundKeyUpHandler, true);
  }

  handleKeyUp(event) {
    if (this.state.isOpen && event.keyCode == 27) {
      this.setState({
        isOpen: false,
        component: null
      });
    }
  }

  render() {
    return (
      <div role='dialog' className={`fc-modal ${this.state.isOpen ? null : 'is-hidden'}`}>
        <div className="fc-modal-container">
          {this.state.component}
        </div>
      </div>
    );
  }
}
