import _ from 'lodash';
import React, { PropTypes } from 'react';
import SectionTitle from '../section-title/section-title';
import LocalNav from '../local-nav/local-nav';
import TableView from '../table/tableview';
import TableRow from '../table/row';
import TableCell from '../table/cell';
import Link from '../link/link';
import { Date } from '../common/datetime';
import { TabListView, TabView } from '../tabs';
import { connect } from 'react-redux';
import * as giftCardActions from '../../modules/gift-cards/cards';

@connect(state => ({giftCards: state.giftCards.cards}), giftCardActions)
export default class GiftCards extends React.Component {

  static propTypes = {
    tableColumns: PropTypes.array
  };

  static defaultProps = {
    tableColumns: [
      {field: 'code', text: 'Gift Card Number', type: 'link', model: 'giftcard', id: 'code'},
      {field: 'originType', text: 'Type'},
      {field: 'originalBalance', text: 'Original Balance', type: 'currency'},
      {field: 'currentBalance', text: 'Current Balance', type: 'currency'},
      {field: 'availableBalance', text: 'Available Balance', type: 'currency'},
      {field: 'status', text: 'Status'},
      {field: 'createdAt', text: 'Date Issued', type: 'date'}
    ]
  };

  componentDidMount() {
    this.props.fetch(this.props.giftCards);
  }

  render() {
    const renderRow = (row, index) => (
      <TableRow key={`${index}`}>
        <TableCell>
          <Link to="giftcard" params={{giftcard: row.code}}>
            {row.code}
          </Link>
        </TableCell>
        <TableCell>{row.originType}</TableCell>
        <TableCell>{row.originalBalance}</TableCell>
        <TableCell>{row.currentBalance}</TableCell>
        <TableCell>{row.availableBalance}</TableCell>
        <TableCell>{row.status}</TableCell>
        <TableCell>
          <Date value={row.createdAt}/>
        </TableCell>
      </TableRow>
    );

    return (
      <div className="fc-list-page">
        <div className="fc-list-page-header">
          <SectionTitle title="Gift Cards" subtitle={this.props.giftCards.total}>
            <Link to="gift-cards-new" className="fc-btn fc-btn-primary">
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
              data={this.props.giftCards}
              renderRow={renderRow}
              setState={this.props.setFetchParams}
              />
          </div>
        </div>
      </div>
    );
  }
}
