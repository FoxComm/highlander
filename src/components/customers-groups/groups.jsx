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
import MultiSelectTable from '../table/multi-select-table';
import GroupRow from './group-row';
import { PrimaryButton } from '../common/buttons';


const prefixed = prefix('fc-customer-groups');

const mapStateToProps = state => ({list: state.customerGroups.list});
const mapDispatchToProps = dispatch => ({actions: bindActionCreators(actions, dispatch)});

const tableColumns = [
  {field: 'name', text: 'Group Name'},
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

  get renderRow() {
    return (row, index, columns, params) => {
      const key = `group-${row.id}`;

      return (
        <GroupRow
          key={key}
          group={row}
          columns={columns}
          params={params} />
      );
    };
  }

  render() {
    const {list, actions: {updateStateAndFetch}} = this.props;

    return (
      <div className={classNames('fc-grid', prefixed())}>
        <div className={classNames('fc-col-md-1-1', prefixed('header'))}>
          <h2 className={prefixed('header__title')}>Customers Groups</h2>
          <PrimaryButton icon="add" onClick={this.handleAddGroup} />
        </div>
        <div className={classNames('fc-col-md-1-1', prefixed('table'))}>
          <MultiSelectTable
            columns={tableColumns}
            data={list}
            renderRow={this.renderRow}
            setState={updateStateAndFetch}
            emptyMessage="No groups found." />
        </div>
      </div>
    );
  }
}
