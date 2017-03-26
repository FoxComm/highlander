// @flow

// lib
import get from 'lodash/get';
import React, { Element, Component } from 'react';
import { autobind } from 'core-decorators';

// components
import ObjectDetailsDeux from 'components/object-page/object-details-deux';
import TaxonListWidget from './taxon-list-widget';

import type { Renderers } from 'components/object-page/object-details-deux';

export default class TaxonDetails extends Component {
  /*
   * should be
   * props: ObjectPageChildProps<Taxon> & {
   *  taxonomy: Taxonomy,
   * }
   * but flow does not understand that props are of type ObjectPageChildProps<Taxon> and throw an error in
   * <ObjectDetailsDeux
   *   {...this.props}
   *   renderers={this.renderers}
   * />
   */
  props: ObjectPageChildProps<Taxon>;

  @autobind
  renderTaxonListWidget() {
    const params = get(this.props, 'params', {});
    return (
      <TaxonListWidget
        context={params.context}
        taxonomyId={params.taxonomyId}
        activeTaxonId={params.taxonId}
      />
    );
  };

  get renderers(): Renderers {
    return {
      taxonList: this.renderTaxonListWidget,
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
