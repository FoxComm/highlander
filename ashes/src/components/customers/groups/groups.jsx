/* @flow */

import _ from 'lodash';
import React, { Component, Element } from 'react';
import { Link } from 'components/link';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import classNames from 'classnames';

import ContentBox from 'components/content-box/content-box';
import { EditButton, DeleteButton } from 'components/common/buttons';
import SearchGroupModal from './search-group-modal';

import { suggestGroups } from 'modules/customer-groups/suggest';
import { saveGroups } from 'modules/customers/details';

import styles from './groups.css';

type Props = {
  groups: Array<TCustomerGroupShort>,
  suggested: Array<TCustomerGroupShort>,
  suggestState: string,
  customerId: number,
  saveGroups: Function,
  suggestGroups: Function,
};

type State = {
  modalShown: boolean,
};

function mapStateToProps(state) {
  return {
    suggested: state.customerGroups.suggest.groups,
    suggestState: _.get(state.asyncActions, 'suggestGroups', {}),
  };
}

class CustomerGroupsBlock extends Component {
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
  onEditGroupsSave(groups: Array<TCustomerGroupShort>): void {
    const id = this.props.customerId;
    const payload = _.filter(this.props.groups, { groupType: 'manual' }).concat(groups);
    this.setState({ modalShown: false }, () => {
      this.props.saveGroups(id, payload);
    });
  }

  @autobind
  handleDelete(id: number) {
    const { customerId, groups } = this.props;
    const payload = _.filter(groups, g => g.id != id && g.groupType == 'manual');
    this.props.saveGroups(customerId, payload);
  }

  get groups(): Element|Array<Element> {
    const { groups } = this.props;

    if (_.isEmpty(groups)) {
      return (
        <div className="fc-content-box__empty-row">
          Customer does not have group membership.
        </div>
      );
    }

    const groupList = this.props.groups.map((group: TCustomerGroupShort) => {
      const linkClass = classNames(styles.group, {[styles.dynamic]: group.groupType != 'manual'});
      return (
        <div className={styles['group-container']} key={group.id}>
          <Link className={linkClass} to="customer-group" params={{groupId: group.id}}>
            <span className={styles.name}><i className="icon icon-customers"></i> {group.name}</span>
            <span className={styles.type}>{_.capitalize(group.groupType)}</span>
          </Link>
          {group.groupType == 'manual' && <DeleteButton onClick={() => this.handleDelete(group.id)} />}
        </div>
      );
    });

    return <div styleName="groups">{groupList}</div>;
  }

  render() {
    return (
      <ContentBox title="Groups" actionBlock={this.actionBlock}>
        {this.groups}
        <SearchGroupModal
          isVisible={this.state.modalShown}
          onCancel={this.onEditGroupsCancel}
          handleSave={this.onEditGroupsSave}
          suggestGroups={this.props.suggestGroups}
          suggested={this.props.suggested}
          suggestState={this.props.suggestState}
        />
      </ContentBox>
    );
  }
};

export default connect(
  mapStateToProps,
  { suggestGroups, saveGroups }
)(CustomerGroupsBlock);
