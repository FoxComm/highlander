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
import { PageTitle } from 'components/section-title';
import { PrimaryButton } from 'components/common/buttons';
import ContentBox from 'components/content-box/content-box';
import SubNav from './details-sub-nav';

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
    shippingMethodId: number,
  },
  status: {
    isFetching: boolean,
    fetchError: ?Object,
  },
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

  componentDidMount() {
    const { shippingMethodId } = this.props.params;
    this.props.actions.fetchShippingMethod(shippingMethodId)
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
    return (
      <div>
        {this.renderPageTitle}
        <div className="fc-grid">
          <div className="fc-col-md-1-1">
            <ContentBox title="General">
            </ContentBox>
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
