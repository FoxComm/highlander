'use strict';

import _ from 'lodash';
import React, { PropTypes } from 'react';
import TableView from '../tables/tableview';
import GiftCardStore from '../../stores/gift-cards';
import GiftCardActions from '../../actions/gift-cards';
import SectionTitle from '../section-title/section-title';
import LocalNav from '../local-nav/local-nav';
import { TabListView, TabView } from '../tabs';
import { Link } from '../link';
import { connect } from 'react-redux';
import * as giftCardActions from '../../modules/gift-cards/cards';

@connect(({giftCards}) => ({items: giftCards.cards.items}), giftCardActions)
export default class GiftCards extends React.Component {

  static propTypes = {
    tableColumns: PropTypes.array,
    items: PropTypes.array,
    fetchGiftCardsIfNeeded: PropTypes.func
  };

  static defaultProps = {
    tableColumns: [
      {field: 'code', text: 'Gift Card Number', type: 'link', model: 'giftcard', id: 'code'},
      {field: 'originType', text: 'Type'},
      {field: 'originalBalance', text: 'Original Balance', type: 'currency'},
      {field: 'currentBalance', text: 'Current Balance', type: 'currency'},
      {field: 'availableBalance', text: 'Available Balance', type: 'currency'},
      {field: 'status', text: 'Status'},
      {field: 'date', text: 'Date Issued', type: 'date'}
    ]
  };

  componentDidMount() {
    this.props.fetchGiftCardsIfNeeded();
  }

  render() {
    return (
      <div className="fc-list-page">
        <div className="fc-list-page-header">
          <SectionTitle title="Gift Cards" subtitle={this.props.items.size}>
            <Link to='gift-cards-new' className="fc-btn fc-btn-primary">
              <i className="icon-add"></i> New Gift Card
            </Link>
          </SectionTitle>
          <LocalNav>
            <a href="">Lists</a>
            <a href="">Returns</a>
          </LocalNav>
          <TabListView>
            <TabView>All</TabView>
            <TabView>Active</TabView>
          </TabListView>
        </div>
        <div className="fc-grid fc-list-page-content">
          <div className="fc-col-md-1-1">
            <TableView
                columns={this.props.tableColumns}
                rows={this.props.items}
                model='giftcard'
            />
          </div>
        </div>
      </div>
    );
  }
}
