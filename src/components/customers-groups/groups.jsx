import React, { Component, PropTypes } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';

import * as actions from '../../modules/customer-groups/list';

import { transitionTo } from '../../route-helpers';
import TableView from '../table/tableview';
import TableRow from '../table/row';
import TableCell from '../table/cell';
import Link from '../link/link';
import { DateTime } from '../common/datetime';
import { PrimaryButton } from '../common/buttons';


const mapStateToProps = state => ({list: state.customerGroups.list});
const mapDispatchToProps = dispatch => ({actions: bindActionCreators(actions, dispatch)});

@connect(mapStateToProps, mapDispatchToProps)
export default class Groups extends Component {

  static propTypes = {
    actions: PropTypes.shape({
      fetch: PropTypes.func.isRequired,
      updateStateAndFetch: PropTypes.func.isRequired,
    }),
    list: PropTypes.object.isRequired,
  };

  static contextTypes = {
    history: PropTypes.object.isRequired
  };

  static tableColumns = [
    {field: 'name', type: 'id', text: 'Group Name', model: 'group'},
    {field: 'type', text: 'Type'},
    {field: 'createdAt', type: 'date', text: 'Date/Time Created'},
    {field: 'modifiedAt', type: 'date', text: 'Date/Time Last Modified'},
  ];

  componentDidMount() {
    this.props.actions.fetch();
  }

  @autobind
  handleAddGroup() {
    transitionTo(this.context.history, 'new-dynamic-group');
  }

  renderRow(row, index) {
    return (
      <TableRow key={`${index}`}>
        <TableCell>
          <Link to={'group'} params={{groupId: row.id}}>
            {row.name}
          </Link>
        </TableCell>
        <TableCell>{row.type}</TableCell>
        <TableCell><DateTime value={row.createdAt} /></TableCell>
        <TableCell><DateTime value={row.modifiedAt} /></TableCell>
      </TableRow>
    );
  }

  render() {
    const {list, actions: {updateStateAndFetch}} = this.props;

    return (
      <div className="fc-grid fc-customer-groups-components">
        <div className="fc-col-md-1-1 _group-header _group-component">
          <h2 className="_group-title">Customers Groups</h2>
          <PrimaryButton icon="add" onClick={this.handleAddGroup} />
        </div>
        <div className="fc-col-md-1-1 _group-component">
          <TableView
            columns={Groups.tableColumns}
            data={list}
            renderRow={this.renderRow}
            setState={updateStateAndFetch}
          />
        </div>
      </div>
    );
  }
}
