// @flow

// libs
import React, { Component } from 'react';
import { find } from 'lodash';

// components
import TaxonomyWidget from './taxonomy-widget';
import { withTaxonomies } from '../hoc';

type Props = {
  productId: number,
  linkedTaxonomies: Array<LinkedTaxonomy>,
  onChange: (taxons: Array<LinkedTaxonomy>) => any,
  systemTaxonomies: Array<TaxonomyResult>,
};

class TaxonomiesListWidget extends Component {
  props: Props;

  render() {
    const { systemTaxonomies, linkedTaxonomies, productId } = this.props;

    return (
      <div>
        {systemTaxonomies.map((taxonomy: TaxonomyResult) => {
          const linkedTaxonomy = find(linkedTaxonomies, linked => linked.taxonomyId === taxonomy.taxonomyId);

          return (
            <div key={taxonomy.taxonomyId}>
              <TaxonomyWidget
                linkedTaxonomy={linkedTaxonomy}
                productId={productId}
                context={taxonomy.context}
                taxonomyId={taxonomy.taxonomyId}
                title={taxonomy.name}
                onChange={this.props.onChange}
              />
            </div>
          );
        })}
      </div>
    );
  }
}

export default withTaxonomies({ taxonomiesField: 'systemTaxonomies' })(TaxonomiesListWidget);
