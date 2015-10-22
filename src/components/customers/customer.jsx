'use strict';

import React, { PropTypes } from 'react';
import { Link, IndexLink } from '../link';
import TitleBlock from './title-block';

export default class Customer extends React.Component {

  static propTypes = {
    params: PropTypes.shape({
      customer: PropTypes.string.isRequired
    }).isRequired,
    customerDetails: PropTypes.object,
    children: PropTypes.node
  };


  renderChildren() {
    return React.Children.map(this.props.children, function (child) {
      return React.cloneElement(child, {
        customer: this.props.customerDetails
      });
    }.bind(this));
  }

  render() {
    let page = null;
    if (this.props.customerDetails) {
      page = (
        <div className="fc-customer">
          <div className="gutter">
            <TitleBlock customer={this.props.customerDetails} />
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
              { this.renderChildren() }
            </div>
          </div>
        </div>
      );
    }
    return page;
  }
}
