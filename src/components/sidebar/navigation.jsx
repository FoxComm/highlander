'use strict';

import React from 'react';
import { IndexLink, Link } from '../link';

export default class Navigation extends React.Component {
  render() {
    return (
      <nav>
        <ul>
          <li><IndexLink to='home'><i className="icon-home"></i>Dashboard</IndexLink></li>
          <li><Link to="customers"><i className="icon-customers"></i>Customers</Link></li>
          <li><Link to='orders'><i className="icon-orders"></i>Orders</Link></li>
          <li><Link to='rmas'><i className="icon-returns"></i>Returns</Link></li>
          <li><a href=""><i className="icon-items"></i>Items</a></li>
          <li><a href=""><i className="icon-inventory"></i>Inventory</a></li>
          <li><Link to="gift-cards"><i className="icon-gift-cards"></i>Gift Cards</Link></li>
          <li><a href=""><i className="icon-discounts"></i>Discounts</a></li>
          <li><a href=""><i className="icon-settings"></i>Settings</a></li>
        </ul>
      </nav>
    );
  }
}
