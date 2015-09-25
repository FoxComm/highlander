'use strict';

import React from 'react';
import { Link } from 'react-router';
import Navigation from './navigation';

export default class Sidebar extends React.Component {
  render() {
    return (
      <aside role='complimentary' className='fc-sidebar'>
        <div className='logo'>
          <img src="https://s3-us-west-2.amazonaws.com/fc-ashes/images/fc-logo-nav.svg"></img>
        </div>
        <Navigation/>
      </aside>
    );
  }
}
