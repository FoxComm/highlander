'use strict';

import React from 'react';
import Api from '../../lib/api';
import TableHead from '../tables/head';
import TableBody from '../tables/body';

export default class GiftCards extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      cards: []
    };
  }

  componentDidMount() {
    Api.get('/gift-cards')
       .then((cards) => { this.setState({cards: cards}); })
       .catch((err) => { console.log(err); });
  }

  render() {
    return (
      <div id="cards">
        <div className="gutter">
          <table className="inline">
            <TableHead columns={this.props.tableColumns} />
            <TableBody columns={this.props.tableColumns} rows={this.state.cards} model="gift-card" />
          </table>
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
    {field: 'cardNumber', text: 'Gift Card Number'},
    {field: 'type', text: 'Type'},
    {field: 'balance', text: 'Original Balance', type: 'currency'},
    {field: 'currentBalance', text: 'Current Balance', type: 'currency'},
    {field: 'availableBalance', text: 'Available Balance', type: 'currency'},
    {field: 'state', text: 'State'},
    {field: 'date', text: 'Date Issued', type: 'date'}
  ]
};
