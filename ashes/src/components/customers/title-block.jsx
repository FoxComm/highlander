//libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import moment from 'moment';

//styles
import styles from './title-block.css';

//components
import Currency from '../common/currency';


export default class Customer extends React.Component {

  static propTypes = {
    customer: PropTypes.object.isRequired
  };

  get customerName() {
    const customer = this.props.customer;

    if (customer.name) {
      return (
        <div id="customer-title-name" styleName="name">
          {customer.name}
        </div>
      );
    }
  }

  get customerRank() {
    const customer = this.props.customer;

    if (_.isNumber(customer.rank)) {
      return (
        <div styleName="rank">
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
      <div styleName="block" className="fc-content-box">
        <div styleName="header">
        </div>
        <article styleName="body">
          <div className="fc-grid">
            <div className="fc-col-md-1-12">
              <div styleName="avatar">
                <i className="icon-customer"></i>
              </div>
            </div>
            <div className="fc-col-md-11-12">
              <div className="fc-col-md-1-1 fc-customer-name-block">
                {this.customerName}
                <div id="customer-title-email" styleName="email">
                  {customer.email}
                </div>
              </div>
              <div className="fc-col-md-1-1 fc-customer-details-block">
                <ul styleName="fields">
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
                <ul styleName="fields">
                  <li>
                    <i className="icon-calendar"></i>
                    <span>{ joinedAt }</span>
                    <span styleName="comment">&nbsp;Date joined</span>
                  </li>
                  <li>
                    <i className="icon-usd"></i>
                    <Currency value={customer.totalSales || 0} />
                    <span styleName="comment">&nbsp;Total Sales</span>
                  </li>
                  <li styleName="days">
                    <i>0</i>
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
