'use strict';

import React, { PropTypes } from 'react';

export default class Customer extends React.Component {
  static propTypes ={
    customer: PropTypes.object.isRequired
  }

  get customer() {
    return this.props.customer;
  }

  render() {
    return (
      <div className="fc-content-box">
        <div className="fc-customer-info-header">
          <div className="fc-customer-info-head">
            <div className="fc-customer-info-rank">
              {this.customer.rank}
            </div>
          </div>
          <div className="fc-customer-info-avatar">
            <i className="icon-customer"></i>
          </div>
          <div className="fc-customer-info-name">
            {this.customer.firstName} {this.customer.lastName}
          </div>
          <div className="fc-customer-info-email">
            {this.customer.email}
          </div>
        </div>
        <article className="fc-customer-info-body">
          <div className="fc-grid">
            <div className="fc-col-1-2">
              <ul className="fc-customer-info-fields">
                <li>
                  <i className="icon-customer"></i><span>{ this.customer.id }</span>
                </li>
                <li>
                  <i className="icon-phone"></i><span>{ this.customer.phoneNumber }</span>
                </li>
                <li>
                  <i className="icon-location"></i><span>{ this.customer.location }</span>
                </li>
                <li>
                  <i className="icon-calendar"></i>
                  <span>{ this.customer.createdAt }</span>
                  <span className="fc-comment">Date joined</span>
                </li>
              </ul>
            </div>
            <div className="fc-col-1-2">
              <ul className="fc-customer-info-fields">
                <li>
                  <i className="icon-mobile"></i><span>{ this.customer.modality }</span>
                </li>
                <li>
                  <i className="icon-usd"></i>
                  <span>{ this.customer.totalSpent}</span>
                  <span className="fc-comment">Total Sales</span>
                </li>
                <li>
                  <i>{ this.customer.id}</i><span>Since last visit</span>
                </li>
                <li>
                  <i>{ this.customer.id}</i><span>Since last visit</span>
                </li>
              </ul>
            </div>
          </div>
        </article>
      </div>
    );
  }
}
