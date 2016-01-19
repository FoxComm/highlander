import React, { PropTypes } from 'react';
import LocalNav from '../local-nav/local-nav';
import { IndexLink, Link } from '../link';
import SectionTitle from '../section-title/section-title';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { transitionTo } from '../../route-helpers';
import { actions as customersActions } from '../../modules/customers/list';

@connect(state => ({customers: state.customers.customers}), customersActions)
export default class CustomersBase extends React.Component {

  static propTypes = {
    header: PropTypes.node,
    children: PropTypes.node.isRequired,
    customers: PropTypes.object.isRequired,
    fetch: PropTypes.func.isRequired,
  };

  static contextTypes = {
    history: PropTypes.object.isRequired
  };

  @autobind
  onAddCustomerClick() {
    transitionTo(this.context.history, 'customers-new');
  }

  get customersTotal() {
    const selectedSearch = this.props.customers.selectedSearch;
    const results = this.props.customers.savedSearches[selectedSearch].results;
    return results.total;
  }

  componentDidMount() {
    if (!this.customersTotal) {
      this.props.fetch("customers_search_view/_search");
    }
  }

  render() {
    return (
      <div className="fc-list-page">
        <div className="fc-list-page-header">
          <SectionTitle title="Customers"
                      subtitle={ this.customersTotal }
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
