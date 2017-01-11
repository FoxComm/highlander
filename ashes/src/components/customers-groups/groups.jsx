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
import { transitionTo } from 'browserHistory';

// components
import MultiSelectTable from '../table/multi-select-table';
import GroupRow from './group-row';
import { PrimaryButton } from '../common/buttons';


const prefixed = prefix('fc-customer-groups');

const mapStateToProps = state => ({ list: state.customerGroups.list });
const mapDispatchToProps = dispatch => ({ actions: bindActionCreators(actions, dispatch) });

const tableColumns = [
  { field: 'name', text: 'Group Name' },
  { field: 'type', text: 'Type' },
  { field: 'customersCount', text: 'Customers Count' },
  { field: 'createdAt', type: 'date', text: 'Date/Time Created' },
  { field: 'modifiedAt', type: 'date', text: 'Date/Time Last Modified' },
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

  componentDidMount() {
    this.props.actions.fetch();
  }

  @autobind
  handleAddGroup() {
    transitionTo('new-dynamic-customer-group');
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
    const { list, actions: { updateStateAndFetch } } = this.props;

    return (
      <div className={classNames(prefixed())}>
        <MultiSelectTable
          columns={tableColumns}
          data={list}
          renderRow={this.renderRow}
          setState={updateStateAndFetch}
          emptyMessage="No groups found." />
      </div>
    );
  }
}
