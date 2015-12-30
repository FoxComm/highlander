import React, { PropTypes } from 'react';
import LocalNav from '../local-nav/local-nav';
import { IndexLink, Link } from '../link';
import SectionTitle from '../section-title/section-title';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { transitionTo } from '../../route-helpers';
import * as customersActions from '../../modules/customers/list';

@connect(state => ({customers: state.customers.customers}), customersActions)
export default class CustomersBase extends React.Component {

  static propTypes = {
    header: PropTypes.node,
    children: PropTypes.node.isRequired,
    customers: PropTypes.object.isRequired,
  };

  static contextTypes = {
    history: PropTypes.object.isRequired
  };

  @autobind
  onAddCustomerClick() {
    transitionTo(this.context.history, 'customers-new');
  }

  componentDidMount() {
    if (!this.props.customers.total) {
      this.props.fetch(this.props.customers);
    }
  }

  render() {
    return (
      <div className="fc-list-page">
        <div className="fc-list-page-header">
          <SectionTitle title="Customers"
                      subtitle={ this.props.customers.total }
                      onAddClick={ this.onAddCustomerClick }
                      addTitle="Customer" />
          <LocalNav>
            <IndexLink to="customers">Lists</IndexLink>
            <IndexLink to="groups">Customer Groups</IndexLink>
            <a href="">Insights</a>
            <a href="">Activity Trial</a>
          </LocalNav>
          {this.props.header}
        </div>
        {this.props.children}
      </div>
    );
  }
}
