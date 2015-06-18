'use strict';

import React from 'react';
import { RouteHandler } from 'react-router';
import Header from '../header/header';
import Menu from '../menu/menu';
import Modal from '../modal/modal';
import { dispatch } from '../../lib/dispatcher';

export default class Site extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isModalOpen: false
    };
  }

  openModal() {
    dispatch('openModal');
  }

  render() {
    return (
      <div>
        <Menu/>
        <Header/>
        <main role='main'>
          <RouteHandler/>
        </main>
        <Modal isOpen={this.state.isModalOpen} />
        <a className='modalbtn btn' onClick={this.openModal}>Toggle Modal</a>
      </div>
    );
  }
}
