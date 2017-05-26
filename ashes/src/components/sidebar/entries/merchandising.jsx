/* @flow */

import React, { Component } from 'react';
import { includes } from 'lodash';

import { anyPermitted } from 'lib/claims';
import { frn, readAction } from 'lib/frn';

// components
import NavigationItem from '../navigation-item';
import Spinner from 'components/core/spinner';
import { withTaxonomies } from 'components/taxonomies/hoc';

// styles
import styles from './entries.css';

const taxonomyClaims = readAction(frn.merch.taxonomy);

type Props = TMenuEntry & {
  taxonomies: Array<TaxonomyResult>,
  fetchState: AsyncState,
  currentParams: {
    taxonomyId: string,
  }
};

class MerchandisingEntry extends Component {
  props: Props;

  get taxonomiesList() {
    const { claims, routes, taxonomies, currentParams, fetchState } = this.props;

    if (!taxonomies || fetchState.inProgress) {
      return <Spinner />;
    }

    return taxonomies.map((taxonomy: TaxonomyResult) => {
      const linkParams = {
        context: taxonomy.context,
        taxonomyId: taxonomy.taxonomyId
      };

      return (
        <li key={`${taxonomy.taxonomyId}`}>
          <NavigationItem
            to="taxonomy"
            linkParams={linkParams}
            icon="taxonomies"
            title={`${taxonomy.name}`}
            routes={routes}
            actualClaims={claims}
            expectedClaims={taxonomyClaims}
            forceActive={taxonomy.taxonomyId.toString() === currentParams.taxonomyId}
          />
        </li>
      );
    });
  }

  render() {
    const { claims, routes, currentParams } = this.props;
    const allClaims = taxonomyClaims;

    if (!anyPermitted(allClaims, claims)) {
      return null;
    }

    const routeNames = routes.map(route => route.name);
    const manageRoute = includes(routeNames, 'taxonomies') && !currentParams.taxonomyId ||
      includes(routeNames, 'taxonomy') && currentParams.taxonomyId === 'new';

    return (
      <div styleName="fc-entries-wrapper">
        <h3>MERCHANDISING</h3>
        {this.taxonomiesList}
        <li>
          <NavigationItem
            to="taxonomies"
            icon="taxonomies"
            title="Manage Taxonomies"
            routes={routes}
            actualClaims={claims}
            expectedClaims={taxonomyClaims}
            currentParams={currentParams}
            forceActive={manageRoute}
          />
        </li>
      </div>
    );
  }
}

export default withTaxonomies({ showLoader: false })(MerchandisingEntry);
