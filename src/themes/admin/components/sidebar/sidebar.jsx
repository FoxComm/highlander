'use strict';

import React from 'react';
import { Link } from 'react-router';

export default class Sidebar extends React.Component {
  render() {
    return (
      <aside role='complementary' className='fc-sidebar'>
        <nav>
          <ul>
            <li><Link to='home'><i className="fa fa-tachometer"></i>Dashboard</Link></li>
            <li><Link to='rmas'><i className="fa fa-exchange"></i>Returns</Link></li>
            <li><Link to='orders'><i className="fa fa-files-o"></i>Orders</Link></li>
            <li><Link to="customers"><i className="fa fa-users"></i>Customers</Link></li>
            <li><a href=""><i className="fa fa-tags"></i>Products</a></li>
            <li><Link to="gift-cards"><i className="fa fa-gift"></i>Gift Cards</Link></li>
          </ul>
        </nav>
      </aside>
    );
  }
}
