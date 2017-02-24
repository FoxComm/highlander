/* @flow */
import React from 'react';

import FoxRouter from 'lib/fox-router';
import { frn } from 'lib/frn';

import TaxonomiesListPage from 'components/taxonomies/taxonomies';
import TaxonomiesSearchableList from 'components/taxonomies/searchable-list';
import TaxonomyPage from 'components/taxonomies/taxonomy';
import TaxonomyDetails from 'components/taxonomies/details';

import TaxonsListPage from 'components/taxonomies/taxons/taxons';

import type { Claims } from 'lib/claims';

const getRoutes = (jwt: Object) => {
  const router = new FoxRouter(jwt);

  const taxonomyRoutes =
    router.read('taxonomies-base', { path: 'taxonomies', frn: frn.merch.taxonomy }, [
      router.read('taxonomies-list-pages', { component: TaxonomiesListPage }, [
        router.read('taxonomies', { component: TaxonomiesSearchableList, isIndex: true }),
      ]),
      router.read('taxonomy', {
        path: ':context/:taxonomyId',
        titleParam: ':taxonomyId',
        component: TaxonomyPage
      }, [
          router.read('taxonomy-details', { component: TaxonomyDetails, isIndex: true }),
          router.read('taxons', { path: 'taxons', component: TaxonsListPage, frn: frn.merch.taxon }),
        ]),

    ]);

  return (
    <div>
      {taxonomyRoutes}
    </div>
  );
};

export default getRoutes;
