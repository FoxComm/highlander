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
        <ul className="fc-sidebar__navigation-list">
          <li>
            <NavigationItem to="home" icon="icon-home" title="Home" isIndex={true} routes={this.props.routes}
                            params={this.props.params}
                            collapsed={this.props.collapsed} />
          </li>
          <li>
            <NavigationItem to="customers"
                            icon="icon-customers"
                            title="Customers"
                            isIndex={true}
                            isExpandable={true}
                            routes={this.props.routes}
                            collapsed={this.props.collapsed} >
              <IndexLink to="customers" className="fc-navigation-item__sublink">Lists</IndexLink>
              <IndexLink to="groups" className="fc-navigation-item__sublink">Customer Groups</IndexLink>
              <a href="" className="fc-navigation-item__sublink">Insights</a>
              <a href="" className="fc-navigation-item__sublink">Activity Trial</a>
            </NavigationItem>
          </li>
          <li>
            <NavigationItem to="orders"
                            icon="icon-orders"
                            title="Orders"
                            isIndex={true}
                            isExpandable={true}
                            routes={this.props.routes}
                            collapsed={this.props.collapsed} >
              <IndexLink to="orders" className="fc-navigation-item__sublink">Lists</IndexLink>
              <a href="" className="fc-navigation-item__sublink">Insights</a>
              <a href="" className="fc-navigation-item__sublink">Activity Trail</a>
            </NavigationItem>
          </li>
          <li>
            <NavigationItem to="rmas"
                            icon="icon-returns"
                            title="Returns"
                            isIndex={true}
                            isExpandable={true}
                            routes={this.props.routes}
                            collapsed={this.props.collapsed} >
              <a href="" className="fc-navigation-item__sublink">Lists</a>
              <a href="" className="fc-navigation-item__sublink">Returns</a>
            </NavigationItem>
          </li>
          <li><a href="" className="fc-navigation-link"><i className="icon-items"></i>Items</a></li>
          <li><a href="" className="fc-navigation-link"><i className="icon-inventory"></i>Inventory</a></li>
          <li>
            <NavigationItem to="gift-cards"
                            icon="icon-gift-cards"
                            title="Gift Cards"
                            isIndex={true}
                            isExpandable={true}
                            routes={this.props.routes}
                            collapsed={this.props.collapsed} >
              <IndexLink to="gift-cards" className="fc-navigation-item__sublink">Lists</IndexLink>
              <a href="" className="fc-navigation-item__sublink">Insights</a>
              <a href="" className="fc-navigation-item__sublink">Activity Trail</a>
            </NavigationItem>
          </li>
          <li><a href="" className="fc-navigation-link"><i className="icon-discounts"></i>Discounts</a></li>
          <li><a href="" className="fc-navigation-link"><i className="icon-settings"></i>Settings</a></li>
        </ul>
      </nav>
    );
  }
}
