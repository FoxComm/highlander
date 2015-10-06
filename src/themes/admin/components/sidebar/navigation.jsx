'use strict';

import React from 'react';
import { Link } from 'react-router';

export default class Navigation extends React.Component {
  render() {
    return (
      <nav>
        <ul>
          <li><Link to='home'><i className="icon-home"></i>Dashboard</Link></li>
          <li><Link to='rmas'><i className="icon-returns"></i>Returns</Link></li>
          <li><Link to='orders'><i className="icon-orders"></i>Orders</Link></li>
          <li><Link to="customers"><i className="icon-customers"></i>Customers</Link></li>
          <li><a href=""><i className="icon-items"></i>Products</a></li>
          <li><Link to="gift-cards"><i className="icon-gift-cards"></i>Gift Cards</Link></li>
        </ul>
      </nav>
    );
  }
}
