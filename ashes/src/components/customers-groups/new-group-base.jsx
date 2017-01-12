/* @flow */

import { autobind } from 'core-decorators';
import React, { Component, Element } from 'react';
import { connect } from 'react-redux';
import { push } from 'react-router-redux';

import { reset, fetchGroup, saveGroup } from 'modules/customer-groups/group';
import { fetchRegions } from 'modules/regions';

import { Link } from 'components/link';

type Props = {
  group: TCustomerGroup;
  reset: () => void;
  fetchGroup: (id: number) => Promise;
  saveGroup: () => Promise;
  fetchRegions: () => Promise;
  push: (location: Object) => void;
  children: Element;
};

class NewGroupBase extends Component {
  props: Props;

  componentDidMount() {
    this.props.fetchRegions();
  }

  @autobind
  handleSave() {
    const { group, saveGroup, push } = this.props;

    saveGroup().then(res => {
      push({ name: 'customer-group', params: { groupId: res.id } });
    });
  }

  render() {
    const { children, ...rest } = this.props;

    return (
      <div className="fc-customer-group-edit">
        {React.cloneElement(children, { ...rest, onSave: this.handleSave })}
      </div>
    );
  }
}

const mapStateToProps = state => ({
  group: state.customerGroups.details.group
});

export default connect(mapStateToProps, { reset, fetchGroup, saveGroup, fetchRegions, push })(NewGroupBase);
