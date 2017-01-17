/* @flow */

import get from 'lodash/get';
import { autobind } from 'core-decorators';
import React, { Component, Element } from 'react';
import { connect } from 'react-redux';
import { push } from 'react-router-redux';

import { reset, fetchGroup, saveGroup, clearFetchErrors, clearSaveErrors } from 'modules/customer-groups/details/group';
import { fetchRegions } from 'modules/regions';

import { Link } from 'components/link';
import WaitAnimation from 'components/common/wait-animation';
import Error from 'components/errors/error';

type Props = {
  group: TCustomerGroup;
  fetchInProgress: boolean;
  saveInProgress: boolean;
  fetchError: Object;
  saveError: Object;
  reset: () => void;
  fetchGroup: (id: number) => Promise;
  saveGroup: () => Promise;
  clearFetchErrors: () => void;
  clearSaveErrors: () => void;
  fetchRegions: () => Promise;
  push: (location: Object) => void;
  params: {
    groupId: number;
  };
  children: Element;
};

class NewGroupBase extends Component {
  props: Props;

  componentWillMount() {
    this.props.clearFetchErrors();
    this.props.clearSaveErrors();

    // reset group data if we have no :groupId param in url
    if (!this.props.params.groupId) {
      this.props.reset();
    }
  }

  componentDidMount() {
    const { group, fetchGroup, fetchRegions, params: { groupId } } = this.props;

    fetchRegions();

    // fetch group if we have :groupId url param but have no group fetched
    if (groupId && !group.id) {
      fetchGroup(groupId);
    }
  }

  @autobind
  handleSave() {
    const { group, saveGroup, push } = this.props;

    saveGroup().then(res => {
      push({ name: 'customer-group', params: { groupId: res.id } });
    });
  }

  render() {
    const { group, fetchInProgress, fetchError, params, children, ...rest } = this.props;

    if (params.groupId && fetchError) {
      return <Error err={fetchError} />;
    }

    // show loader if loading in progress or we have :groupId url param but have no group fetched
    if (fetchInProgress || params.groupId && !group.id) {
      return <div><WaitAnimation /></div>;
    }

    return (
      <div className="fc-customer-group-edit">
        {React.cloneElement(children, { group, onSave: this.handleSave, ...rest })}
      </div>
    );
  }
}

const mapStateToProps = state => ({
  fetchInProgress: get(state, 'asyncActions.fetchCustomerGroup.inProgress', false),
  saveInProgress: get(state, 'asyncActions.saveCustomerGroup.inProgress', false),
  fetchError: get(state, 'asyncActions.fetchCustomerGroup.err', false),
  saveError: get(state, 'asyncActions.saveCustomerGroup.err', false),
  group: state.customerGroups.details.group
});

const mapActions = { reset, fetchGroup, saveGroup, clearFetchErrors, clearSaveErrors, fetchRegions, push };

export default connect(mapStateToProps, mapActions)(NewGroupBase);
