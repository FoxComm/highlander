import React, { PropTypes } from 'react';
import { Link, IndexLink } from '../link';
import TitleBlock from './title-block';
import { connect } from 'react-redux';
import * as CustomersActions from '../../modules/customers/details';
import LocalNav, { NavDropdown } from '../local-nav/local-nav';

@connect((state, props) => ({
  ...state.customers.details[props.params.customerId]
}), CustomersActions)
export default class Customer extends React.Component {

  static propTypes = {
    params: PropTypes.shape({
      customerId: PropTypes.string.isRequired
    }).isRequired,
    details: PropTypes.object,
    fetchCustomer: PropTypes.func,
    children: PropTypes.node
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

  get page() {
    const {details, params} = this.props;

    return (
      <div className="fc-customer">
        <div className="fc-grid">
          <div className="fc-col-md-1-1">
            <TitleBlock customer={details} />
          </div>
        </div>
        <LocalNav gutter={true}>
          <IndexLink to="customer-details" params={params}>Details</IndexLink>
          <Link to="customer-cart" params={params}>Cart</Link>
          <Link to="customer-transactions" params={params}>Orders</Link>
          <a href="">Items</a>
          <Link to="customer-storecredits" params={params}>Store Credit</Link>
          <a href="">Notifications</a>
          <Link to="customer-notes" params={params}>Notes</Link>
          <Link to="customer-activity-trail" params={params}>Activity Trail</Link>
        </LocalNav>
        <div className="fc-grid">
          <div className="fc-col-md-1-1">
            { this.renderChildren() }
          </div>
        </div>
      </div>
    );
  }

  render() {
    return (this.props.details ? this.page : null);
  }
}
