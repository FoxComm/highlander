
import _ from 'lodash';
import React, { PropTypes } from 'react';

import NavigationItem from './navigation-item';
import { IndexLink, Link } from '../link';

function getMenuItemState(props, to) {
  console.log(props);
  const isOpen = _.get(props, ['menuItems', to]);
  return isOpen;
}

const Navigation = props => {
  return (
    <nav>
      <ul className="fc-sidebar__navigation-list">
        <li>
          <NavigationItem to="home"
                          icon="icon-home"
                          title="Home"
                          isIndex={true}
                          routes={props.routes}
                          collapsed={props.collapsed} />
        </li>
        <li>
          <NavigationItem to="customers"
                          icon="icon-customers"
                          title="Customers"
                          isIndex={true}
                          isExpandable={true}
                          routes={props.routes}
                          collapsed={props.collapsed}
                          status={getMenuItemState(props, 'customers')}
                          toggleMenuItem={props.toggleMenuItem} >
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
                          routes={props.routes}
                          collapsed={props.collapsed}
                          status={getMenuItemState(props, 'orders')}
                          toggleMenuItem={props.toggleMenuItem} >
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
                          routes={props.routes}
                          collapsed={props.collapsed}
                          status={getMenuItemState(props, 'rmas')}
                          toggleMenuItem={props.toggleMenuItem} >
            <IndexLink to="rmas" className="fc-navigation-item__sublink">Lists</IndexLink>
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
                          routes={props.routes}
                          collapsed={props.collapsed}
                          status={getMenuItemState(props, 'gift-cards')}
                          toggleMenuItem={props.toggleMenuItem} >
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
};

Navigation.propTypes = {
  routes: PropTypes.array.isRequired,
  collapsed: PropTypes.bool,
  getMenuItemState: PropTypes.func.isRequired,
  toggleMenuItem: PropTypes.func.isRequired
};

Navigation.defaultProps = {
  collapsed: false
};

export default Navigation;
