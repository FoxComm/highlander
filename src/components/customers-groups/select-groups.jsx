/* @flow weak */

import _ from 'lodash';
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';

import styles from './select-groups.css';

import RadioButton from '../forms/radio-button';
import { Table, TableRow, TableCell } from '../table';
import GroupsPopup from './groups-popup';
import type { GroupType } from './groups-popup';

import { fetchCustomerGroups } from '../../modules/customer-groups/all';

type Props = {
  onSelect: (groups: Array<number>) => any;
  groups: Array<GroupType>;
  selectedGroupIds: Array<number>;
  dispatch: (action: any) => any;
};

type State = {
  qualifyAll: boolean;
  popupOpened: boolean;
};

class SelectCustomerGroups extends Component {
  props: Props;

  state: State = {
    qualifyAll: true,
    popupOpened: false,
  };

  componentDidMount() {
    this.props.dispatch(fetchCustomerGroups());
  }

  @autobind
  handleChangeQualifier({target}: Object) {
    this.setState({
      qualifyAll: target.getAttribute('name') == 'qualifyAll',
    });
  }

  get tableColumns(): Array<Object> {
    return [
      { field: 'name', text: 'Customer Group Name', type: null },
      { field: 'type', text: 'Type', type: null },
      { type: null, control: this.togglePopupControl },
    ];
  }

  @autobind
  togglePopup() {
    this.setState({
      popupOpened: !this.state.popupOpened,
    });
  }

  @autobind
  handleSelect(groups: Array<number>) {
    this.closePopup();
    this.props.onSelect(groups);
  }

  @autobind
  closePopup() {
    this.setState({
      popupOpened: false,
    });
  }

  get togglePopupControl(): Element {
    const iconClass = this.state.popupOpened ? 'icon-close' : 'icon-add';
    return (
      <i className={iconClass} styleName="toggle-control" onClick={this.togglePopup}>
        <GroupsPopup
          visible={this.state.popupOpened}
          onSelect={this.handleSelect}
          groups={this.props.groups}
          selectedGroupIds={this.props.selectedGroupIds}
          onBlur={this.closePopup}
        />
      </i>
    );
  }

  get selectedGroups(): Array<GroupType> {
    return _.filter(this.props.groups, group => this.props.selectedGroupIds.indexOf(group.id) != -1);
  }

  get customersGroups(): ?Element {
    if (this.state.qualifyAll !== false) return null;

    return (
      <div>
        <Table
          styleName="groups-table"
          emptyMessage="No customer groups yet."
          columns={this.tableColumns}
          renderRow={this.renderCustomerGroupRow}
          data={{
            rows: this.selectedGroups,
          }}
        />
      </div>
    );
  }

  removeGroup(idForRemoval) {
    const newGroupIds = _.filter(this.props.selectedGroupIds, id => id != idForRemoval);

    this.props.onSelect(newGroupIds);
  }

  @autobind
  renderCustomerGroupRow(group) {
    return (
      <TableRow key={`row-${group.id}`}>
        <TableCell styleName="row-name">{group.name}</TableCell>
        <TableCell styleName="row-type">{group.type}</TableCell>
        <TableCell>
          <i
            className="icon-trash"
            styleName="remove-customer-group"
            onClick={() => this.removeGroup(group.id)}
          />
        </TableCell>
      </TableRow>
    );
  }

  render(): Element {
    return (
      <div>
        <RadioButton
          id="qualifyAll"
          name="qualifyAll"
          checked={this.state.qualifyAll === true}
          onChange={this.handleChangeQualifier}
        >
          <label htmlFor="qualifyAll">All customers qualify</label>
        </RadioButton>
        <RadioButton
          id="qualifyGroups"
          name="qualifyGroups"
          checked={this.state.qualifyAll === false}
          onChange={this.handleChangeQualifier}
        >
          <label htmlFor="qualifyGroups">Select Customer Groups qualify</label>
        </RadioButton>
        {this.customersGroups}
      </div>
    );
  }
}

export default connect(
  state => ({groups: state.customerGroups.all.groups}),
  dispatch => ({dispatch})
)(SelectCustomerGroups);
