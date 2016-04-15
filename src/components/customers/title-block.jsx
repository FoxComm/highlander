//libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import moment from 'moment';

//helpers
import { prefix } from '../../lib/text-utils';

//components
import Currency from '../common/currency';


const prefixed = prefix('fc-customer-info');


export default class Customer extends React.Component {

  static propTypes = {
    customer: PropTypes.object.isRequired
  };

  get customerName() {
    const customer = this.props.customer;

    if (customer.name) {
      return (
        <div className={prefixed('name')}>
          {customer.name}
        </div>
      );
    }
  }

  get customerRank() {
    const customer = this.props.customer;

    if (_.isNumber(customer.rank)) {
      return (
        <div className={prefixed('rank')}>
          Top {customer.rank}%
        </div>
      );
    }
  }

  render() {
    let customer = this.props.customer;
    let joinedAt = '';
    if (customer.createdAt !== undefined) {
      joinedAt = moment.utc(customer.createdAt).format('MM/DD/YYYY HH:mm:ss');
    }

    return (
      <div className="fc-content-box fc-customer-title-block">
        <div className={prefixed('header')}>
          <div className={prefixed('head')}>
            {this.customerRank}
          </div>
        </div>
        <article className={prefixed('body')}>
          <div className="fc-grid">
            <div className="fc-col-md-1-12">
              <div className={prefixed('avatar')}>
                <i className="icon-customer"></i>
              </div>
            </div>
            <div className="fc-col-md-11-12">
              <div className="fc-col-md-1-1 fc-customer-name-block">
                {this.customerName}
                <div className={prefixed('email')}>
                  {customer.email}
                </div>
              </div>
              <div className="fc-col-md-1-1 fc-customer-details-block">
                <ul className={prefixed('fields')}>
                  <li>
                    <i className="icon-customer"></i>
                    <span>{ customer.id }</span>
                  </li>
                  <li>
                    <i className="icon-phone"></i>
                    <span>{ customer.phoneNumber }</span>
                  </li>
                  <li>
                    <i className="icon-location"></i>
                    <span>{ customer.location }</span>
                  </li>
                </ul>
                <ul className={prefixed('fields')}>
                  <li>
                    <i className="icon-calendar"></i>
                    <span>{ joinedAt }</span>
                    <span className={prefixed('comment')}>&nbsp;Date joined</span>
                  </li>
                  <li>
                    <i className="icon-usd"></i>
                    <Currency value={customer.totalSales || 0} />
                    <span className={prefixed('comment')}>&nbsp;Total Sales</span>
                  </li>
                  <li className={prefixed('days')}>
                    <i>{ customer.id}</i>
                    <span>Days since last order</span>
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
