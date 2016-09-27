/* @flow */

// libs
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import _ from 'lodash';

// components
import { Dropdown } from 'components/dropdown';
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

const SELECT_STATE = [
  ['new', 'New', true],
  ['approved', 'Approved'],
  ['rejected', 'Rejected'],
  ['abandonded', 'Abandoned'],
];

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

type State = {
  newState: string,
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
  state: State;

  constructor(props: Props, ...args: Object) {
    super(props, ...args);

    const { application } = props.details;
    if (application) {
      this.state = { newState: application.state };
    }
  }

  componentDidMount() {
    this.props.fetchApplication(this.props.params.applicationId);
  }

  componentWillReceiveProps(nextProps: Props) {
    const { application } = nextProps.details;
    if (application) {
      this.setState({ newState: application.state });
    }
  }

  get isDirty(): Element {
    const { application } = this.props.details;
    if (!application) {
      return false;
    }

    return this.state.newState != application.state;
  }


  get isStateEditable(): Element {
    const state = _.get(this.props, 'details.application.state', '');
    return state == 'new';
  }

  get renderPageTitle(): Element {
    if (this.props.details.application) {
      const title = this.props.details.application.business_name;
      return (
        <PageTitle title={title}>
          <PrimaryButton
            type="button"
            disabled={!this.isDirty}
            onClick={this.handleSubmit}>
            Save
          </PrimaryButton>
        </PageTitle>
      );
    }
  }

  get renderState(): Element {
    return (
      <div className="fc-col-md-1-3">
        <ContentBox title="State">
          <Dropdown
            value={this.state.newState}
            onChange={this.handleStateChange}
            disabled={!this.isStateEditable}
            items={SELECT_STATE}
            changeable={false} />
        </ContentBox>
      </div>
    );
  }

  @autobind
  handleStateChange(newState) {
    this.setState({ newState });
  }

  @autobind
  handleSubmit() {
    const { application } = this.props.details;
    if (application) {
      // :( The Elixir app has camel cased JSON.
      const merchant_application = {
        ...application,
        state: this.state.newState,
      };

      this.props.updateApplication(application.id, { merchant_application });
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
                  <li>
                    <FormField label="Description">
                      <input name="descriptionn" type="text" defaultValue={application.description} disabled={true} />
                    </FormField>
                  </li>
                </ul>
              </FoxyForm>
            </ContentBox>
          </div>
          {this.renderState}
        </div>
      </div>
    );
  }
}

export default connect(mapStateToProps, applicationActions)(MerchantApplicationDetails);
