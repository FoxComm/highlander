import React, { PropTypes } from 'react';
import moment from 'moment';
import Currency from '../common/currency';
import _ from 'lodash';

export default class Customer extends React.Component {
  static propTypes ={
    customer: PropTypes.object.isRequired
  }

  get customerName() {
    const customer = this.props.customer;
    if (customer.name) {
      return (
        <div className="fc-customer-info-name">
          {customer.name}
        </div>
      );
    }
    return null;
  }

  get customerRank() {
    const customer = this.props.customer;
    if (_.isNumber(customer.rank)) {
      return (
        <div className="fc-customer-info-rank">
          Top {customer.rank}%
        </div>
      );
    }
    return null;
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
            {this.customerRank}
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
              <div className="fc-col-md-1-1 fc-customer-name-block">
                {this.customerName}
                <div className="fc-customer-info-email">
                  {customer.email}
                </div>
              </div>
              <div className="fc-col-md-1-1 fc-customer-details-block">
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
                      <span className="fc-customer-info-comment">&nbsp;Date joined</span>
                    </li>
                  </ul>
                  <ul className="fc-customer-info-fields">
                    <li>
                      <i className="icon-mobile"></i><span>{ customer.modality }</span>
                    </li>
                    <li>
                      <i className="icon-usd"></i>
                      <span><Currency value={customer.totalSales} /></span>
                      <span className="fc-customer-info-comment">&nbsp;Total Sales</span>
                    </li>
                    <li className="fc-customer-info-days">
                      <i>{ customer.id}</i><span>Days since last visit</span>
                    </li>
                    <li className="fc-customer-info-days">
                      <i>{ customer.id}</i><span>Days since last order</span>
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
