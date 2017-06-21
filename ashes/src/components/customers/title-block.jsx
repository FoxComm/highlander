//libs
import _ from 'lodash';
import React from 'react';
import PropTypes from 'prop-types';

// components
import Icon from 'components/core/icon';

//styles
import styles from './title-block.css';

//components
import Currency from '../common/currency';
import { DateTime } from 'components/common/datetime';

export default class Customer extends React.Component {
  static propTypes = {
    customer: PropTypes.object.isRequired,
  };

  get customerName() {
    const customer = this.props.customer;

    if (customer.name) {
      return (
        <div id="fct-customer-title-name" styleName="name">
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
      joinedAt = <DateTime value={customer.createdAt} />;
    }

    return (
      <div styleName="block" className="fc-content-box">
        <div styleName="header" />
        <article styleName="body">
          <div className="fc-grid">
            <div className="fc-col-md-1-12">
              <div styleName="avatar">
                <Icon name="customer" />
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
                    <Icon name="customer" />
                    <span>{customer.id}</span>
                  </li>
                  <li>
                    <Icon name="phone" />
                    <span>{customer.phoneNumber}</span>
                  </li>
                  <li>
                    <Icon name="location" />
                    <span>{customer.location}</span>
                  </li>
                </ul>
                <ul styleName="fields">
                  <li>
                    <Icon name="calendar" />
                    <span>{joinedAt}</span>
                    <span styleName="comment">&nbsp;Date joined</span>
                  </li>
                  <li>
                    <Icon name="usd" />
                    <Currency value={customer.totalSales || 0} />
                    <span styleName="comment">&nbsp;Total Sales</span>
                  </li>
                  <li styleName="days">
                    <Icon>0</Icon>
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
