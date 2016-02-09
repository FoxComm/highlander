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
    return (
      <div className="fc-customer">
        <div className="fc-grid">
          <div className="fc-col-md-1-1">
            <TitleBlock customer={this.props.details} />
          </div>
        </div>
        <LocalNav gutter={true}>
          <a href="">Insights</a>
          <IndexLink to="customer-details" params={this.props.params}>Details</IndexLink>
          <NavDropdown title="Transaction">
            <Link to="customer-transactions" params={this.props.params}>Orders</Link>
            <Link to="customer-returns" params={this.props.params}>Returns</Link>
            <Link to="customer-cart" params={this.props.params}>Cart</Link>
          </NavDropdown>
          <a href="">Items</a>
          <Link to="customer-storecredits" params={this.props.params}>Store Credit</Link>
          <a href="">Notifications</a>
          <a href="">Reviews</a>
          <Link to="customer-notes" params={this.props.params}>Notes</Link>
          <Link to="customer-activity-trail" params={this.props.params}>Activity Trail</Link>
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
