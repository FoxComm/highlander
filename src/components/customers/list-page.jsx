
// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { transitionTo } from '../../route-helpers';

// components
import LocalNav from '../local-nav/local-nav';
import { IndexLink, Link } from '../link';
import SectionTitle from '../section-title/section-title';
import { ListPageContainer, selectCountFromLiveSearch } from '../list-page';

// redux
import { actions as customersActions } from '../../modules/customers/list';

@connect(selectCountFromLiveSearch(state => state.customers.list), customersActions)
export default class CustomersListPage extends React.Component {

  static propTypes = {
    children: PropTypes.node.isRequired,
    entitiesCount: PropTypes.number,
    fetch: PropTypes.func.isRequired,
  };

  static contextTypes = {
    history: PropTypes.object.isRequired
  };

  @autobind
  onAddCustomerClick() {
    transitionTo(this.context.history, 'customers-new');
  }

  componentDidMount() {
    if (this.props.entitiesCount == null) {
      this.props.fetch('customers_search_view/_search');
    }
  }

  render() {
    const navLinks = [
      { title: 'Lists', to: 'customers' },
      { title: 'Customer Groups', to: 'groups' },
      { title: 'Insights', to: '' },
      { title: 'Activity Trail', to: 'customers-activity-trail' }
    ];

    return (
      <ListPageContainer
        title="Customers"
        subtitle={this.props.entitiesCount}
        addTitle="Customer"
        handleAddAction={ this.onAddCustomerClick }
        navLinks={navLinks}
      >
        {this.props.children}
      </ListPageContainer>
    );
  }
}
