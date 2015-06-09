'use strict';

import React from 'react';
import { Link } from 'react-router';

class Menu extends React.Component {
  render() {
    return (
      <aside role='complementary'>
        <nav>
          <ul>
            <li><Link to='home'><i className="icon-gauge"></i>Dashboard</Link></li>
            <li><Link to='orders'><i className="icon-docs"></i>Orders</Link></li>
            <li><Link to="users"><i className="icon-group"></i>Users</Link></li>
            <li><a href=""><i className="icon-tags"></i>Products</a></li>
          </ul>
        </nav>
      </aside>
    );
  }
}

export default Menu;
