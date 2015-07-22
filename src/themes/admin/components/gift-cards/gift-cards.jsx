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
    Api.get(``)
       .then((cards) => { this.setState({cards: cards}); })
       .catch((err) => { console.log(err); });
  }

  render() {
    return (
      <table className="inline">
        <TableHead columns={this.props.tableColumns} />
        <TableBody columns={this.props.tableColumns} rows={this.state.cards} model="gift-card" />
      </table>
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
    {field: 'balance', text: 'Original Balance'},
    {field: 'state', text: 'State'},
    {field: 'date', text: 'Date Issued', type: 'date'}
  ]
};
