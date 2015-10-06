'use strict';

import React from 'react';
import { Link, IndexLink } from '../link';

export default class Sidebar extends React.Component {
  render() {
    return (
      <aside role='complementary' className='fc-sidebar'>
        <nav>
          <ul>
            <li><IndexLink to='home'><i className="icon-home"></i>Dashboard</IndexLink></li>
            <li><Link to='rmas'><i className="icon-returns"></i>Returns</Link></li>
            <li><Link to='orders'><i className="icon-orders"></i>Orders</Link></li>
            <li><Link to="customers"><i className="icon-customers"></i>Customers</Link></li>
            <li><a href=""><i className="icon-items"></i>Products</a></li>
            <li><Link to="gift-cards"><i className="icon-gift-cards"></i>Gift Cards</Link></li>
          </ul>
        </nav>
      </aside>
    );
  }
}
