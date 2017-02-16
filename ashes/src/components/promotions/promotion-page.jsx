/* @flow weak */

// libs
import React, { Element} from 'react';

// components
import SubNav from './sub-nav';
import { connectPage, ObjectPage } from '../object-page/object-page';

// actions
import * as PromotionActions from 'modules/promotions/details';


class PromotionPage extends ObjectPage {
  subNav(): Element<*> {
    return <SubNav promotionId={this.entityId} />;
  }
}

export default connectPage('promotion', PromotionActions)(PromotionPage);
