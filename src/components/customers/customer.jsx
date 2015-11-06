'use strict';

import React, { PropTypes } from 'react';
import { Link, IndexLink } from '../link';
import TitleBlock from './title-block';
import { connect } from 'react-redux';
import * as CustomersActions from '../../modules/customers/details';
import LocalNav from '../local-nav/local-nav';

@connect((state, props) => ({
  ...state.customers.details[props.params.customer]
}), CustomersActions)
export default class Customer extends React.Component {

  static propTypes = {
    params: PropTypes.shape({
      customer: PropTypes.string.isRequired
    }).isRequired,
    details: PropTypes.object,
    children: PropTypes.node
  };

  componentDidMount() {
    const { customer } = this.props.params;

    this.props.fetchCustomer(customer);
  }

  renderChildren() {
    return React.Children.map(this.props.children, function (child) {
      return React.cloneElement(child, {
        entity: this.props.details
      });
    }.bind(this));
  }

  get page() {
    return (
      <div className="fc-customer">
        <div className="fc-grid">
          <div className="fc-col-md-1-1">
            <TitleBlock customer={this.props.details} />
          </div>
        </div>
        <LocalNav gutter={true}>
          <a href="">Insights</a>
          <IndexLink to="customer-details" params={this.props.params}>Details</IndexLink>
          <li>
            <a>Transaction</a>
            <ul className="fc-tabbed-nav-dropdown">
              <li><Link to="customer-returns" params={this.props.params}>Returns</Link></li>
            </ul>
          </li>
          <a href="">Items</a>
          <a href="">Store Credit</a>
          <a href="">Notifications</a>
          <a href="">Reviews</a>
          <Link to="customer-notes" params={this.props.params}>Notes</Link>
          <a href="">Activity Trail</a>
        </LocalNav>
        <div className="fc-grid">
          <div className="fc-col-md-1-1">
            { this.renderChildren() }
          </div>
        </div>
      </div>
    );
  }

  render() {
    return (this.props.details ? this.page : null);
  }
}
