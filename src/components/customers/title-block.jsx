'use strict';

import React, { PropTypes } from 'react';
import moment from 'moment';

export default class Customer extends React.Component {
  static propTypes ={
    customer: PropTypes.object.isRequired
  }

  render() {
    let customer = this.props.customer;
    let joinedAt = '';
    if (customer.createdAt !== undefined) {
      joinedAt = moment(customer.createdAt).format('MM/DD/YYYY HH:mm:ss');
    }
    return (
      <div className="fc-content-box fc-customer-title-block">
        <div className="fc-customer-info-header">
          <div className="fc-customer-info-head">
            <div className="fc-customer-info-rank">
              {customer.rank}
            </div>
          </div>
        </div>
        <article className="fc-customer-info-body">
          <div className="fc-grid">
            <div className="fc-col-md-1-12">
              <div className="fc-customer-info-avatar">
                <i className="icon-customer"></i>
              </div>
            </div>
            <div className="fc-col-md-11-12">
              <div className="fc-col-md-1-1">
                <div className="fc-customer-info-name">
                  {customer.name}
                </div>
                <div className="fc-customer-info-email">
                  {customer.email}
                </div>
              </div>
              <div className="fc-col-md-1-1">
                  <ul className="fc-customer-info-fields">
                    <li>
                      <i className="icon-customer"></i><span>{ customer.id }</span>
                    </li>
                    <li>
                      <i className="icon-phone"></i><span>{ customer.phoneNumber }</span>
                    </li>
                    <li>
                      <i className="icon-location"></i><span>{ customer.location }</span>
                    </li>
                    <li>
                      <i className="icon-calendar"></i>
                      <span>{ joinedAt }</span>
                      <span className="fc-comment">&nbsp;Date joined</span>
                    </li>
                  </ul>
                  <ul className="fc-customer-info-fields">
                    <li>
                      <i className="icon-mobile"></i><span>{ customer.modality }</span>
                    </li>
                    <li>
                      <i className="icon-usd"></i>
                      <span>{ customer.totalSpent}</span>
                      <span className="fc-comment">&nbsp;Total Sales</span>
                    </li>
                    <li>
                      <i>{ customer.id}</i><span>days since last visit</span>
                    </li>
                    <li>
                      <i>{ customer.id}</i><span>days since last order</span>
                    </li>
                  </ul>
              </div>
            </div>
          </div>
        </article>
      </div>
    );
  }
}
