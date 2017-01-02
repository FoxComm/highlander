/**
 * @flow
 */

// libs
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import _ from 'lodash';

// actions
import * as ShippingMethodActions from 'modules/shipping-methods/details';

// components
import { Dropdown } from 'components/dropdown';
import { PageTitle } from 'components/section-title';
import { PrimaryButton } from 'components/common/buttons';
import ContentBox from 'components/content-box/content-box';
<<<<<<< HEAD
=======
import CurrencyInput from 'components/forms/currency-input';
import FoxyForm from 'components/forms/foxy-form';
import FormField from 'components/forms/formfield';
>>>>>>> d4bb0ad991c01fde73050f97330aa5a6db7fc87e
import SubNav from './details-sub-nav';
import WaitAnimation from 'components/common/wait-animation';

// styles
import styles from './details.css';

// type
import type { ShippingMethod, CreatePayload, UpdatePayload } from 'paragons/shipping-method';

type Props = {
  actions: {
    fetchShippingMethod: (id: string) => Promise,
    createShippingMethod: (payload: CreatePayload) => Promise,
    updateShippingMethod: (payload: UpdatePayload) => Promise,
    archiveShippingMethod: (id: string) => Promise,
  },
  details: {
    shippingMethod: ?ShippingMethod,
  },
  params: {
    shippingMethodId: string,
  },
  status: {
    isFetching: boolean,
    fetchError: ?Object,
  },
};

type State = {
  carrierId: ?number,
  eta: ?number,
  externalFreightId: ?number,
  type: '',
};

function mapStateToProps(state) {
  return {
    details: state.shippingMethods.details,
    status: {
      isFetching: _.get(state.asyncActions, 'fetchShippingMethod.inProgress', true),
      fetchError: _.get(state.asyncActions, 'fetchShippingMethod.err', null),
      isCreating: _.get(state.asyncActions, 'createShippingMethod.inProgress', false),
      createError: _.get(state.asyncActions, 'createShippingMethod.err', null),
      isUpdating: _.get(state.asyncActions, 'updateShippingMethod.inProgress', false),
      updateError: _.get(state.asyncActions, 'updateShippingMethod.err', null),
    },
  };
}

function mapDispatchToProps(dispatch, props) {
  return {
    actions: bindActionCreators(ShippingMethodActions, dispatch),
  };
}

class ShippingMethodDetails extends Component {
  props: Props;
  state: State = { carrierId: null, eta: null, type: '' };

  componentDidMount() {
    const { shippingMethodId } = this.props.params;
    this.props.actions.fetchShippingMethod(shippingMethodId)
  }

  componentWillReceiveProps(nextProps) {
    const { shippingMethod } = nextProps.details;
    if (shippingMethod) {
      const carrierId = shippingMethod.carrier
        ? shippingMethod.carrier.id
        : null;

      this.setState({
        carrierId: carrierId,
        type: shippingMethod.type, 
      });
    }
  }

  get carriers() {
    return [
      [2, 'FedEx'],
      [3, 'UPS'],
      [1, 'USPS'],
    ]
  }

  get eta() {
    return [
      [7, '5-7 Days'],
      [3, '2-3 Days'],
      [1, 'Overnight'],
    ];
  }

  get pricingTypes() {
    return [
      ['flat', 'Flat'],
      ['variable', 'Variable'],
    ];
  }

  get freightOptions() {
    if (this.state.carrierId == 3) {
      return [
        [1, 'Standard'],
        [2, 'Ground'],
        [3, '3 Day Select'],
        [4, '2nd Day Air'],
        [5, '2nd Day Air AM'],
        [6, 'Next Day Air Saver'],
        [7, 'Next Day Air'],
        [8, 'Next Day Air A.M.'],
        [9, 'Worldwide Express'],
        [10, 'Worldwide Express Plus'],
        [11, 'Worldwide Expedited'],
        [12, 'Worldwide Saver'],
      ];
    } else if (this.state.carrierId == 1) {
      return [
        [13, 'First Class'],
        [14, 'Priority'],
        [15, 'Express'],
        [16, 'Parcel'],
      ];
    } else if (this.state.carrierId == 2) {
      return [
        [17, 'Same Day'],
        [18, 'First Overnight'],
        [19, 'Priority Overnight'],
        [20, 'Standard Overnight'],
        [21, '2 Day AM'],
        [22, '2 Day'],
        [23, 'Express Saver'],
        [24, 'Ground'],
      ];
    } else {
      return [];
    }
  }

