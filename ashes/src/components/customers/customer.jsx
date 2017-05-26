import React, { Component } from 'react';
import PropTypes from 'prop-types';

import { Link, IndexLink } from 'components/link';
import TitleBlock from './title-block';
import { connect } from 'react-redux';
import * as CustomersActions from '../../modules/customers/details';
import PageNav from 'components/core/page-nav';
import WaitAnimation from '../common/wait-animation';

@connect((state, props) => ({
  ...state.customers.details[props.params.customerId]
}), CustomersActions)
export default class Customer extends Component {

  static propTypes = {
    params: PropTypes.shape({
      customerId: PropTypes.string.isRequired
    }).isRequired,
    details: PropTypes.object,
    fetchCustomer: PropTypes.func,
    children: PropTypes.node,
    failed: PropTypes.bool,
  };

  componentDidMount() {
    const { customerId } = this.props.params;

    this.props.fetchCustomer(customerId);
  }

  renderChildren() {
    return React.Children.map(this.props.children, child => {
      return React.cloneElement(child, {
        entity: this.props.details
      });
    });
  }

  render() {
    let content;

    if (this.props.failed) {
      content = this.errorMessage;
    } else if (this.props.isFetching || !this.props.details) {
      content = this.waitAnimation;
    } else {
      content = this.content;
    }

    return (
      <div className="fc-customer">
        {content}
      </div>
    );
  }

  get waitAnimation() {
    return <WaitAnimation />;
  }

  get errorMessage() {
    return <div className="fc-customer__empty-messages">An error occurred. Try again later.</div>;
  }

  get content() {
    const { details, params } = this.props;

    return (
      <div>
        <div className="fc-grid">
          <div className="fc-col-md-1-1">
            <TitleBlock customer={details} />
          </div>
        </div>
        <PageNav>
          <IndexLink to="customer-details" params={params}>Details</IndexLink>
          <Link to="customer-cart" params={params}>Cart</Link>
          <Link to="customer-transactions" params={params}>Orders</Link>
          <Link to="customer-items" params={params}>Items</Link>
          <Link to="customer-storecredits" params={params}>Store Credit</Link>
          <Link to="customer-notes" params={params}>Notes</Link>
          <Link to="customer-activity-trail" params={params}>Activity Trail</Link>
        </PageNav>
        <div className="fc-grid">
          <div className="fc-col-md-1-1 fc-col-no-overflow">
            { this.renderChildren() }
          </div>
        </div>
      </div>
    );
  }
}
