// @flow

// lib
import React, { Element } from 'react';
import { autobind } from 'core-decorators';

// components
import ObjectDetailsDeux from 'components/object-page/object-details-deux';
import TaxonListWidget from './taxon-list-widget';

import type { Renderers } from 'components/object-page/object-details-deux';

export default class TaxonDetails extends React.Component {

  props: ObjectPageChildProps<Taxon>;

  @autobind
  renderTaxonListWidget() {
    const { taxonomyId, context, taxonId } = this.props.params;
    return(
      <TaxonListWidget
        id={taxonomyId}
        context={context}
        currentTaxon={taxonId}
      />
    );
  };

  get renderers(): Renderers {
    return {
      taxonList: this.renderTaxonListWidget
    };
  }

  render() {
    return (
      <ObjectDetailsDeux
        {...this.props}
        renderers={this.renderers}
      />
    );
  }

}
