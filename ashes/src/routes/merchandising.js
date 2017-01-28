/* @flow */
import React from 'react';

import FoxRouter from 'lib/fox-router';
import { frn } from 'lib/frn';

import TaxonomiesListPage from 'components/taxonomies/taxonomies';
import TaxonomiesSearchableList from 'components/taxonomies/searchable-list';
import TaxonomyDetails from 'components/taxonomies/details';

import type { Claims } from 'lib/claims';

const getRoutes = (jwt: Object) => {
  const router = new FoxRouter(jwt);

  const taxonomyRoutes =
    router.read('taxonomies-base', { path: 'taxonomies', frn: frn.merch.taxonomy }, [
      router.read('taxonomies-list-pages', { component: TaxonomiesListPage }, [
        router.read('taxonomies', { component: TaxonomiesSearchableList, isIndex: true }),
      ]),
      router.read('taxonomy', { path: ':context/:taxonomyId', titleParam: ':taxonomyId' }, [
        router.read('taxonomy-details', { component: TaxonomyDetails, isIndex: true }),
      ]),
    ]);

  return (
    <div>
      {taxonomyRoutes}
    </div>
  );
};

export default getRoutes;
