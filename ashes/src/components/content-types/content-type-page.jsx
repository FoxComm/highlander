/* @flow weak */

// libs
import _ from 'lodash';
import React, { Element } from 'react';

// components
import SubNav from './sub-nav';
import { connectPage, ObjectPage } from '../object-page/object-page';
import { transitionTo } from 'browserHistory';

// actions
import * as ContentTypeActions from 'modules/content-types/details';

class ContentTypePage extends ObjectPage {
  save(): ?Promise<*> {
    let isNew = this.isNew;
    let willBePromo = super.save();

    if (willBePromo && isNew) {
      willBePromo.then((data) => {
        if (data.applyType === 'coupon') {
          transitionTo('content-type-coupon-new',{promotionId: data.id});
        }
      });
    }

    return willBePromo;
  }

  subNav(): Element<*> {
    return <SubNav applyType={_.get(this.props, 'details.contentType.applyType')} contentTypeId={this.entityId} />;
  }
}

export default connectPage('contentType', ContentTypeActions)(ContentTypePage);
