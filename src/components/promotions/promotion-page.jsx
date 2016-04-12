
/* @flow weak */

import _ from 'lodash';
import React, { Component, PropTypes, Element } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import { pushState } from 'redux-router';

import styles from './promotion-page.css';

// components
import { PageTitle } from '../section-title';
import { PrimaryButton } from '../common/buttons';
import SubNav from './sub-nav';
import WaitAnimation from '../common/wait-animation';

import * as PromotionActions from '../../modules/promotions/details';

class PromotionPage extends Component {

  static propTypes = {
    params: PropTypes.shape({
      promotionId: PropTypes.string.isRequired,
    }).isRequired,
  };

  state = {
    promotion: this.props.details.promotion,
  };

  get entityId() {
    return this.props.params.promotionId;
  }

  get isNew(): boolean {
    return this.entityId === 'new';
  }

  componentDidMount() {
    if (this.isNew) {
      this.props.actions.promotionsNew();
    } else {
      this.props.actions.fetchPromotion(this.entityId);
    }
  }

  componentWillReceiveProps(nextProps) {
    const { isFetching } = nextProps;

    if (!isFetching) {
      const nextPromotion = nextProps.details.promotion;
      if (this.isNew && nextPromotion.form.id) {
        this.props.dispatch(pushState(null, `/promotions/${nextPromotion.form.id}`, ''));
      }
      this.setState({ promotion: nextProps.details.promotion });
    }
  }

  get pageTitle(): string {
    if (this.isNew) {
      return 'New Promotion';
    }

    const { promotion } = this.props.details;
    return _.get(promotion, 'form.attributes.name', '');
  }

  @autobind
  handleUpdatePromotion(promotion) {
    this.setState({ promotion });
  }

  @autobind
  handleSubmit() {
    if (this.state.promotion) {
      const promotion = this.state.promotion;

      if (this.isNew) {
        this.props.actions.createPromotion(promotion);
      } else {
        this.props.actions.updatePromotion(promotion);
      }
    }
  }

  render(): Element {
    const props = this.props;
    const { promotion } = this.state;

    if (!promotion || props.isFetching) {
      return <div><WaitAnimation /></div>;
    }

    const children = React.cloneElement(props.children, {
      ...props.children.props,
      promotion,
      onUpdatePromotion: this.handleUpdatePromotion,
      entity: { entityId: this.entityId, entityType: 'promotion' },
    });

    return (
      <div>
        <PageTitle title={this.pageTitle}>
          <PrimaryButton
            type="submit"
            onClick={this.handleSubmit}>
            Save
          </PrimaryButton>
        </PageTitle>
        <SubNav promotionId={this.entityId} />
        <div styleName="promotion-details">
          {children}
        </div>
      </div>
    );
  }
}

export default connect(
  state => ({
    details: state.promotions.details,
    isFetching: _.get(state.asyncActions, 'getPromotion.inProgress', false),
  }),
  dispatch => ({ actions: bindActionCreators(PromotionActions, dispatch), dispatch })
)(PromotionPage);
