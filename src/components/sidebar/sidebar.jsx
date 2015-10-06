'use strict';

import React from 'react';
import Navigation from './navigation';
import static_url from '../../lib/s3';

export default class Sidebar extends React.Component {
  render() {
    return (
      <aside role='complimentary' className='fc-sidebar'>
        <div className='logo'>
          <img src={static_url('images/fc-logo-nav.svg')}></img>
        </div>
        <Navigation/>
      </aside>
    );
  }
}
