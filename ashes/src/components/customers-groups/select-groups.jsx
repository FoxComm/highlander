/* @flow weak */

import _ from 'lodash';
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { trackEvent } from 'lib/analytics';

import styles from './select-groups.css';

import RadioButton from '../forms/radio-button';
import { Table, TableRow, TableCell } from '../table';
import { SelectableList, SelectableItem } from '../selectable-list';

import { fetchCustomerGroups } from '../../modules/customer-groups/all';

type GroupType = {
  name: string;
  type: string;
  id: number;
}

type Props = {
  onSelect: (groups: Array<number>) => any;
  groups: Array<GroupType>;
  selectedGroupIds: Array<number>;
  qualifyAll: boolean,
  dispatch: (action: any) => any;
  parent: string;
};

type State = {
  popupOpened: boolean;
};

class SelectCustomerGroups extends Component {
  props: Props;

  static defaultProps = {
    parent: '',
  };

  state: State = {
    popupOpened: false,
  };

  componentDidMount() {
    this.props.dispatch(fetchCustomerGroups());
  }

  @autobind
  handleChangeQualifier({target}: Object) {
    let isAllQualify = target.getAttribute('name') == 'qualifyAll'
    this.props.qualifyAllChange(isAllQualify);
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
    const eventName = this.state.popupOpened ? 'click_popup_close' : 'click_popup_open';
    trackEvent(`Customer groups(${this.props.parent})`, eventName);
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

  @autobind
  renderGroup(group: GroupType) {
    return (
      <SelectableItem id={group.id}>
        <strong>{group.name}</strong>
        <span styleName="group-description">• {group.type}</span>
      </SelectableItem>
    );
  }

  get togglePopupControl() {
    const iconClass = this.state.popupOpened ? 'icon-close' : 'icon-add';

    return (
      <i className={iconClass} styleName="toggle-control" onClick={this.togglePopup}>
        <SelectableList
          visible={this.state.popupOpened}
          onSelect={this.handleSelect}
          items={this.props.groups}
          selectedItemIds={this.props.selectedGroupIds}
          onBlur={this.closePopup}
          emptyMessage="There are no customers groups."
          renderItem={this.renderGroup}
        />
      </i>
    );
  }

  get selectedGroups(): Array<GroupType> {
    return _.filter(this.props.groups, group => this.props.selectedGroupIds.indexOf(group.id) != -1);
  }

  get customersGroups(): ?Element<*> {
    if (this.props.qualifyAll !== false) return null;

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

  render() {
    return (
      <div>
        <RadioButton
          id="qualifyAll"
          name="qualifyAll"
          checked={this.props.qualifyAll === true}
          onChange={this.handleChangeQualifier}
        >
          <label htmlFor="qualifyAll">All customers qualify</label>
        </RadioButton>
        <RadioButton
          id="qualifyGroups"
          name="qualifyGroups"
          checked={this.props.qualifyAll === false}
          onChange={this.handleChangeQualifier}
        >
          <label htmlFor="qualifyGroups">Select customer groups qualify</label>
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
