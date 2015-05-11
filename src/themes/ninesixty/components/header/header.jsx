'use strict';

import React from 'react';
import { Link } from 'react-router';

class Header extends React.Component {
  render() {
    return (
      <header role='banner'>
        <div className="grid-row">
          <nav>
            <ul>
              <li><Link to='home'>Home</Link></li>
            </ul>
          </nav>
        </div>
      </header>
    );
  }
}

export default Header;
