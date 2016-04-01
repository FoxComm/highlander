// libs
import React, { Component, PropTypes } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import classNames from 'classnames';

// data
import * as actions from '../../modules/customer-groups/list';

// helpers
import { prefix } from '../../lib/text-utils';
import { transitionTo } from '../../route-helpers';

// components
import TableView from '../table/tableview';
import TableRow from '../table/row';
import TableCell from '../table/cell';
import Link from '../link/link';
import { DateTime } from '../common/datetime';
import { PrimaryButton } from '../common/buttons';


const prefixed = prefix('fc-customer-groups');

const mapStateToProps = state => ({list: state.customerGroups.list});
const mapDispatchToProps = dispatch => ({actions: bindActionCreators(actions, dispatch)});

const tableColumns = [
  {field: 'name', type: 'id', text: 'Group Name', model: 'group'},
  {field: 'type', text: 'Type'},
  {field: 'createdAt', type: 'date', text: 'Date/Time Created'},
  {field: 'modifiedAt', type: 'date', text: 'Date/Time Last Modified'},
];

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

  componentDidMount() {
    this.props.actions.fetch();
  }

  @autobind
  handleAddGroup() {
    transitionTo(this.context.history, 'new-dynamic-customer-group');
  }

  renderRow(row, index) {
    return (
      <TableRow key={`${index}`}>
        <TableCell>
          <Link to={'customer-group'} params={{groupId: row.id}}>
            {row.name}
          </Link>
        </TableCell>
        <TableCell>{row.type}</TableCell>
        <TableCell>
          <DateTime value={row.createdAt} />
        </TableCell>
        <TableCell>
          <DateTime value={row.modifiedAt} />
        </TableCell>
      </TableRow>
    );
  }

  render() {
    const {list, actions: {updateStateAndFetch}} = this.props;

    return (
      <div className={classNames('fc-grid', prefixed())}>
        <div className={classNames('fc-col-md-1-1', prefixed('header'))}>
          <h2 className={prefixed('header__title')}>Customers Groups</h2>
          <PrimaryButton icon="add" onClick={this.handleAddGroup} />
        </div>
        <div className={classNames('fc-col-md-1-1',prefixed('table'))}>
          <TableView
            columns={tableColumns}
            data={list}
            renderRow={this.renderRow}
            setState={updateStateAndFetch}
          />
        </div>
      </div>
    );
  }
}
