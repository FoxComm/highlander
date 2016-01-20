import React, { PropTypes } from 'react';
import { actions } from '../../modules/gift-cards/cards';
import { transitionTo } from '../../route-helpers';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import _ from 'lodash';

import GiftCardRow from './gift-card-row';
import ListPage from '../list-page/list-page';

const getState = state => ({ list: state.giftCards.list });

const mapDispatchToProps = dispatch => {
  return { actions: bindActionCreators(actions, dispatch) };
};

@connect(getState, mapDispatchToProps)
export default class GiftCards extends React.Component {
  static propTypes = {
    list: PropTypes.object.isRequired,
    actions: PropTypes.object.isRequired
  };

  static contextTypes = {
    history: PropTypes.object.isRequired
  };

  get navLinks() {
    return [
      { title: 'Lists', to: 'gift-cards' },
      { title: 'Insights', to: '' },
      { title: 'Activity Trail', to: '' }
    ];
  }

  get newGiftCard() {
    return () => transitionTo(this.context.history, 'gift-cards-new');
  }

  get renderRow() {
    return (row, index, columns) => {
      const key = `gift-card-${row.code}`;
      return <GiftCardRow giftCard={row} columns={columns} key={key} />;
    };
  }

  get tableColumns() {
    return [
      {field: 'code', text: 'Gift Card Number', type: 'link', model: 'giftcard', id: 'code'},
      {field: 'originType', text: 'Type', type: 'status', model: 'giftCard'},
      {field: 'originalBalance', text: 'Original Balance', type: 'currency'},
      {field: 'currentBalance', text: 'Current Balance', type: 'currency'},
      {field: 'availableBalance', text: 'Available Balance', type: 'currency'},
      {field: 'status', text: 'State', type: 'status', model: 'giftCard'},
      {field: 'createdAt', text: 'Date/Time Issued', type: 'date'}
    ];
  }

  render() {
    return (
      <ListPage
        addTitle="Gift Card"
        emptyResultMessage="No gift cards found."
        handleAddAction={this.newGiftCard}
        list={this.props.list}
        navLinks={this.navLinks}
        renderRow={this.renderRow}
        tableColumns={this.tableColumns}
        searchActions={this.props.actions}
        title="Gift Cards"
        url="gift_cards/_search" />
    );
  }
}
