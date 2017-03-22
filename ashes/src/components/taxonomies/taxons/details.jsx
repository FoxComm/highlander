// @flow

// lib
import React, { Element } from 'react';
import { autobind } from 'core-decorators';
import _ from 'lodash';

// components
import ObjectDetailsDeux from 'components/object-page/object-details-deux';
import TaxonListWidget from './taxon-list-widget';

import type { Renderers } from 'components/object-page/object-details-deux';

export default class TaxonDetails extends React.Component {

  props: ObjectPageChildProps<Taxon>

  render() {
    return (
      <ObjectDetailsDeux
        {...this.props}
      />
    );
  }

}
