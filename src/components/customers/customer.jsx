'use strict';

import React, { PropTypes } from 'react';
import { Link, IndexLink } from '../link';
import TitleBlock from './title-block';
import { connect } from 'react-redux';
import * as CustomersActions from '../../modules/customers/details';

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
        customer: this.props.details
      });
    }.bind(this));
  }

  get page() {
    return (
      <div className="fc-customer">
        <div className="gutter">
          <TitleBlock customer={this.props.details} />
        </div>
        <div className="gutter">
          <ul className="fc-tabbed-nav">
            <li><a href="">Insights</a></li>
            <li><IndexLink to="customer-details" params={this.props.params}>Details</IndexLink></li>
            <li><a href="">Transaction</a></li>
            <li><a href="">Items</a></li>
            <li><a href="">Store Credit</a></li>
            <li><a href="">Notifications</a></li>
            <li><a href="">Reviews</a></li>
            <li><Link to="customer-notes" params={this.props.params}>Notes</Link></li>
            <li><a href="">Activity Trail</a></li>
          </ul>
          <div>
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
