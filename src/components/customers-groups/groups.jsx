import React, { PropTypes } from 'react';
import TableView from '../table/tableview';
import TableRow from '../table/row';
import TableCell from '../table/cell';
import Link from '../link/link';
import { DateTime } from '../common/datetime';
import TabListView from '../tabs/tabs';
import TabView from '../tabs/tab';
import SectionTitle from '../section-title/section-title';
import { connect } from 'react-redux';
import * as groupsActions from '../../modules/groups/list';
import LocalNav from '../local-nav/local-nav';
import Currency from '../common/currency';
import Status from '../common/status';


@connect(state => ({groups: state.groups.list}), groupsActions)
export default class Groups extends React.Component {

  static propTypes = {
    fetch: PropTypes.func.isRequired,
    setFetchParams: PropTypes.func.isRequired,
    tableColumns: PropTypes.array,
    groups: PropTypes.shape({
      rows: PropTypes.array.isRequired,
      total: PropTypes.number
    }),
  };

  static defaultProps = {
    tableColumns: [
      {field: 'name', text: 'Group Name', model: 'group'},
      {field: 'type', text: 'Type', model: 'group'},
      {field: 'customersCount', text: 'Number in Group', model: 'group'},
      {field: 'createdAt', text: 'Date/Time Created', type: 'date'},
      {field: 'modifiedAt', text: 'Date/Time Last Modified', type: 'date'},
    ]
  };

  componentDidMount() {
    this.props.fetch(this.props.groups);
  }

  handleAddOrderClick() {
    console.log('add click');
  }

  render() {
    const renderRow = (row, index) => (
      <TableRow key={`${index}`}>
        <TableCell>
          {row.name}
        </TableCell>
        <TableCell>{row.type}</TableCell>
        <TableCell>{row.customersCount}</TableCell>
        <TableCell><DateTime value={row.createdAt}/></TableCell>
        <TableCell><DateTime value={row.modifiedAt}/></TableCell>
      </TableRow>
    );

    return (
      <div className="fc-list-page">
        <div className="fc-list-page-header">
          <SectionTitle title="Groups" subtitle={this.props.groups.total}
                        onAddClick={this.handleAddOrderClick }
                        addTitle="Group"
          />
        </div>
        <div className="fc-grid fc-list-page-content">
          <TableView
            columns={this.props.tableColumns}
            data={this.props.groups}
            renderRow={renderRow}
            setState={this.props.setFetchParams}
          />
        </div>
      </div>
    );
  }
}
