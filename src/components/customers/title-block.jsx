'use strict';

import React, { PropTypes } from 'react';
import moment from 'moment';

export default class Customer extends React.Component {
  static propTypes ={
    customer: PropTypes.object.isRequired
  }

  get customer() {
    return this.props.customer;
  }

  render() {
    let joinedAt = '';
    if (this.customer.createdAt !== undefined) {
      joinedAt = moment(this.customer.createdAt).format('MM/DD/YYYY HH:mm:ss');
    }
    return (
      <div className="fc-content-box fc-customer-title-block">
        <div className="fc-customer-info-header">
          <div className="fc-customer-info-head">
            <div className="fc-customer-info-rank">
              {this.customer.rank}
            </div>
          </div>
        </div>
        <article className="fc-customer-info-body">
          <div className="fc-grid">
            <div className="fc-col-1-1">
              <div className="fc-col-1-6">
                <div className="fc-customer-info-avatar">
                  <i className="icon-customer"></i>
                </div>
              </div>
              <div className="fc-col-5-6">
                <div className="fc-col-1-1">
                  <div className="fc-customer-info-name">
                    {this.customer.firstName} {this.customer.lastName}
                  </div>
                  <div className="fc-customer-info-email">
                    {this.customer.email}
                  </div>
                </div>
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
                      <span>{ joinedAt }</span>
                      <span className="fc-comment">&nbsp;Date joined</span>
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
                      <span className="fc-comment">&nbsp;Total Sales</span>
                    </li>
                    <li>
                      <i>{ this.customer.id}</i><span>days since last visit</span>
                    </li>
                    <li>
                      <i>{ this.customer.id}</i><span>days since last order</span>
                    </li>
                  </ul>
                </div>
              </div>
            </div>
          </div>
        </article>
      </div>
    );
  }
}
