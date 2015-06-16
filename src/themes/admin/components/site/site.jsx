'use strict';

import React from 'react';
import { RouteHandler } from 'react-router';
import Header from '../header/header';
import Menu from '../menu/menu';
import Modal from '../modal/modal';

class Site extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isModalOpen: false
    };
  }

  openModal() {
    this.setState({
      isModalOpen: true
    });
  }

  closeModal() {
    this.setState({
      isModalOpen: false
    });
  }

  render() {
    return (
      <div>
        <Menu/>
        <Header/>
        <main role='main'>
          <RouteHandler/>
        </main>
        <Modal isOpen={this.state.isModalOpen} openHandler={this.openModal.bind(this)} closeHandler={this.closeModal.bind(this)} />
        <a className='modalbtn btn' onClick={this.openModal.bind(this)}>Toggle Modal</a>
      </div>
    );
  }
}

export default Site;
