'use strict';

import React from 'react';
import TableView from '../tables/tableview';
import GiftCardStore from '../../stores/gift-cards';
import GiftCardActions from '../../actions/gift-cards';
import SectionTitle from '../section-title/section-title';
import LocalNav from '../local-nav/local-nav';
import { TabListView, TabView } from '../tabs';
import { Link } from '../link';
import _ from 'lodash';

export default class GiftCards extends React.Component {
  constructor(props, context) {
    super(props, context);
    this.state = {
      data: GiftCardStore.getState()
    };
    this.onChange = this.onChange.bind(this);
  }

  componentDidMount() {
    GiftCardStore.listen(this.onChange);

    GiftCardActions.fetchGiftCards();
  }

  componentWillUnmount() {
    GiftCardStore.unlisten(this.onChange);
  }

  onChange() {
    let state = GiftCardStore.getState();
    this.setState({
      data: state
    });
  };

  onCardsAdded(cards) {
    let cardList = this.state.cards.slice(0, this.state.cards.length);

    this.toggleNew();

    for (let card of cards) {
      card.isNew = true;
    }

    Array.prototype.unshift.apply(cardList, cards);
    this.setState({
      cards: cardList
    });

    this.removeNew();
  }

  removeNew() {
    setTimeout(() => {
      let rows = [].slice.call(document.querySelectorAll('tr.new'));
      for (let row of rows) {
        row.classList.remove('new');
      }
    }, 5000);
  }

  render() {
    return (
      <div id="cards" className="fc-list-page">
        <div className="fc-list-page-header">
          <SectionTitle title="Gift Cards" count={this.state.data.size}>
            <Link to='gift-cards-new' className="fc-btn fc-btn-primary"><i className="icon-add"></i> New Gift Card</Link>
          </SectionTitle>
          <LocalNav>
            <a href="">Lists</a>
            <a href="">Returns</a>
          </LocalNav>
          <TabListView>
            <TabView selector="#all">All</TabView>
            <TabView selector="#active">Active</TabView>
          </TabListView>
        </div>
        <div className="fc-grid fc-list-page-content">
          <div id="all" className="fc-col-md-1-1">
            <TableView
                columns={this.props.tableColumns}
                rows={this.state.data.toArray()}
                model='giftcard'
            />
          </div>
        </div>
      </div>
    );
  }
}

GiftCards.propTypes = {
  tableColumns: React.PropTypes.array
};

GiftCards.defaultProps = {
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
