import React from 'react';
<<<<<<< f6d1367b0ef95ab0cd682cfb4d03307e200e32bc
=======

import NavigationItem from './navigation-item';
>>>>>>> collapse/expand
import { IndexLink, Link } from '../link';

export default class Navigation extends React.Component {
  render() {
    return (
      <nav>
        <ul>
          <li><NavigationItem to="home" icon="icon-home" title="Home" isIndex={true} /></li>
          <li>
            <NavigationItem to="customers" icon="icon-customers" title="Customers" isIndex={true} isExpandable={true} />
          </li>
          <li>
            <NavigationItem to="orders" icon="icon-orders" title="Orders" isIndex={true} isExpandable={true} />
          </li>
          <li>
            <NavigationItem to="rmas" icon="icon-returns" title="Returns" isIndex={true} isExpandable={true} />
          </li>
          <li><a href=""><i className="icon-items"></i>Items</a></li>
          <li><a href=""><i className="icon-inventory"></i>Inventory</a></li>
          <li>
            <NavigationItem to="gift-cards" icon="icon-gift-cards" title="Gift Cards" isIndex={true} isExpandable={true} />
          </li>
          <li><a href=""><i className="icon-discounts"></i>Discounts</a></li>
          <li><a href=""><i className="icon-settings"></i>Settings</a></li>
        </ul>
      </nav>
    );
  }
}
