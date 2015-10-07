'use strict';

import React, { PropTypes } from 'react';
import { Link, IndexLink } from '../link';

import CustomerStore from '../../stores/customers';

export default class Customer extends React.Component {

  static propTypes = {
    params: PropTypes.shape({
      customer: PropTypes.string.isRequired
    }).isRequired,
    children: PropTypes.node
  };

  constructor(props, context) {
    super(props, context);
    this.state = {
      customer: {}
    };
  }

  get customerId() {
    return this.props.params.customer;
  }

  componentDidMount() {
    CustomerStore.listenToEvent('change-item', this);
    CustomerStore.fetch(this.customerId);
  }

  componentWillUnmount() {
    CustomerStore.stopListeningToEvent('change-item', this);
  }

  onChangeItemCustomerStore(customer) {
    if (parseInt(this.customerId) !== customer.id) {
      return;
    }

    this.setState({
      customer: customer
    });
  }

  render() {
    console.log(this.props);
    console.log(this.state);

    return (
      <div className="fc-user">
        <div className="gutter">
          <div className="fc-content-box">
            <div className="fc-customer-info-header">
              <div className="fc-customer-info-head">
                <div className="fc-customer-info-rank">
                  Top 10%
                </div>
              </div>
              <div className="fc-customer-info-avatar">
                <i className="icon-customer"></i>
              </div>
              <div className="fc-customer-info-name">
                {this.state.customer.firstName} {this.state.customer.lastName}
              </div>
              <div className="fc-customer-info-email">
                {this.state.customer.email}
              </div>
            </div>
          </div>
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
            <li><a href="">Notes</a></li>
            <li><a href="">Activity Trail</a></li>
          </ul>
          <div>
            {this.props.children}
          </div>
        </div>
      </div>
    );
  }
}
