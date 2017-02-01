/* @flow */

import _ from 'lodash';
import React, { Component } from 'react';
import { Link } from 'components/link';
import { autobind } from 'core-decorators';

import ContentBox from 'components/content-box/content-box';
import { EditButton } from 'components/common/buttons';
import SearchGroupModal from './search-group-modal';

import styles from './groups.css';

type Props = {
  groups: Array<TCustomerGroupShort>,
};

type State = {
  modalShown: boolean,
};

export default class CustomerGroupsBlock extends Component {
  props: Props

  state: State = {
    modalShown: false,
  };

  @autobind
  toggleModal(): void {
    this.setState({modalShown: !this.state.modalShown});
  }

  get actionBlock(): Element {
    return (
      <EditButton onClick={this.toggleModal}/>
    );
  }

  @autobind
  onEditGroupsCancel(): void {
    this.setState({ modalShown: false });
  }

  @autobind
  onEditGroupsSave(): void {
    this.setState({ modalShown: false });
  }

  get groups(): Array<Element> {
    const { groups } = this.props;

    if (_.isEmpty(groups)) {
      return (
        <div className="fc-content-box__empty-row">
          Customer does not have group membership.
        </div>
      );
    }

    return this.props.groups.map((group: TCustomerGroupShort) => (
      <Link className={styles.group} to="customer-group" params={{groupId: group.id}} key={group.id}>
        <span className={styles.name}><i className="icon icon-customers"></i> {group.name}</span>
        <span className={styles.type}>{_.capitalize(group.groupType)}</span>
      </Link>
    ));
  }

  render() {
    return (
      <ContentBox title="Groups" actionBlock={this.actionBlock}>
        {this.groups}
        <SearchGroupModal
          isVisible={this.state.modalShown}
          onCancel={this.onEditGroupsCancel}
          handleSave={this.onEditGroupsSave}
        />
      </ContentBox>
    );
  }
};
