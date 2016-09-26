/* @flow */

// libs
import React, { Component, Element } from 'react';
import { connect } from 'react-redux';
import _ from 'lodash';

// components
import { PageTitle } from 'components/section-title';
import { PrimaryButton } from 'components/common/buttons';
import ContentBox from 'components/content-box/content-box';
import FormField from 'components/forms/formfield';
import FoxyForm from 'components/forms/foxy-form';
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

  get renderPageTitle(): Element {
    if (this.props.details.application) {
      const title = this.props.details.application.business_name;
      return (
        <PageTitle title={title}>
          <PrimaryButton type="button" onClick={_.noop}>
            Save
          </PrimaryButton>
        </PageTitle>
      );
    }
  }

  render(): Element {
    const { application } = this.props.details;
    const { isFetching, fetchError } = this.props;
  
    if (isFetching || (!application && !fetchError)) {
      return <WaitAnimation />;
    }


    return (
      <div>
        {this.renderPageTitle}
        <div className="fc-grid">
          <div className="fc-col-md-2-3">
            <ContentBox title="Application">
              <FoxyForm onSubmit={_.noop}>
                <ul className="fc-address-form-fields">
                  <li>
                    <FormField label="Business Name">
                      <input name="business_name" type="text" defaultValue={application.business_name} disabled={true} />
                    </FormField>
                  </li>
                  <li>
                    <FormField label="Reference Number">
                      <input name="reference_number" type="text" defaultValue={application.reference_number} disabled={true} />
                    </FormField>
                  </li>
                  <li>
                    <FormField label="Name">
                      <input name="name" type="text" defaultValue={application.name} disabled={true} />
                    </FormField>
                  </li>
                  <li>
                    <FormField label="Email Address">
                      <input name="email_address" type="text" defaultValue={application.email_address} disabled={true} />
                    </FormField>
                  </li>
                </ul>
              </FoxyForm>
            </ContentBox>
          </div>
          <div className="fc-col-md-1-3">
            <ContentBox title="State">
              Hi!
            </ContentBox>
          </div>
        </div> 
      </div>
    );
  }
}

export default connect(mapStateToProps, applicationActions)(MerchantApplicationDetails);
