'use strict';

import React from 'react';
import { RouteHandler } from 'react-router';
import Header from '../header/header';
import Sidebar from '../sidebar/sidebar';
import Modal from '../modal/modal';

export default class Site extends React.Component {
  render() {
    return (
      <div className="fc-admin">
        <Sidebar/>
        <div className="fc-container">
          <Header/>
          <main role='main' className="fc-main">
            <RouteHandler/>
          </main>
        </div>
        <Modal />
      </div>
    );
  }
}
