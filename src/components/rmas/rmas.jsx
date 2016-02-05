
// libs
import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';

// components
import { Link } from '../link';
import { PageTitle } from '../section-title';
import LocalNav from '../local-nav/local-nav';
import { TabListView, TabView } from '../tabs';
import TableView from '../table/tableview';
import { renderRow } from './helpers';

// redux
import * as rmaActions from '../../modules/rmas/list';

const mapStateToProps = state => {
  return {
    rmas: {
      total: 0,
      ...state.rmas.list,
    },
  };
};

@connect(mapStateToProps, rmaActions)
export default class Rmas extends React.Component {
  static propTypes = {
    tableColumns: PropTypes.array,
    fetchRmas: PropTypes.func.isRequired,
    rmas: PropTypes.shape({
      total: PropTypes.number
    })
  };

  static defaultProps = {
    tableColumns: [
      {field: 'referenceNumber', text: 'Return', type: 'id'},
      {field: 'createdAt', text: 'Date', type: 'date'},
      {field: 'orderRefNum', text: 'Order', model: 'order', type: 'id'},
      {field: 'email', text: 'Email'},
      {field: 'state', text: 'Return State', type: 'rmaStatus'},
      {field: 'returnTotal', text: 'Total', type: 'currency'}
    ]
  };

  componentDidMount() {
    this.props.fetchRmas();
  }

  render() {
    return (
      <div className="fc-list-page">
        <div className="fc-list-page-header">
          <PageTitle title="Returns" subtitle={this.props.rmas.total} />
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
                data={this.props.rmas}
                columns={this.props.tableColumns}
                setState={this.props.fetchRmas}
                renderRow={renderRow}
            />
          </div>
        </div>
      </div>
    );
  }
}
