/* @flow */

// libs
import React, { Component, Element } from 'react';
import { connect } from 'react-redux';
import _ from 'lodash';

// components
import WaitAnimation from 'components/common/wait-animation';

// redux
import * as applicationActions from 'modules/merchant-applications/details';

// types
import type { MerchantApplication } from 'paragons/merchant-application';

type Props = {
  params: {
    applicationId: number,
  },
  details: {
    application: ?MerchantApplication,
  },
  isFetching: boolean,
  fetchError: ?Object,
  fetchApplication: Function,
  updateApplication: Function,
};

const mapStateToProps = (state) => {
  return {
    details: state.applications.details,
    isFetching: _.get(state.asyncActions, 'getApplication.inProgress', null),
    fetchError: _.get(state.asyncActions, 'getApplication.err', null),
  };
};

class MerchantApplicationDetails extends Component {
  props: Props;

  componentDidMount() {
    this.props.fetchApplication(this.props.params.applicationId);
  }

  render(): Element {
    const { application } = this.props.details;
    const { isFetching, fetchError } = this.props;
  
    if (isFetching || (!application && !fetchError)) {
      return <WaitAnimation />;
    }


    return <div>Details for applicationID</div>;
  }
}

export default connect(mapStateToProps, applicationActions)(MerchantApplicationDetails);
