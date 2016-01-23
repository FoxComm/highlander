// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';

// components
import { ListPageContainer, selectCountFromLiveSearch } from '../list-page';

// redux
import { actions } from '../../modules/orders/list';

@connect(selectCountFromLiveSearch(state => state.orders.list), actions)
export default class OrdersListPage extends React.Component {

  static propTypes = {
    children: PropTypes.node.isRequired,
    entitiesCount: PropTypes.number,
    fetch: PropTypes.func.isRequired,
  };

  componentDidMount() {
    if (this.props.entitiesCount == null) {
      this.props.fetch('orders_search_view/_search');
    }
  }

  render() {
    const props = this.props;

    const navLinks = [
      { title: 'Lists', to: 'orders' },
      { title: 'Insights', to: '' },
      { title: 'Activity Trail', to: 'orders-activity-trail' }
    ];

    return (
      <ListPageContainer
        title="Orders"
        subtitle={props.entitiesCount}
        addTitle="Order"
        navLinks={navLinks}>
        {props.children}
      </ListPageContainer>
    );
  }
};
