
import _ from 'lodash';
import React, { PropTypes } from 'react';

import NavigationItem from './navigation-item';
import { IndexLink, Link } from '../link';

function getMenuItemState(props, to) {
  return _.get(props, ['menuItems', to]);
}

function goNowhere(e) {
  e.preventDefault();
}

const Navigation = props => {
  return (
    <nav>
      <ul className="fc-sidebar__navigation-list">
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
            <Link to="customers-activity-trail" className="fc-navigation-item__sublink">
              Activity Trail
            </Link>
          </NavigationItem>
        </li>
        <li>
          <NavigationItem to="carts"
                          icon="icon-orders"
                          title="Carts"
                          isIndex={true}
                          isExpandable={true}
                          routes={props.routes}
                          collapsed={props.collapsed}
                          status={getMenuItemState(props, 'carts')}
                          toggleMenuItem={props.toggleMenuItem} >
            <IndexLink to="carts" className="fc-navigation-item__sublink">Lists</IndexLink>
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
            <Link to="orders-activity-trail" className="fc-navigation-item__sublink">
              Activity Trail
            </Link>
          </NavigationItem>
        </li>
        <li>
          <NavigationItem to="products"
                          icon="icon-items"
                          title="Products"
                          isIndex={true}
                          isExpandable={true}
                          routes={props.routes}
                          collapsed={props.collapsed}
                          status={getMenuItemState(props, 'products')}
                          toggleMenuItem={props.toggleMenuItem}>
            <IndexLink to="products" className="fc-navigation-item__sublink">Lists</IndexLink>
            <a href="" className="fc-navigation-item__sublink" onClick={goNowhere}>Activity Trail</a>
          </NavigationItem>
        </li>
        <li>
          <NavigationItem to="skus"
                          icon="icon-barcode"
                          title="SKUs"
                          isIndex={true}
                          isExpandable={true}
                          routes={props.routes}
                          collapsed={props.collapsed}
                          status={getMenuItemState(props, 'skus')}
                          toggleMenuItem={props.toggleMenuItem}>
            <IndexLink to="skus" className="fc-navigation-item__sublink">Lists</IndexLink>
            <a href="" className="fc-navigation-item__sublink" onClick={goNowhere}>Activity Trail</a>
          </NavigationItem>
        </li>
        <li>
          <NavigationItem to="inventory"
                          icon="icon-inventory"
                          title="Inventory"
                          isIndex={true}
                          isExpandable={true}
                          routes={props.routes}
                          collapsed={props.collapsed}
                          status={getMenuItemState(props, 'inventory')}
                          toggleMenuItem={props.toggleMenuItem} >
              <IndexLink to="inventory" className="fc-navigation-item__sublink">Lists</IndexLink>

              <Link to="inventory-activity-trail" className="fc-navigation-item__sublink">
                Activity Trail
              </Link>
          </NavigationItem>
        </li>
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

            <Link to="gift-cards-activity-trail" className="fc-navigation-item__sublink">
              Activity Trail
            </Link>
          </NavigationItem>
        </li>
        <li>
          <NavigationItem to="promotions"
                          icon="icon-promotion"
                          title="Promotions"
                          isIndex={true}
                          isExpandable={true}
                          routes={props.routes}
                          collapsed={props.collapsed}
                          status={getMenuItemState(props, 'promotions')}
                          toggleMenuItem={props.toggleMenuItem} >
            <IndexLink to="promotions" className="fc-navigation-item__sublink">Lists</IndexLink>
            <Link to="promotions-activity-trail" className="fc-navigation-item__sublink">
              Activity Trail
            </Link>
          </NavigationItem>
        </li>
        <li>
          <NavigationItem to="coupons"
                          icon="icon-discounts"
                          title="Coupons"
                          isIndex={true}
                          isExpandable={true}
                          routes={props.routes}
                          collapsed={props.collapsed}
                          status={getMenuItemState(props, 'coupons')}
                          toggleMenuItem={props.toggleMenuItem} >
            <IndexLink to="coupons" className="fc-navigation-item__sublink">Lists</IndexLink>
            <Link to="coupons-activity-trail" className="fc-navigation-item__sublink">
              Activity Trail
            </Link>
          </NavigationItem>
        </li>
        <li>
          <NavigationItem to="users"
                          icon="icon-settings"
                          title="Settings"
                          isIndex={true}
                          isExpandable={true}
                          routes={props.routes}
                          collapsed={props.collapsed}
                          status={getMenuItemState(props, 'settings')}
                          toggleMenuItem={props.toggleMenuItem} >
            <IndexLink to="users" className="fc-navigation-item__sublink">Users</IndexLink>
            <Link to="plugins" className="fc-navigation-item__sublink">
              Plugins
            </Link>
            <a href="" className="fc-navigation-item__sublink" onClick={goNowhere}>Roles & Permissions</a>
            <a href="" className="fc-navigation-item__sublink" onClick={goNowhere}>Integrations</a>
          </NavigationItem>
        </li>
      </ul>
    </nav>
  );
};

Navigation.propTypes = {
  routes: PropTypes.array.isRequired,
  collapsed: PropTypes.bool,
  toggleMenuItem: PropTypes.func.isRequired
};

Navigation.defaultProps = {
  collapsed: false
};

export default Navigation;
