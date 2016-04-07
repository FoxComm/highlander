
import _ from 'lodash';
import React, { Component, PropTypes, Element } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';

import styles from './page.css';

// components
import { PageTitle } from '../section-title';
import { PrimaryButton } from '../common/buttons';
import SubNav from './sub-nav';
import WaitAnimation from '../common/wait-animation';

import * as PromotionActions from '../../modules/promotions/details';

export default class PromotionPage extends Component {

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
      this.setState({ promotion: nextProps.details.promotion });
    }
  }

  get pageTitle(): string {
    if (this.isNew) {
      return 'New Promotion';
    }

    return 'Old promotion';
  }

  @autobind
  handleSubmit() {

  }

  render(): Element {
    const props = this.props;
    const { promotion } = this.state;

    const children = React.cloneElement(props.children, {
      ...props.children.props,
      promotion,
      entity: { entityId: this.entityId, entityType: 'promotion' },
    });

    return (
      <div>
        <PageTitle title={this.pageTitle}>
          <PrimaryButton
            styleName="save-button"
            type="submit"
            onClick={this.handleSubmit}>
            Save
          </PrimaryButton>
        </PageTitle>
        <SubNav promotionId={this.entityId} />
        {children}
      </div>
    );
  }
}

export default connect(
  state => ({
    details: state.promotions.details,
    isFetching: _.get(state.asyncActions, 'getPromotion.inProgress', false),
  }),
  dispatch => ({ actions: bindActionCreators(PromotionActions, dispatch) })
)(PromotionPage);