  get renderContent() {
    const carrierDropdown = (
      <FormField label="Carrier" validator="ascii" maxLength={255} required>
        <Dropdown
          name="carrier"
          placeholder="Select carrier"
          value={this.state.carrierId}
          onChange={value => this.setState({ carrierId: Number(value) })}
          items={this.carriers} />
      </FormField>
    );

    if (this.state.type == 'flat') {
      return (
        <div>
          <FormField label="Price" validator="ascii" maxLength={255} required>
            <CurrencyInput value={this.props.details.shippingMethod.price.value} />
          </FormField>
          {carrierDropdown}
          <FormField label="Estimated Arrival" validator="ascii" maxLength={255} required>
            <Dropdown
              name="eta"
              placeholder="Select expected ETA"
              value={this.state.eta}
              onChange={value => this.setState({ eta: Number(value) })}
              items={this.eta} />
          </FormField>
        </div>
      );
    } else if (this.state.type == 'variable') {
      const freightPlaceholder = this.freightOptions.length == 0
        ? 'Please choose carrier'
        : 'Select freight option';

      return (
        <div>
          {carrierDropdown}
          <FormField label="Freight Option" validator="ascii" maxLength={255} required>
            <Dropdown
              name="externalFreightId"
              placeholder={freightPlaceholder}
              value={this.state.externalFreightId}
              onChange={value => this.setState({ externalFreightId: Number(value) })}
              items={this.freightOptions} />
          </FormField>
        </div>
      );
    }
  }

  @autobind
  renderSubNav() {
    if (this.props.details.shippingMethod) {
      const { id } = this.props.details.shippingMethod;
      if (id) {
        return <SubNav shippingMethodId={id} />;
      }
    }
  }

  get renderPageTitle(): ?Element {
    const { isFetching } = this.props.status;
    const { shippingMethod } = this.props.details;

    if (!isFetching && shippingMethod) {
      console.log('method');
      console.log(shippingMethod);
      const title = `Shipping Method ${shippingMethod.code}`;

      return (
        <PageTitle title={title}>
        </PageTitle>
      );
    }
  }

  render(): Element {
    const { shippingMethod } = this.props.details;
    const { isFetching } = this.props.status;

    if (isFetching || !shippingMethod) {
      return (
        <div styleName="waiting">
          <WaitAnimation />
        </div>
      );
    }

    return (
      <div>
        {this.renderPageTitle}
        <div className="fc-grid">
          <div className="fc-col-md-1-1">
<<<<<<< HEAD
            <ContentBox title="General">
            </ContentBox>
=======
            {this.renderSubNav()}
            <FoxyForm>
              <ContentBox title="General">
                <FormField label="Name" validator="ascii" maxLength={255} required>
                  <input type="text" defaultValue={shippingMethod.name} />
                </FormField>
                <FormField label="Code" validator="ascii" maxLength={255} required>
                  <input type="text" defaultValue={shippingMethod.code} />
                </FormField>
              </ContentBox>
              <ContentBox title="Pricing">
                <FormField label="Pricing Type" validator="ascii" maxLength={255} required>
                  <Dropdown
                    name="pricingType"
                    placeholder="Pricing Type"
                    value={this.state.type}
                    onChange={value => this.setState({ type: value })}
                    items={this.pricingTypes} />
                </FormField>
                {this.renderContent}
              </ContentBox>
            </FoxyForm>
>>>>>>> d4bb0ad991c01fde73050f97330aa5a6db7fc87e
          </div>
        </div>
      </div>
    );
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(ShippingMethodDetails);

//
// function cleanShippingMethod(entity) {
//   const attributes = _.get(entity, 'attributes', entity);
//   return _.reduce(attributes, (res, val, key) => {
//     return {
//       ...res,
//       [key]: _.get(val, 'v', val),
//     };
//   });
// }
//
// class ShippingMethodDetails extends ObjectPage {
//   props: Props;
//
//   get pageTitle(): string {
//     if (this.isNew) {
//       return 'New Shipping Method';
//     }
//
//     return _.get(this.props.details.shippingMethod, 'attributes.code', '');
//   }
//
//
//   @autobind
//   createEntity(entity) {
//     // Strip attributes out because server doesn't use them.
//     const cleaned = cleanShippingMethod(entity);
//     this.props.actions.createShippingMethod(cleaned);
//   }
//
//   @autobind
//   updateEntity(entity) {
//     // Strip attributes out because server doesn't use them.
//     const cleaned = cleanShippingMethod(entity);
//     this.props.actions.updateShippingMethod(entity.id, cleaned).then(resp => {
//       this.transitionTo(resp.payload.id);
//     });
//   }
//
//   @autobind
//   archiveEntity() {
//     this.props.actions.archiveShippingMethod(this.entityId).then(() => {
//       this.transitionToList();
//     });
//   }
//
//   @autobind
//   subNav() {
//     if (this.props.details.shippingMethod) {
//       const { id } = this.props.details.shippingMethod;
//       if (id) {
//         return <SubNav shippingMethodId={id} />;
//       }
//     }
//   }
// }
//
// export default connectPage('shippingMethod', ShippingMethodActions)(ShippingMethodDetails);
