
import React, { Component } from 'react';

// components
import { PageTitle } from '../section-title';
import { PrimaryButton, Button } from '../common/buttons';
import SubNav from './sub-nav';

class CouponPage extends Component {

  get pageTitle() {
    return 'New Coupon';
  }

  render() {
    return (
      <div>
        <PageTitle title={this.pageTitle}>
          <Button>
            Cancel
          </Button>
          <PrimaryButton>
            Save
          </PrimaryButton>
        </PageTitle>
        <SubNav params={this.props.params}/>
        <div>
          {this.props.children}
        </div>
      </div>
    );
  }
};

export default CouponPage;
