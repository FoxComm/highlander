
import React, { Component, PropTypes } from 'react';
import { autobind } from 'core-decorators';

import styles from './page.css';

// components
import { PageTitle } from '../section-title';
import { PrimaryButton } from '../common/buttons';
import SubNav from './sub-nav';
import WaitAnimation from '../common/wait-animation';

export default class PromotionPage extends Component {

  static propTypes = {
    params: PropTypes.shape({
      promotionId: PropTypes.string.isRequired,
    }).isRequired,
  };

  get entityId() {
    return this.props.params.promotionId;
  }

  get isNew(): boolean {
    return this.entityId === 'new';
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
        {this.props.children}
      </div>
    );
  }
}
