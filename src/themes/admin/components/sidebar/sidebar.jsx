'use strict';

import React from 'react';
import { Link } from 'react-router';

export default class Sidebar extends React.Component {
  render() {
    return (
      <aside role='complementary'>
        <nav>
          <ul>
            <li><Link to='home'><i className="icon-gauge"></i>Dashboard</Link></li>
            <li><Link to='orders'><i className="icon-docs"></i>Orders</Link></li>
            <li><Link to="customers"><i className="icon-group"></i>Customers</Link></li>
            <li><a href=""><i className="icon-tags"></i>Products</a></li>
            <li><Link to="gift-cards"><i className="icon-gift"></i>Gift Cards</Link></li>
          </ul>
        </nav>
      </aside>
    );
  }
}
