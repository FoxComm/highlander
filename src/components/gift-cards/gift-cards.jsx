
// libs
import React, { PropTypes } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import _ from 'lodash';

// components

import GiftCardRow from './gift-card-row';
import { SearchableList } from '../list-page';

// redux
import { actions } from '../../modules/gift-cards/list';

const mapStateToProps = state => ({ list: state.giftCards.list });

const mapDispatchToProps = dispatch => {
  return { actions: bindActionCreators(actions, dispatch) };
};

const GiftCards = props => {
  const renderRow = (row, index, columns) => {
    const key = `gift-card-${row.code}`;
    return <GiftCardRow giftCard={row} columns={columns} key={key} />;
  };

  const tableColumns = [
    {field: 'code', text: 'Gift Card Number', type: 'link', model: 'giftcard', id: 'code'},
    {field: 'originType', text: 'Type', type: 'state', model: 'giftCard'},
    {field: 'originalBalance', text: 'Original Balance', type: 'currency'},
    {field: 'currentBalance', text: 'Current Balance', type: 'currency'},
    {field: 'availableBalance', text: 'Available Balance', type: 'currency'},
    {field: 'status', text: 'State', type: 'state', model: 'giftCard'},
    {field: 'createdAt', text: 'Date/Time Issued', type: 'date'}
  ];

  return (
    <SearchableList
      emptyResultMessage="No gift cards found."
      list={props.list}
      renderRow={renderRow}
      tableColumns={tableColumns}
      searchActions={props.actions}
      url="gift_cards_search_view/_search" />
  );

};

GiftCards.propTypes = {
  list: PropTypes.object.isRequired,
  actions: PropTypes.object.isRequired
};

export default connect(mapStateToProps, mapDispatchToProps)(GiftCards);
